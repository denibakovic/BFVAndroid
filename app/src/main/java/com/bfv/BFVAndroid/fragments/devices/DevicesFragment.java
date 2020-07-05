package com.bfv.BFVAndroid.fragments.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bfv.BFVAndroid.bluetooth.BluetoothController;
import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;
import com.bfv.BFVAndroid.bluetooth.BluetoothService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;


public class DevicesFragment extends Fragment implements DevicesRecyclerAdapter.ItemClickListener  {
    private SharedDataViewModel sharedData;

    private TextView statusTextView;
    private RecyclerView devicesRecyclerView;
    private DevicesRecyclerAdapter devicesRecyclerAdapter;

    private BluetoothController bluetoothController;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> pairedDevices;
    private boolean askedForBluetooth;

    private View rootView;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Get sharedData ViewModel and set observers
        sharedData = new ViewModelProvider(getActivity()).get(SharedDataViewModel.class);
        sharedData.getConnectionState().observe(getViewLifecycleOwner(), connectionStateObserver);

        // Inflate View
        rootView = inflater.inflate(R.layout.fragment_devices, container, false);

        return rootView;
    }


    @Override
    public void onStart() {
        super.onStart();
        // Connect Views to rootView
        statusTextView = rootView.findViewById(R.id.statusTextView);
        devicesRecyclerView = rootView.findViewById(R.id.devicesRecyclerView);
        FloatingActionButton openBluetoothSettings = rootView.findViewById(R.id.searchForDevices);

        // Set up the RecyclerView layout manager
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If we don't have bluetooth
        if (mBluetoothAdapter == null) {
            sharedData.setConnectionState(BluetoothService.STATE_NO_BLUETOOTH_ADAPTER);
        }

        // We have bluetooth device
        else {
            // Enable bluetooth if it wasn't already on
            if ( ! mBluetoothAdapter.isEnabled()) {
                sharedData.setConnectionState(BluetoothService.STATE_BLUETOOTH_DISABLED);
                if ( ! askedForBluetooth) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 0);
                    askedForBluetooth = true;
                }
                else {
                    Toast.makeText(getContext(), "Please Enable Bluetooth!", Toast.LENGTH_LONG).show();
                }
            }
            // Bluetooth is Enabled
            else {
                // List already paired devices
                listPairedDevices();
            }
        }

        openBluetoothSettings.setOnClickListener(v -> {
            Intent intentOpenBluetoothSettings = new Intent();
            intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            try {
                startActivity(intentOpenBluetoothSettings);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Look for all paired Bluetooth Devices and shows them on the device list
     * Checks if we have bluetooth and if its enabled should be made before calling this method
     */
    private void listPairedDevices() {
        // Set paired devices
        pairedDevices = new ArrayList<>(mBluetoothAdapter.getBondedDevices());

        // Check if we have any devices
        if (pairedDevices.isEmpty()) {
            sharedData.setConnectionState(BluetoothService.STATE_NO_PAIRED_DEVICES);
            Toast.makeText(getContext(), "Please pair the device via your phone's Settings!", Toast.LENGTH_SHORT).show();
        } else {
            // Set adapter
            devicesRecyclerAdapter = new DevicesRecyclerAdapter(getContext(), pairedDevices);
            devicesRecyclerAdapter.setClickListener(this);
            devicesRecyclerView.setAdapter(devicesRecyclerAdapter);

            sharedData.setConnectionState(BluetoothService.STATE_DISCONNECTED);

            // Set background color if connected
            if (sharedData.getIsConnected().getValue()) {
                BluetoothDevice bluetoothDevice = sharedData.getConnectedDevice().getValue();
                if (pairedDevices.contains(bluetoothDevice)) {
                    devicesRecyclerView.post(changeDeviceItemBackgroundColor(bluetoothDevice, true));
                    sharedData.setConnectionState(BluetoothService.STATE_CONNECTED);
                }
            }
        }
    }


    /**
     * Look for all paired Bluetooth Devices and shows them on the device list
     * Checks if we have bluetooth and if its enabled should be made before calling this method
     */
    private void clearPairedDevices() {
        // Set paired devices
        pairedDevices = new ArrayList<>();

        devicesRecyclerAdapter = new DevicesRecyclerAdapter(getContext(), pairedDevices);
        devicesRecyclerAdapter.setClickListener(this);
        devicesRecyclerView.setAdapter(devicesRecyclerAdapter);
    }


    /**
     *
     * @param bluetoothDevice BluetoothDevice to change color on
     * @param state state to change to
     * @return runnable color changer
     */
    private Runnable changeDeviceItemBackgroundColor(BluetoothDevice bluetoothDevice, boolean state) {
        return () -> devicesRecyclerView.getLayoutManager().findViewByPosition(pairedDevices.indexOf(bluetoothDevice)).setSelected(state);
    }


    /**
     * Click listener for item(device) in paired bluetooth devices list
     */
    @Override
    public void onItemClick(View view, int position) {
        BluetoothDevice connectedDevice = sharedData.getConnectedDevice().getValue();
        BluetoothDevice deviceToConnectTo = devicesRecyclerAdapter.getItem(position);

        // If not connected user probably wants to connect
        if (! sharedData.getIsConnected().getValue()) {
            bluetoothController.connectBtDevice(deviceToConnectTo);
        }
        // If user clicks on already connected device -> disconnect
        else {
            if (deviceToConnectTo.equals(connectedDevice)) {
                bluetoothController.disconnectBtDevice();
            }
            // If user clicks unconnected device but we are already connected to some other device
            else {
                Toast.makeText(getContext(), "Please Disconnect from "
                                + connectedDevice.getName() + " before Connecting gain!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * Observer for sharedData.connectionState
     */
    private final Observer<Integer> connectionStateObserver = new Observer<Integer>() {
        @Override
        public void onChanged(@Nullable Integer i) {
            switch (i) {
                case BluetoothService.STATE_BLUETOOTH_DISABLED:
                    statusTextView.setText("Bluetooth disabled!");
                    break;
                case BluetoothService.STATE_NO_BLUETOOTH_ADAPTER:
                    statusTextView.setText("Phone doesn't have bluetooth!");
                    break;
                case BluetoothService.STATE_DISCONNECTED:
                    statusTextView.setText("Disconnected!");
                    if(sharedData.getLastConnectedDevice().getValue() != null) {
                        devicesRecyclerView.post(changeDeviceItemBackgroundColor(sharedData.getLastConnectedDevice().getValue(), false));
                    }
                    break;
                case BluetoothService.STATE_CONNECTING:
                    statusTextView.setText("Connecting ..");
                    break;
                case BluetoothService.STATE_CONNECTED:
                    statusTextView.setText("Connected to " + sharedData.getConnectedDevice().getValue().getName());
                    devicesRecyclerView.post(changeDeviceItemBackgroundColor(sharedData.getConnectedDevice().getValue(), true));
                    break;
                case BluetoothService.STATE_NO_PAIRED_DEVICES:
                    statusTextView.setText("No paired devices!");
                    break;
            }
        }
    };


    /**
     * Covers the case when we return from Enabling Bluetooth
     */
    @Override
    public void onResume() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            listPairedDevices();
        }
        else {
            clearPairedDevices();
        }

        super.onResume();
    }


    @Override
    public void onAttach(@NonNull Context context) {
        // We use bluetoothController to command BluetoothService via MainActivity that implements
        // BluetoothController interface
        bluetoothController = (BluetoothController) context;
        super.onAttach(context);
    }
}