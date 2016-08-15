package com.wally.wally.tango.states;

import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.wally.wally.adf.AdfInfo;
import com.wally.wally.events.WallyEvent;
import com.wally.wally.renderer.WallyRenderer;
import com.wally.wally.tango.TangoFactory;
import com.wally.wally.tango.TangoUpdater;

/**
 * Created by shota on 8/9/16.
 * Manages Tango for Ready State
 */
public class TangoForReadyState extends TangoState {
    private static final String TAG = TangoForReadyState.class.getSimpleName();

    private AdfInfo mAdfInfo;

    public TangoForReadyState(Executor executor,
                              TangoUpdater tangoUpdater,
                              TangoFactory tangoFactory,
                              WallyRenderer wallyRenderer,
                              TangoPointCloudManager pointCloudManager){
        super(executor, tangoUpdater, tangoFactory, wallyRenderer, pointCloudManager);
    }

    public TangoForReadyState withTangoAndAdf(Tango tango, AdfInfo adf) {
        Log.d(TAG, "withTangoAndAdf() called with: " + "tango = [" + tango + "], adf = [" + adf + "]");
        mTango = tango;
        this.mAdfInfo = adf;
        return this;
    }

    @Override
    protected void pauseHook() {
        mFailStateConnector.toNextState();
//        Log.d(TAG, "changeToSavedAdfState Thread = " + Thread.currentThread());
//        TangoState nextTango = ((TangoForSavedAdf)mTangoStatePool.get(TangoForSavedAdf.class)).withAdf(mAdfInfo);
//        changeState(nextTango);
    }

    @Override
    public TangoState withSuccessStateConnector(TangoStateConnector connector) {
        String msg = "TangoForReadyState does not support withSuccessStateConnector method";
        throw new UnsupportedOperationException(msg);
    }

    @Override
    protected void resumeHook() {
        Log.d(TAG, "resume Thread = " + Thread.currentThread());
        fireOnTangoReady();
    }

    @Override
    public AdfInfo getAdf() {
        return mAdfInfo;
    }

    private void fireOnTangoReady(){
        fireEvent(WallyEvent.createEventWithId(WallyEvent.TANGO_READY));
    }
}