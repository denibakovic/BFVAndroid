package com.bfv.BFVAndroid.fragments.dashboard;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;

import java.util.ArrayList;


public class RawDataFragment extends Fragment {

    private RecyclerView rawDataRecyclerView;
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
}