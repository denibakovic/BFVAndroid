package com.bfv.BFVAndroid.fragments.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bfv.BFVAndroid.R;

import java.util.List;


/**
 * https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 */
public class RawDataViewAdapter extends RecyclerView.Adapter<RawDataViewAdapter.ViewHolder> {
    private final List<String> mData;
    private final LayoutInflater mInflater;

    // data is passed into the constructor
    public RawDataViewAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.raw_data_recycler_view_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String rawDataItem = mData.get(position);
        holder.myTextView.setText(rawDataItem);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.recyclerViewRow);
        }

    }

}