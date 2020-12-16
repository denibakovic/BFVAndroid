package com.bfv.BFVAndroid.bluetooth;

import android.app.Application;


public class BluetoothApplication extends Application {

    private BluetoothProvider bluetoothProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothProvider = new BluetoothProvider();
    }


    public BluetoothProvider getBluetoothProvider(){
        return bluetoothProvider;
    }
}
