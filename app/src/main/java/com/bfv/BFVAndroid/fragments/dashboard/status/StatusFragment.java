package com.bfv.BFVAndroid.fragments.dashboard.status;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;
import com.bfv.BFVAndroid.bluetooth.BluetoothController;
import com.bfv.BFVAndroid.bluetooth.BluetoothProvider;


public class StatusFragment extends Fragment {

    private View rootView;
    private TextView textViewBattery;
    private TextView textViewAltitude;
    private TextView textViewHwversion;
    private TextView textViewTemperature;
    private BluetoothController bluetoothController;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get sharedData ViewModel
        SharedDataViewModel sharedData = new ViewModelProvider(getActivity()).get(SharedDataViewModel.class);
        sharedData.getConnectionState().observe(getViewLifecycleOwner(), connectionStateObserver);

        // Inflate the View
        rootView = inflater.inflate(R.layout.fragment_status, container, false);

        // Shared data observers
        sharedData.getDeviceAltitude().observe(getViewLifecycleOwner(), deviceAltitudeObserver);
        sharedData.getDeviceBattery().observe(getViewLifecycleOwner(), deviceBatteryObserver);
        sharedData.getDeviceTemp().observe(getViewLifecycleOwner(), deviceTempObserver);
        sharedData.getDeviceHwVersion().observe(getViewLifecycleOwner(), deviceHwVersionObserver);

        // Connect Views to rootView
        textViewBattery = rootView.findViewById(R.id.batteryText);
        textViewAltitude = rootView.findViewById(R.id.altitudeText);
        textViewHwversion = rootView.findViewById(R.id.hwVersionText);
        textViewTemperature = rootView.findViewById(R.id.temperatureText);

        if(bluetoothController.getState() == BluetoothProvider.STATE_CONNECTED) {
            setTextColor(R.color.colorPrimary);
        }
        else {
            setTextColor(R.color.colorGrey);
        }

        // Inflate the layout for this fragment
        return rootView;
    }


    private void setTextColor(int p) {
        textViewBattery.setTextColor(ContextCompat.getColor(getContext(), p));
        textViewAltitude.setTextColor(ContextCompat.getColor(getContext(), p));
        textViewHwversion.setTextColor(ContextCompat.getColor(getContext(), p));
        textViewTemperature.setTextColor(ContextCompat.getColor(getContext(), p));
    }


    private String toBatteryPercent(Double battery) {
        double high = 4.2;
        double low = 3.5;
        int percent = (int) (((battery - low) / (high - low)) * 100);
        if (percent >= 100) percent = 100;
        if (percent <= 0) percent = 0;
        return String.valueOf(percent);
    }

    /**
     * Observer for sharedData.connectionState
     */
    private final Observer<Integer> connectionStateObserver = new Observer<Integer>() {
        @Override
        public void onChanged(@Nullable Integer i) {
            if (i == BluetoothProvider.STATE_CONNECTED) {
                rootView.post(() -> setTextColor(R.color.colorPrimary));
            } else {
                rootView.post(() -> setTextColor(R.color.colorGrey));
            }
        }
    };


    /**
     * Observer for sharedData.deviceHwVersion
     */
    private final Observer<String> deviceHwVersionObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable String hwVersion) {
            textViewHwversion.setText(hwVersion);
        }
    };


    /**
     * Observer for sharedData.deviceBattery
     */
    private final Observer<Double> deviceBatteryObserver = new Observer<Double>() {
        @Override
        public void onChanged(@Nullable Double battery) {
            if(! battery.isNaN()) {
                textViewBattery.setText(getString(R.string.battery_value, toBatteryPercent(battery), battery.toString()));
            }
        }
    };


    /**
     * Observer for sharedData.deviceTemp
     */
    private final Observer<Double> deviceTempObserver = new Observer<Double>() {
        @Override
        public void onChanged(@Nullable Double temperature) {
            if(! temperature.isNaN()) {
                textViewTemperature.setText(getString(R.string.temperature_value, temperature.toString()));
            }
        }
    };


    /**
     * Observer for sharedData.deviceAltitude
     */
    private final Observer<Double> deviceAltitudeObserver = new Observer<Double>() {
        @Override
        public void onChanged(@Nullable Double altitude) {
            if(! altitude.isNaN()) {
                textViewAltitude.setText(getString(R.string.altitude_value, String.format("%.2f", altitude)));
            }
        }
    };


    @Override
    public void onAttach(@NonNull Context context) {
        // We use bluetoothController to command BluetoothProvider via MainActivity that implements
        // BluetoothController interface
        bluetoothController = (BluetoothController) context;
        super.onAttach(context);
    }
}