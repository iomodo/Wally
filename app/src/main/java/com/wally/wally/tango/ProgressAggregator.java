package com.wally.wally.tango;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shota on 8/7/16.
 */
public class ProgressAggregator implements ProgressReporter, ProgressListener {

    private static final String TAG = ProgressAggregator.class.getSimpleName();
    private Map<ProgressReporter, Double> reporters;
    private Map<ProgressReporter, Double> progresses;
    private List<ProgressListener> listeners;
    private double weightSum;

    public ProgressAggregator() {
        reporters = new HashMap<>();
        progresses = new HashMap<>();
        listeners = new ArrayList<>();
        weightSum = 0;
    }

    @Override
    public void onProgressUpdate(ProgressReporter reporter, double progress) {
        progresses.put(reporter, progress);
        fireProgress(getProgress());
    }

    public void addProgressReporter(ProgressReporter reporter, double weight) {
        reporter.addProgressListener(this);
        reporters.put(reporter, weight);
        progresses.put(reporter, 0.0);
        weightSum += weight;
    }

    @Override
    public void forceReport() {
        fireProgress(1);
    }

    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    private void fireProgress(double progress) {
        for (ProgressListener listener : listeners) {
            listener.onProgressUpdate(this, progress);
        }
    }

    private double getProgress() {
        double res = 0;
        for (ProgressReporter listener : reporters.keySet()) {
            res += reporters.get(listener) * progresses.get(listener) / weightSum;
        }
        return res;
    }
}