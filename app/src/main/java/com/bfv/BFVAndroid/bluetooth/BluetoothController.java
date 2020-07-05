package com.bfv.BFVAndroid.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothController {
    void connectBtDevice(BluetoothDevice bd);
    void disconnectBtDevice();
    boolean writeToBT(String data);
}
