package com.bfv.BFVAndroid;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bfv.BFVAndroid.bluetooth.BluetoothProvider;

import java.util.ArrayList;

import BFVlib.BFV;

public class SharedDataViewModel extends ViewModel{
    private final MutableLiveData<Integer> connectionState;

    private final MutableLiveData<Double> vario;

    private final MutableLiveData<Double> deviceBattery;
    private final MutableLiveData<String> deviceHwVersion;
    private final MutableLiveData<Double> deviceTemp;
    private final MutableLiveData<Double> deviceAltitude;

    private final MutableLiveData<String> rawDataObservable;
    private final ArrayList<String> rawData;

    private final MutableLiveData<Boolean> dryRun;

    public BFV bfv;


    // Cannot invoke setValue on a background thread, use postValue
    public SharedDataViewModel() {
        rawDataObservable = new MutableLiveData<>();
        rawData = new ArrayList<>();

        vario = new MutableLiveData<>(0.0);

        deviceAltitude = new MutableLiveData<>(Double.NaN);
        deviceBattery = new MutableLiveData<>(Double.NaN);
        deviceHwVersion = new MutableLiveData<>("");
        deviceTemp = new MutableLiveData<>(Double.NaN);

        connectionState = new MutableLiveData<>(BluetoothProvider.STATE_DISCONNECTED);
        dryRun = new MutableLiveData<>(Boolean.TRUE);

        bfv = new BFV();
    }


    /**
     * Vario
     */
    public LiveData<Double> getVario() {return this.vario;}

    public void setVario(Double vario) {this.vario.postValue(vario);}


    /**
     * RawData
     */
    public void setRawData(String rawData) {
        this.rawData.add(rawData);
        this.rawDataObservable.postValue(rawData);
    }

    public ArrayList<String> getRawDataArray() {
        return this.rawData;
    }

    public MutableLiveData<String> getLastRawData() {
        return this.rawDataObservable;
    }


    /**
     * Connection State
     */
    public LiveData<Integer> getConnectionState() {return this.connectionState;}

    public void setConnectionState(int connectionState) {
        this.connectionState.postValue(connectionState);
    }


    /**
     * Device
     */
    public MutableLiveData<Double> getDeviceBattery() {
        return this.deviceBattery;
    }

    public void setDeviceBattery(Double deviceBattery) {
        this.deviceBattery.postValue(deviceBattery);
    }

    public void resetDeviceData() {
        this.deviceAltitude.postValue(Double.NaN);
        this.deviceBattery.postValue(Double.NaN);
        this.deviceHwVersion.postValue("");
        this.deviceTemp.postValue(Double.NaN);
    }


    public MutableLiveData<String> getDeviceHwVersion() {
        return this.deviceHwVersion;
    }

    public void setDeviceHwVersion(String deviceHwVersion) {
        this.deviceHwVersion.postValue(deviceHwVersion);
    }


    public MutableLiveData<Double> getDeviceAltitude() {
        return this.deviceAltitude;
    }

    public void setDeviceAltitude(Double deviceAltitude) {
        this.deviceAltitude.postValue(deviceAltitude);
    }


    public MutableLiveData<Double> getDeviceTemp() {
        return this.deviceTemp;
    }

    public void setDeviceTemp(Double deviceTemp) {
        this.deviceTemp.postValue(deviceTemp);
    }

    public MutableLiveData<Boolean> getDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun.postValue(dryRun);
    }
}
