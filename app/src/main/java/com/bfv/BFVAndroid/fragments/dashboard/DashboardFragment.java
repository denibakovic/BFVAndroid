package com.bfv.BFVAndroid.fragments.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bfv.BFVAndroid.R;


public class DashboardFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the View
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        return rootView;
    }
}