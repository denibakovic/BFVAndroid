package com.bfv.BFVAndroid.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.bfv.BFVAndroid.SharedDataViewModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;


/**
 * Class to handle BT connection
 */
public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";

    private final IBinder binder;

    // "random" unique identifier
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    // Member fields
    private final BluetoothAdapter mBluetoothAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;

    private SharedDataViewModel sharedData;

    // Constants that indicate the current connection state
    public static final int STATE_DISCONNECTED = 0;       //disconnected
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device
    public static final int STATE_NO_PAIRED_DEVICES = 33;  // user hasnt paired any devices
    public static final int STATE_BLUETOOTH_DISABLED = 66;  // phone doesn't have bluetooth
    public static final int STATE_NO_BLUETOOTH_ADAPTER = 99;  // bluetooth is disabled by user


    /**
     * This class does all the work for setting up and managing Bluetooth
     * connections with other devices. It has a thread for connecting with a device
     * and a thread for performing data transmissions when connected.
     */
    public BluetoothService() {
        binder = new LocalBinder();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_DISCONNECTED;
        mNewState = mState;
    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        // Update ConnectionStatus
        updateConnectionStatusInfo();
    }


    /**
     * Disconnect from device
     */
    public synchronized void disconnect() {
        Log.d(TAG, "disconnect");

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        noConnection();
    }


    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @return true if written, false otherwise
     * @see ConnectedThread#write(String)
     */
    public boolean write(String out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return false;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        return r.write(out);
    }


    public void setSharedData(SharedDataViewModel sharedDataViewModel) {
        sharedData = sharedDataViewModel;
    }


    /**
     * Update ConnectionStatus according to the current state of the chat connection
     */
    private synchronized void updateConnectionStatusInfo() {
        Log.d(TAG, "updateConnectionStatusInfo() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        sharedData.setConnectionState(mNewState);
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Set sharedData connection info
        sharedData.setIsConnected(true);
        sharedData.setConnectedDevice(device);

        // Update ConnectionStatus
        updateConnectionStatusInfo();
    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void noConnection() {
        mState = STATE_DISCONNECTED;
        sharedData.setIsConnected(false);
        sharedData.setConnectedDevice(null);

        // NOTE: uncomment to hide after disconnect
        //sharedData.bfv.resetAllValues();
        //sharedData.resetDeviceData();

        Log.i(TAG, "disconnected");

        // Update ConnectionStatus
        updateConnectionStatusInfo();
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                        BT_MODULE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                noConnection();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BufferedReader mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = new BufferedReader(new InputStreamReader(tmpIn));
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            // Keep reading from InputStream while connected
            boolean sendGetSettings = true;
            String line;
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    line = mmInStream.readLine();

                    // Parse data
                    sharedData.bfv.parseRawData(line);

                    // Update sharedData.rawData
                    sharedData.setRawData("In: " + line);

                    // Calling here because device doesn't send settings automatically when
                    // connected over bluetooth
                    boolean hw = sharedData.bfv.isUpdatedHardwareVersion();
                    boolean uv = sharedData.bfv.checkUpdatedValues();
                    if(hw && !(uv) && sendGetSettings) {
                        sharedData.setDeviceHwVersion(sharedData.bfv.getHwVersion());
                        write(sharedData.bfv.getAllCommands().get("getSettings").serializeCommand());
                        sendGetSettings = false;
                    }

                    // Update device HW version
                    if(sharedData.bfv.isUpdatedHardwareVersion()) {
                        sharedData.setDeviceHwVersion(sharedData.bfv.getHwVersion());
                    }

                    // Update device battery level
                    if(sharedData.bfv.isUpdatedBattery()) {
                        sharedData.setDeviceBattery(sharedData.bfv.getBattery());
                    }

                    // Update device temperature
                    if(sharedData.bfv.isUpdatedTemperature()) {
                        sharedData.setDeviceTemp(sharedData.bfv.getTemperature());
                    }

                    // Update device altitude
                    if(sharedData.bfv.isUpdatedAltitude()) {
                        sharedData.setDeviceAltitude(sharedData.bfv.getAltitude());
                    }
                } catch (IOException e) {
                    Log.i(TAG, "IOException in mConnectedThread");
                    noConnection();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param string The string to write
         * @return true if written, false otherwise
         */
        public boolean write(String string) {
            try {
                byte[] bytes = string.getBytes();
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                return false;
            }
            sharedData.setRawData("Out: " + string);
            return true;
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            // Return this instance of BluetoothService so clients can call public methods
            return BluetoothService.this;
        }
    }


    /**
     * Provide binder to MainActivity
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
