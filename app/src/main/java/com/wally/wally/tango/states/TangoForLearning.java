package com.wally.wally.tango.states;

import android.util.Log;

import com.projecttango.tangosupport.TangoPointCloudManager;
import com.wally.wally.adf.AdfInfo;
import com.wally.wally.events.WallyEvent;
import com.wally.wally.renderer.WallyRenderer;
import com.wally.wally.tango.LearningEvaluator;
import com.wally.wally.tango.TangoFactory;
import com.wally.wally.tango.TangoUpdater;

/**
 * Created by shota on 8/9/16.
 * Tango state for learning
 */
public class TangoForLearning extends TangoState {
    private static final String TAG = TangoForLearning.class.getSimpleName();

    private AdfInfo mAdfInfo;
    private LearningEvaluator mLearningEvaluator;

    public TangoForLearning(Executor executor,
                            TangoUpdater tangoUpdater,
                            TangoFactory tangoFactory,
                            WallyRenderer wallyRenderer,
                            LearningEvaluator evaluator,
                            TangoPointCloudManager pointCloudManager) {
        super(executor, tangoUpdater, tangoFactory, wallyRenderer, pointCloudManager);
        mLearningEvaluator = evaluator;
    }

    @Override
    protected void pauseHook() {
        Log.d(TAG, "pause Thread = " + Thread.currentThread());
    }

    @Override
    protected void resumeHook() {
        Log.d(TAG, "startLearning Thread = " + Thread.currentThread());
        mLearningEvaluator.addLearningEvaluatorListener(getLearningEvaluatorListener());
        mTangoUpdater.addValidPoseListener(mLearningEvaluator);
        mTango = mTangoFactory.getTangoForLearning(getTangoInitializer());
        Log.d(TAG, "resume() mTango = " + mTango);
        fireStartLearning();
    }

    @Override
    public boolean isLearningState() {
        return true;
    }

    private LearningEvaluator.LearningEvaluatorListener getLearningEvaluatorListener(){
        return new LearningEvaluator.LearningEvaluatorListener() {
            @Override
            public void onLearningFinish() {
                if (mIsLocalized) {
                    mTangoUpdater.removeValidPoseListener(mLearningEvaluator);
                    finishLearning();
                } else {
                    onLearningFailed();
                }
            }

            @Override
            public void onLearningFailed() {
                mTangoUpdater.removeValidPoseListener(mLearningEvaluator);
                mFailStateConnector.toNextState();
//                pause();
//                resume();
            }
        };
    }

    private synchronized void finishLearning() {
        Log.d(TAG, "finishLearning");
        mAdfInfo = saveAdf();
        fireFinishLearning();
        mSuccessStateConnector.toNextState();
//        changeToLearnedAdfState(info);
    }

    @Override
    public AdfInfo getAdf() {
        return mAdfInfo;
    }

    private void changeToLearnedAdfState(AdfInfo info){
//        Log.d(TAG, "changeToLearnedAdfState with: adf = [" + info + "]");
//        pause();
//        Log.d(TAG, "changeToLearnedAdfState after pause");
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        TangoState nextTango = ((TangoForLearnedAdf)mTangoStatePool.get(TangoForLearnedAdf.class)).withAdf(info);
//        changeState(nextTango);
//        Log.d(TAG, "changeToLearnedAdfState after change");
//        nextTango.resume();
//        Log.d(TAG, "changeToLearnedAdfState after resume");
    }

    private AdfInfo saveAdf() {
        Log.d(TAG, "saveAdf");
        String uuid = mTango.saveAreaDescription();
        return new AdfInfo().withUuid(uuid);
    }

    private void fireStartLearning(){
        fireEvent(WallyEvent.createEventWithId(WallyEvent.LEARNING_START));
    }

    private void fireFinishLearning(){
        fireEvent(WallyEvent.createEventWithId(WallyEvent.LEARNING_FINISH));
    }

}