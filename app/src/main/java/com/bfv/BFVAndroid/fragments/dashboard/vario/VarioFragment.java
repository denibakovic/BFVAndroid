package com.bfv.BFVAndroid.fragments.dashboard.vario;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;
import com.bfv.BFVAndroid.bluetooth.BluetoothProvider;

public class VarioFragment extends Fragment {

    private TextView varioText;
    private long lastUpdateTime;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get sharedData ViewModel
        SharedDataViewModel sharedData = new ViewModelProvider(getActivity()).get(SharedDataViewModel.class);

        // Inflate the View
        View rootView = inflater.inflate(R.layout.fragment_vario, container, false);

        varioText = rootView.findViewById(R.id.varioText);
        varioText.setText(getString(R.string.vario, String.format("%.2f", sharedData.getVario().getValue())));
        lastUpdateTime = System.currentTimeMillis();

        // Shared data observers
        sharedData.getVario().observe(getViewLifecycleOwner(), varioObserver);
        sharedData.getConnectionState().observe(getViewLifecycleOwner(), connectionStateObserver);

        // Inflate the layout for this fragment
        return rootView;
    }


    /**
     * Observer for sharedData.deviceAltitude
     */
    private final Observer<Double> varioObserver = new Observer<Double>() {
        @Override
        public void onChanged(@Nullable Double value) {
            long currentTime = System.currentTimeMillis();
            if(currentTime - lastUpdateTime > 400) {
                varioText.setText(getString(R.string.vario, String.format("%.2f", value)));
                lastUpdateTime= currentTime;
            }
        }
    };


    /**
     * Observer for sharedData.connectionState
     */
    private final Observer<Integer> connectionStateObserver = new Observer<Integer>() {
        @Override
        public void onChanged(@Nullable Integer i) {
            if (i == BluetoothProvider.STATE_CONNECTED) {
                varioText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            } else {
                varioText.setTextColor(ContextCompat.getColor(getContext(), R.color.colorGrey));
            }
        }
    };
}
