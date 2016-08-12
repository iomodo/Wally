package com.wally.wally.tango;

import android.util.Log;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.rajawali.Pose;
import com.wally.wally.adf.AdfInfo;
import com.wally.wally.tango.states.TangoState;


/**
 * Created by shota on 8/9/16.
 *
 */
public class TangoDriver implements TangoState.StateChangeListener {
    private static final String TAG = TangoDriver.class.getSimpleName();

    private TangoState tangoState;

    public TangoDriver(TangoState startTangoState) {
        tangoState = startTangoState;
        tangoState.setStateChangeListener(this);
    }

    public synchronized void pause() {
        tangoState.pause();
    }

    public synchronized void resume() {
        tangoState.resume();
    }

    @Override
    public synchronized void onStateChange(TangoState nextTangoState) {
        Log.d(TAG, "onStateChange from =" + tangoState + " -- To [" + nextTangoState + "]");
        tangoState = nextTangoState;
        tangoState.setStateChangeListener(this);
    }

    public synchronized boolean isLearningState() {
        return tangoState.isLearningState();
    }

    public synchronized boolean isTangoLocalized() {
        return tangoState.isLocalized();
    }

    public synchronized boolean isTangoConnected() {
        return tangoState.isConnected();
    }

    /**
     * Finds plane pose in the middle of the screen.
     */
    public synchronized TangoPoseData findPlaneInMiddle() {
        return tangoState.findPlaneInMiddle();
    }

    public synchronized AdfInfo getAdf() {
        Log.d(TAG, "getAdf");
        return tangoState.getAdf();
    }

    public synchronized Pose getDevicePoseInFront() {
        return tangoState.getDevicePoseInFront();
    }

}
