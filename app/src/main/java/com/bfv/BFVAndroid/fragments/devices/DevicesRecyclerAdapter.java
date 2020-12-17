package com.bfv.BFVAndroid.fragments.devices;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bfv.BFVAndroid.R;

import java.util.List;


/**
 * https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 */
public class DevicesRecyclerAdapter extends RecyclerView.Adapter<DevicesRecyclerAdapter.ViewHolder> {

    private final List<BluetoothDevice> mDevices;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // bluetoothDevices is passed into the constructor
    DevicesRecyclerAdapter(Context context, List<BluetoothDevice> bluetoothDevices) {
        this.mInflater = LayoutInflater.from(context);
        this.mDevices = bluetoothDevices;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.devices_recycler_view_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BluetoothDevice device = mDevices.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceAddr.setText(device.getAddress());

        if(device.getName().contains("BlueFly") || device.getName().contains("BFV")) {
            holder.deviceIcon.setVisibility(View.VISIBLE);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mDevices.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView deviceName;
        private final TextView deviceAddr;
        private final ImageView deviceIcon;

        ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddr = itemView.findViewById(R.id.device_addr);
            deviceIcon = itemView.findViewById(R.id.device_icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    /**
     * Convenient method for getting device at click position
     * @param id position of device
     * @return BluetoothDevice
     */
    public BluetoothDevice getItem(int id) {
        return mDevices.get(id);
    }


    /**
     * allows clicks events to be caught
     * @param itemClickListener click listener to set
     */
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }


    /**
     * Parent activity/fragment will implement this interface to respond to click events
     */
    public interface ItemClickListener {
        void onItemClick(int position);
    }
}
