package com.bfv.BFVAndroid.fragments.parameters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;
import com.bfv.BFVAndroid.bluetooth.BluetoothController;
import com.bfv.BFVAndroid.bluetooth.BluetoothProvider;

import java.util.Map;

import BFVlib.Command;


/**
 * https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 */
public class ParametersRecyclerAdapter extends RecyclerView.Adapter<ParametersRecyclerAdapter.ViewHolder> {

    private final Map<String, Command> mParameters;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final SharedDataViewModel sharedData;
    private final Context mContext;
    private final BluetoothController bluetoothController;

    // Command is passed into the constructor
    ParametersRecyclerAdapter(SharedDataViewModel sharedData, Context context, Map<String,
            Command> parameters, BluetoothController bluetoothController) {
        this.mInflater = LayoutInflater.from(context);
        this.mParameters = parameters;
        this.sharedData = sharedData;
        this.mContext = context;
        this.bluetoothController = bluetoothController;
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView parameterName;
        TextView parameterDescription;
        TextView parameterValue;
        TextView defaultValue;

        ViewHolder(View itemView) {
            super(itemView);
            parameterName = itemView.findViewById(R.id.parameterName);
            parameterDescription = itemView.findViewById(R.id.parameterDescription);
            parameterValue = itemView.findViewById(R.id.parameterValue);
            defaultValue = itemView.findViewById(R.id.parameterDefaultValue);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // Inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.parameters_recycler_view_row, parent, false);

        return new ViewHolder(view);
    }


    // Binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String parameterName = (String) mParameters.keySet().toArray()[position];
        Command parameter = mParameters.get(parameterName);

        holder.parameterName.setText(parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1));
        holder.parameterDescription.setText(parameter.getDescription());

        holder.defaultValue.setText(parameter.getDefaultValueAsString());

        if(parameter.hasValue()) {
            holder.parameterValue.setText(parameter.getValueAsString());

        } else {
            // We need to 'clear' value so it doesn't show up when we reuse holder for other items
            holder.parameterValue.setText("");
        }

        if(bluetoothController.getState() == BluetoothProvider.STATE_CONNECTED) {
            holder.parameterValue.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
        } else {
            holder.parameterValue.setTextColor(ContextCompat.getColor(mContext, R.color.colorGrey));
        }
    }

    // Total number of rows
    @Override
    public int getItemCount() {
        return mParameters.size();
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
        void onItemClick(View view, int position);
    }
}
