package com.wally.wally.tango.states;

import android.util.Log;

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

    public TangoForReadyState withPreviousState(TangoState state, AdfInfo adf){
        mTango = state.mTango;
        mAdfInfo = adf;
        mTangoUpdater = state.mTangoUpdater;
        mIsLocalized = state.mIsLocalized;
        mIsConnected = state.mIsConnected;
        mEventListeners = state.mEventListeners;
        mTangoFactory = state.mTangoFactory;
        mExecutor = state.mExecutor;
        mIntrinsics = state.mIntrinsics;
        mExtrinsics = state.mExtrinsics;
//        mCameraPoseTimestamp = state.mCameraPoseTimestamp;
//        mRgbTimestampGlThread = state.mRgbTimestampGlThread;
//        mConnectedTextureIdGlThread = state.mConnectedTextureIdGlThread;
//        mIsFrameAvailableTangoThread = state.mIsFrameAvailableTangoThread;
//        mRenderer = state.mRenderer;
//        mPointCloudManager = state.mPointCloudManager;
        return this;
    }

    @Override
    protected void pauseHook() {
        mFailStateConnector.toNextState();
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
