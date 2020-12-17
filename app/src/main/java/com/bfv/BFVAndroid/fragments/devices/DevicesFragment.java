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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;
import com.bfv.BFVAndroid.bluetooth.BluetoothController;
import com.bfv.BFVAndroid.bluetooth.BluetoothProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;


public class DevicesFragment extends Fragment implements DevicesRecyclerAdapter.ItemClickListener  {
    private SharedDataViewModel sharedData;

    private TextView stateTextView;
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
        stateTextView = rootView.findViewById(R.id.stateTextView);
        devicesRecyclerView = rootView.findViewById(R.id.devicesRecyclerView);
        FloatingActionButton openBluetoothSettings = rootView.findViewById(R.id.searchForDevices);

        // Set up the RecyclerView layout manager
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If we don't have bluetooth
        if (mBluetoothAdapter == null) {
            sharedData.setConnectionState(BluetoothProvider.STATE_NO_BLUETOOTH_ADAPTER);
        }

        // We have bluetooth
        else {
            // Enable bluetooth if it wasn't already on
            if ( ! mBluetoothAdapter.isEnabled()) {
                sharedData.setConnectionState(BluetoothProvider.STATE_BLUETOOTH_DISABLED);
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

        DividerItemDecoration itemDecorator = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider));

        devicesRecyclerView.addItemDecoration(itemDecorator);
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
            sharedData.setConnectionState(BluetoothProvider.STATE_NO_PAIRED_DEVICES);
            Toast.makeText(getContext(), "Please pair the device via your phone's Settings!", Toast.LENGTH_SHORT).show();
        } else {
            // Set adapter
            devicesRecyclerAdapter = new DevicesRecyclerAdapter(getContext(), pairedDevices);
            devicesRecyclerAdapter.setClickListener(this);
            devicesRecyclerView.setAdapter(devicesRecyclerAdapter);

            // Set background color if connected
            if (bluetoothController.getState() == BluetoothProvider.STATE_CONNECTED) {
                BluetoothDevice bluetoothDevice = bluetoothController.getConnectedDevice();
                if (pairedDevices.contains(bluetoothDevice)) {
                    devicesRecyclerView.post(changeDeviceItemBackgroundColor(bluetoothDevice, true));
                    stateTextView.setText(getString(R.string.connected_to, bluetoothDevice.getName()));
                }
            }
            else {
                stateTextView.setText(getString(R.string.disconnected));
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
    public void onItemClick(int position) {
        BluetoothDevice connectedDevice = bluetoothController.getConnectedDevice();
        BluetoothDevice deviceToConnectTo = devicesRecyclerAdapter.getItem(position);

        // If not connected user probably wants to connect
        if (bluetoothController.getState() != BluetoothProvider.STATE_CONNECTED) {
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
                case BluetoothProvider.STATE_BLUETOOTH_DISABLED:
                    stateTextView.setText(R.string.bluetooth_off);
                    break;
                case BluetoothProvider.STATE_NO_BLUETOOTH_ADAPTER:
                    stateTextView.setText(R.string.no_bluetooth);
                    break;
                case BluetoothProvider.STATE_DISCONNECTED:
                    stateTextView.setText(R.string.disconnected);
                    if(bluetoothController.getPreviousConnectedDevice() != null) {
                        devicesRecyclerView.post(changeDeviceItemBackgroundColor(bluetoothController.getPreviousConnectedDevice(), false));
                    }
                    break;
                case BluetoothProvider.STATE_CONNECTING:
                    stateTextView.setText(R.string.connecting);
                    break;
                case BluetoothProvider.STATE_CONNECTED:
                    stateTextView.setText(getString(R.string.connected_to, bluetoothController.getConnectedDevice().getName()));
                    devicesRecyclerView.post(changeDeviceItemBackgroundColor(bluetoothController.getConnectedDevice(), true));
                    break;
                case BluetoothProvider.STATE_NO_PAIRED_DEVICES:
                    stateTextView.setText(R.string.no_paired_devices);
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
        // We use bluetoothController to command BluetoothProvider via MainActivity that implements
        // BluetoothController interface
        bluetoothController = (BluetoothController) context;
        super.onAttach(context);
    }
}