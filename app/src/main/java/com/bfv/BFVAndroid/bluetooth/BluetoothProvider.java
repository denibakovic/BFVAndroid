package com.bfv.BFVAndroid.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.bfv.BFVAndroid.SharedDataViewModel;
import com.bfv.BFVAndroid.kalmanFilteredVario.KalmanFilteredVario;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;


/**
 * Class to handle BT connection
 * FIXME: if user clicks disconnect/connect before the device closes socket on its side we get new
 *  socket from the device but no output on that socket
 *  WORKAROUNDS:
 *      1. Reuse sockets?
 *      2. Wait for xy seconds before actually trying to connect to the same device
 */
public class BluetoothProvider {
    private static final String TAG = "BluetoothProvider";

    // "random" unique identifier
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final KalmanFilteredVario kalmanFilteredVario;
    private long lastAltitudeTime;
    private boolean firstAltitude = true;

    // Member fields
    private final BluetoothAdapter mBluetoothAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice connectedDevice;
    private BluetoothDevice previousConnectedDevice;
    private volatile int mState;
    private volatile int mNewState;

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
    public BluetoothProvider() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_DISCONNECTED;
        mNewState = mState;
        connectedDevice = null;
        previousConnectedDevice = null;
        kalmanFilteredVario = new KalmanFilteredVario(0.2, 0.5);
    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect() to: " + device);

        if (mState == STATE_CONNECTING) {
            // If we are getting request to connect to the same device that we are
            //  already connecting to - do nothing
            if(mConnectThread.getDevice().getAddress().equals(device.getAddress())) {
                Log.d(TAG, "connect() already connecting to: " + device);
                return;
            }

            // Cancel any thread attempting to make a connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            // If we are getting request to connect to the same device that we are
            //  already connected to - do nothing
            if(getConnectedDevice().getAddress().equals(device.getAddress())) {
                Log.d(TAG, "connect() already connected to: " + device);
                return;
            }

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
        Log.d(TAG, "disconnect()");

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread.interrupt();
                mConnectThread = null;

            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread.interrupt();
            mConnectedThread = null;
        }

        mState = STATE_DISCONNECTED;

        // NOTE: uncomment to hide after disconnect
        //sharedData.getBfv().resetAllValues();
        //sharedData.resetDeviceData();

        if(connectedDevice != null) {
            previousConnectedDevice = connectedDevice;
        }
        connectedDevice = null;

        kalmanFilteredVario.reset();
        firstAltitude = true;
        sharedData.setVario(0.0);

        // Update ConnectionStatus
        updateConnectionStatusInfo();
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


    public int getState() {
        return mState;
    }


    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }


    public BluetoothDevice getPreviousConnectedDevice() {
        return previousConnectedDevice;
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
        Log.d(TAG, "connected() to socket: " + socket + " device: " + device);

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

        connectedDevice = device;

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
                Log.e(TAG, "createInsecureRfcommSocketToServiceRecord() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "run() ConnectThread");
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
                disconnect();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothProvider.this) {
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

        public BluetoothDevice getDevice() {
            return mmDevice;
        }
    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private BufferedReader mmInStream;
        private OutputStream mmOutStream;

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
                Log.e(TAG, "tmpIn/tmpOut sockets not created", e);
                disconnect();
            }

            mmInStream = new BufferedReader(new InputStreamReader(tmpIn));
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "run() ConnectedThread");
            setName("ConnectedThread");

            // Keep reading from InputStream while connected
            boolean sendGetSettings = true;
            String line;
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    line = mmInStream.readLine();

                    // Parse data
                    sharedData.getBfv().parseLine(line);

                    // Update sharedData.rawData
                    sharedData.setRawData("In: " + line);

                    // Calling here because device doesn't send settings automatically when
                    // connected over bluetooth
                    boolean hw = sharedData.getBfv().isUpdatedHardwareVersion();
                    boolean uv = sharedData.getBfv().checkUpdatedValues();
                    if(hw && !(uv) && sendGetSettings) {
                        sharedData.setDeviceHwVersion(sharedData.getBfv().getHwVersion());
                        write(sharedData.getBfv().getAllCommands().get("getSettings").serializeCommand());
                        sendGetSettings = false;
                    }

                    // Update device HW version
                    if(sharedData.getBfv().isUpdatedHardwareVersion()) {
                        sharedData.setDeviceHwVersion(sharedData.getBfv().getHwVersion());
                    }

                    // Update device battery level
                    if(sharedData.getBfv().isUpdatedBattery()) {
                        sharedData.setDeviceBattery(sharedData.getBfv().getBattery());
                    }

                    // Update device temperature
                    if(sharedData.getBfv().isUpdatedTemperature()) {
                        sharedData.setDeviceTemp(sharedData.getBfv().getTemperature());
                    }

                    // Update device altitude
                    if(sharedData.getBfv().isUpdatedAltitude()) {
                        double altitude = sharedData.getBfv().getAltitude();
                        sharedData.setDeviceAltitude(altitude);

                        double timeDelta;
                        long currentTime = System.currentTimeMillis();

                        if(firstAltitude) {
                            timeDelta = 1.0;
                            firstAltitude = false;
                        }
                        else {
                            timeDelta = (currentTime - lastAltitudeTime) / 1000.0;  // convert to seconds
                        }

                        kalmanFilteredVario.addData(timeDelta, altitude);
                        sharedData.setVario(kalmanFilteredVario.getVar());

                        lastAltitudeTime = currentTime;
                    }
                } catch (IOException e) {
                    Log.i(TAG, "IOException in mConnectedThread.run()");
                    disconnect();
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
            mmSocket = null;

            if (mmInStream != null) {
                try {
                    mmInStream.close();}
                catch (Exception e) {
                    Log.e(TAG, "close() of input stream failed", e);
                }
                mmInStream = null;
            }

            if (mmOutStream != null) {
                try {
                    mmOutStream.close();}
                catch (Exception e) {
                    Log.e(TAG, "close() of output stream failed", e);
                }
                mmOutStream = null;
            }
        }
    }
}
