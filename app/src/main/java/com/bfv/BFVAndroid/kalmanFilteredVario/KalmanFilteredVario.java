/*
 BlueFlyVario flight instrument - http://www.alistairdickie.com/blueflyvario/
 Copyright (C) 2011-2012 Alistair Dickie

 BlueFlyVario is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 BlueFlyVario is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with BlueFlyVario.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bfv.BFVAndroid.kalmanFilteredVario;




public class KalmanFilteredVario extends Vario{

    private double positionNoise = 0.015;
    private double accelerationNoise = 1.0;
    private double var;
    private final KalmanFilter kalmanFilter;


    public KalmanFilteredVario() {
        super();
        kalmanFilter = new KalmanFilter(accelerationNoise);
    }


    public KalmanFilteredVario(double positionNoise2, double accelerationNoise2) {
        this.positionNoise = positionNoise2;
        this.accelerationNoise = accelerationNoise2;
        kalmanFilter = new KalmanFilter(accelerationNoise2);
    }


    public double addData(double timeDelta, double alt) {
        kalmanFilter.update(alt, positionNoise, timeDelta);
        var = kalmanFilter.getXVel();

        return var;
    }

    public double getVar() {
        return var;
    }

    public double getAlpha() {
        return positionNoise;
    }

    public void setAlpha(double positionNoise) {
        this.positionNoise = positionNoise;
    }

    public void setBeta(double accelerationNoise) {
        this.accelerationNoise = accelerationNoise;
        kalmanFilter.setAccelerationVariance(accelerationNoise);
    }

    public double getBeta() {
        return accelerationNoise;
    }

    public double getAltitude(){
        return kalmanFilter.getXAbs();
    }

    public void reset() {kalmanFilter.reset();}
}



