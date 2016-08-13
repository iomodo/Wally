package com.wally.wally.tango;

import android.content.Context;
import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoErrorException;
import com.projecttango.tangosupport.TangoSupport;


/**
 * Created by shota on 5/28/16.
 */
public class TangoFactory {
    public static final String TAG = TangoFactory.class.getSimpleName();

    private Context mContext;
    private Tango mTango;

    public TangoFactory(Context context) {
        mContext = context;
    }

    public Tango getTango(Runnable r) {
        return new Tango(mContext, r);
    }

    public Tango getTangoForLearning(final RunnableWithError r) {
        mTango = new Tango(mContext, new Runnable() {
            @Override
            public void run() {
                try {
                    TangoSupport.initialize();
                    connectTangoForLearning(mTango);
                    r.run();
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Cannot create tango! : " + e);
                    r.onError(e);
                }
            }
        });
        Log.d(TAG, "getTangoForLearning = " + mTango);
        return mTango;
    }

    public Tango getTango(final RunnableWithError r) {
        mTango = new Tango(mContext, new Runnable() {
            @Override
            public void run() {
                try {
                    TangoSupport.initialize();
                    connectTango(mTango);
                    r.run();
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Cannot create tango! : " + e);
                    r.onError(e);
                }
            }
        });
        Log.d(TAG, "getTango = " + mTango);
        return mTango;
    }

    public Tango getTangoWithUuid(final RunnableWithError r, final String uuid) {
        mTango = new Tango(mContext, new Runnable() {
            @Override
            public void run() {
                try {
                    TangoSupport.initialize();
                    connectTangoWithUuid(mTango, uuid);
                    r.run();
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Cannot create tango! : " + e);
                    r.onError(e);
                }
            }
        });
        Log.d(TAG, "getTangoWithUuid = " + mTango);
        return mTango;
    }

    private void connectTangoWithUuid(Tango mTango, String uuid) {
        TangoConfig config = getBasicConfig(mTango);
        config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, uuid);
        mTango.connect(config);
    }

    private void connectTango(Tango mTango) {
        TangoConfig config = getBasicConfig(mTango);
        // config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);
        mTango.connect(config);
    }

    private void connectTangoForLearning(Tango mTango) {
        TangoConfig config = getBasicConfig(mTango);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        mTango.connect(config);
    }

    private TangoConfig getBasicConfig(Tango mTango) {
        TangoConfig config = mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        return config;
    }


    public interface RunnableWithError {
        void run();

        void onError(Exception e);
    }
}
