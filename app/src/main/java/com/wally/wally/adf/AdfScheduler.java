package com.wally.wally.adf;

import android.util.Log;

import com.wally.wally.tango.ProgressListener;
import com.wally.wally.tango.ProgressReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AdfScheduler implements ProgressReporter {
    public static final String TAG = AdfScheduler.class.getSimpleName();
    public static final int DEFAULT_TIMEOUT = 1000;

    private boolean done;
    private long timeout;
    private AdfManager mAdfManager;
    private List<AdfSchedulerListener> callbackList;

    private ProgressListener listener;
    private int adfsSoFar;
    private Thread scheduler;

    public AdfScheduler(AdfManager adfManager) {
        done = false;
        adfsSoFar = 0;
        timeout = DEFAULT_TIMEOUT;
        this.mAdfManager = adfManager;
        callbackList = new ArrayList<>();
    }

    public AdfScheduler withTimeout(long timeoutMs) {
        this.timeout = timeoutMs;
        return this;
    }

    public AdfScheduler addListener(AdfSchedulerListener listener) {
        callbackList.add(listener);
        return this;
    }

    public void finish() {
        this.done = true;
        if (!scheduler.isInterrupted()) {
            scheduler.interrupt();
        }
        listener.onProgressUpdate(this, 1);
    }

    private void fireSuccess(AdfInfo info) {
        for (AdfSchedulerListener c : callbackList) {
            if (info != null) {
                Log.d(TAG, info.getUuid());
                c.onNewAdfSchedule(info);
            } else {
                Log.d(TAG, "end");
                c.onScheduleFinish();
            }
        }
    }

    public void start() {
        scheduler = new Thread(new Runnable() {
            @Override
            public void run() {
                schedulingLoop();
            }
        });
        scheduler.start();
    }

    private void schedulingLoop() {
        while (!done && !scheduler.isInterrupted()) {
            final CountDownLatch latch = new CountDownLatch(1);
            mAdfManager.getAdf(new AdfManager.AdfManagerStateListener() {
                @Override
                public void onAdfReady(AdfInfo info) {
                    if (done || scheduler.isInterrupted()) return;
                    fireSuccess(info);
                    fireProgress();
                    latch.countDown();
                }

                @Override
                public void onNoMoreAdfs() {
                    fireSuccess(null);
                }
            });

            try {
                latch.await();
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void fireProgress() {
        adfsSoFar++;
        double progress = (double) adfsSoFar / mAdfManager.getAdfTotalCount();
        listener.onProgressUpdate(this, progress);
    }

    @Override
    public void addProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public interface AdfSchedulerListener {
        void onNewAdfSchedule(AdfInfo info);

        void onScheduleFinish();
    }
}