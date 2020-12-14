package com.bfv.BFVAndroid.bluetooth;

import android.app.Application;


public class BluetoothAplication extends Application {
    private BluetoothProvider bluetoothProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothProvider = new BluetoothProvider();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public BluetoothProvider getBluetoothProvider(){
        return bluetoothProvider;
    }
}
