package com.bfv.BFVAndroid.fragments.dashboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;


public class RawDataFragment extends Fragment implements RawDataRecyclerView.LongClickListener {

    private RawDataRecyclerView rawDataRecyclerView;
    private ArrayList<String> rawDataArray;
    private RawDataViewAdapter rawDataAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the View
        View rootView = inflater.inflate(R.layout.fragment_raw_data, container, false);

        // Get sharedData ViewModel
        SharedDataViewModel sharedData = new ViewModelProvider(getActivity()).get(SharedDataViewModel.class);

        // Set shareData observers
        sharedData.getLastRawData().observe(getViewLifecycleOwner(), rawDataObserver);

        // Attach rawData Adapter to RecyclerView
        rawDataRecyclerView = rootView.findViewById(R.id.rawDataRecyclerView);

        rawDataRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        rawDataArray = sharedData.getRawDataArray();
        rawDataAdapter = new RawDataViewAdapter(getContext(), rawDataArray);

        rawDataRecyclerView.setAdapter(rawDataAdapter);

        rawDataRecyclerView.setLongClickListener(this);

        // Inflate the layout for this fragment
        return rootView;
    }


    /**
     * Observer for sharedData.rawData
     */
    private final Observer<String> rawDataObserver = new Observer<String>() {
        @Override
        public void onChanged(@Nullable String s) {
            rawDataAdapter.notifyDataSetChanged();
            rawDataRecyclerView.scrollToPosition(rawDataArray.size() -1 );
        }
    };


    @Override
    public void onRawDataLongClick(View view) {
        try {
            StringBuilder data = new StringBuilder();
            for (String str : rawDataArray) {
                data.append(str).append("\n");
            }

            ClipboardManager clipboard = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("BFV Data Stream", data);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getContext(), "Data Stream copied!", Toast.LENGTH_SHORT).show();
        }
        catch (ConcurrentModificationException e) {
            Toast.makeText(getContext(), "Please disconnect before coping!", Toast.LENGTH_LONG).show();
        }
    }
}