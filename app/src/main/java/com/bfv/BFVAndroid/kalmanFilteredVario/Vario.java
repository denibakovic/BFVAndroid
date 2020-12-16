package com.bfv.BFVAndroid.kalmanFilteredVario;

/**
 * Created by IntelliJ IDEA.
 * User: Alistair
 * Date: 29/04/2011
 * Time: 8:16:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class Vario {

    private double damp;
    private int windowSize;
    private boolean windowFull;

    private int r;
    private double var;

    private RegressionSlope regression;

    private double[][] window;

    public Vario() {}


    public Vario(double damp, int windowSize) {
        this.damp = damp;
        regression = new RegressionSlope();
        this.setWindowSize(windowSize);
    }


    public void setWindowSize(int windowSize){
        this.windowSize = windowSize;
        window = new double[windowSize][2];
        r = 0;
        regression.clear();
        windowFull = false;
        var = 0.0;
    }


    public double addData(double time, double alt) {

        if(windowFull){
            regression.removeData(window[r][0],window[r][1]);
        }

        window[r][0] = time;
        window[r][1] = alt;

        regression.addData(window[r][0],window[r][1]);

        r++;
        if(r == windowSize){
            r = 0;
            windowFull = true;
        }

        //rawVar
        double rawVar = regression.getSlope();

        if (!Double.isNaN(rawVar) && windowFull ){
            var = var + damp * (rawVar - var);
        }

        return var;
    }

    public double getVar() {
        return var;
    }
}
