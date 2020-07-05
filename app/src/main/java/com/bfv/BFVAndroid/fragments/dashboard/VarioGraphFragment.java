package com.bfv.BFVAndroid.fragments.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;

import java.util.Arrays;


public class VarioGraphFragment extends Fragment {
    private VarioGraph varioGraph;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get sharedData ViewModel
        SharedDataViewModel sharedData = new ViewModelProvider(getActivity()).get(SharedDataViewModel.class);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_vario_graph, container, false);
        FrameLayout frameLayout = rootView.findViewById(R.id.varioGraphContainer);

        // Set shareData observers
        sharedData.getDeviceAltitude().observe(getViewLifecycleOwner(), deviceAltitudeObserver);

        varioGraph = new VarioGraph(getContext());

        frameLayout.addView(varioGraph);

        return rootView;
    }


    /**
     * Observer for sharedData.deviceAltitude
     */
    private final Observer<Double> deviceAltitudeObserver = new Observer<Double>() {
        @Override
        public void onChanged(@Nullable Double altitude) {
            varioGraph.addData(altitude);
        }
    };


    private static class VarioGraph extends View{
        private final Paint paint = new Paint();

        private final double[] altitudeData;
        private boolean firstAlt = true;
        private double lastAlt;
        private double Yoffset;

        private int i = 0;
        private final int size = 1000;


        public VarioGraph(Context context) {
            super(context);
            altitudeData = new double[size];
            Yoffset = 205;
        }

        public void addData(double alt){
            if(firstAlt) {
                fillData(alt);
                firstAlt = false;
            } else {
                double altDamp = 0.05;
                this.altitudeData[i] = lastAlt * (1.0 - altDamp) + alt * altDamp;
                lastAlt = this.altitudeData[i];

                i++;
                if (i == size) {
                    i = 0;
                }
            }
            this.invalidate();
        }

        private void fillData(double alt){
            Arrays.fill(this.altitudeData, alt);
            lastAlt = alt;
            i = 0;
        }

        private double getMaxValue(double[] val){
            double max = Double.MIN_VALUE;

            for (double v : val) {
                if (v > max) {
                    max = v;
                }
            }

            return max;
        }

        private double getMinValue(double[] val){
            double min = Double.MAX_VALUE;

            for (double v : val) {
                if (v < min) {
                    min = v;
                }
            }

            return min;
        }

        /**
         *
         * @param altShape gets modified inside method!!
         */
        private void getAlt(double currentAlt, double scale, Path altShape, int xAlt, double[] altData, int i, int size, int xStep) {
            for (int p = i; p > (i - size + 1); p--) {
                int c = p - 1;
                if (c < 0) {
                    c = c + size;
                }

                float yAlt = (float) (Yoffset + (currentAlt - altData[c]) * 100 / scale);
                altShape.lineTo(xAlt, yAlt);
                xAlt += xStep;
            }
        }


        @Override
        public void onDraw(Canvas canvas) {
            paint.setAntiAlias(true);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);

            double min = getMinValue(altitudeData);
            double max = getMaxValue(altitudeData);

            double currentAlt = altitudeData[i];
            int xAlt;

            double scale = (max - min) * 0.1;
            if(scale < 0.5){
                scale = 0.5;
            }

            Yoffset = getHeight() / 2;

            //alt
            Path path = new Path();
            int xBorder = 1;
            xAlt = xBorder;
            path.moveTo(xBorder,(float) Yoffset);
            int xStep = 2;
            getAlt(currentAlt, scale, path, xAlt, altitudeData, i, size, xStep);

            paint.setColor(Color.BLACK);
            canvas.drawPath(path, paint);

            //canvas.scale((float)scale, (float)scale);
        }
    }


}