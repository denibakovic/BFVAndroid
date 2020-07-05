package com.bfv.BFVAndroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import java.util.TreeMap;

import BFVlib.Command;


public class SendCommandRecyclerAdapter extends RecyclerView.Adapter<SendCommandRecyclerAdapter.ViewHolder> {

    private final TreeMap<String, Command> mCommands;
    private final LayoutInflater mInflater;
    private SendCommandRecyclerAdapter.ItemClickListener mClickListener;

    // Command is passed into the constructor
    SendCommandRecyclerAdapter(Context context, TreeMap<String, Command> commands) {
        this.mInflater = LayoutInflater.from(context);
        this.mCommands = commands;
    }

    // inflates the row layout from xml when needed
    @Override
    public SendCommandRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.send_command_recycler_view_row, parent, false);

        return new SendCommandRecyclerAdapter.ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(SendCommandRecyclerAdapter.ViewHolder holder, int position) {
        String commandName = (String) mCommands.keySet().toArray()[position];
        Command command = mCommands.get(commandName);

        holder.commandName.setText(commandName);
        holder.commandDescription.setText(command.getDescription());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mCommands.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView commandName;
        TextView commandDescription;

        ViewHolder(View itemView) {
            super(itemView);
            commandName = itemView.findViewById(R.id.commandName);
            commandDescription = itemView.findViewById(R.id.commandDescription);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onSendCommandItemClick(view, getAdapterPosition());
        }
    }


    /**
     * Convenient method for getting Command at click position
     * @param position position of Command
     * @return Command
     */
    public Command getItem(int position) {
        return mCommands.get((String) mCommands.keySet().toArray()[position]);
    }


    /**
     * allows clicks events to be caught
     * @param itemClickListener click listener to set
     */
    void setClickListener(SendCommandRecyclerAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }


    /**
     * Parent activity/fragment will implement this interface to respond to click events
     */
    public interface ItemClickListener {
        void onSendCommandItemClick(View view, int position);
    }
}
