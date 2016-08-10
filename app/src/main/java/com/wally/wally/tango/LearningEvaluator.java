package com.wally.wally.tango;


import android.util.Log;

import com.google.atap.tangoservice.TangoPoseData;
import com.wally.wally.config.Config;
import com.wally.wally.config.LearningEvaluatorConstants;

import org.rajawali3d.math.Quaternion;

import java.util.ArrayList;
import java.util.List;

public class LearningEvaluator implements TangoUpdater.ValidPoseListener, ProgressReporter {
    public static final String TAG = LearningEvaluator.class.getSimpleName();
    private static final double RATIO = 0.7;

    private int minTimeMs;
    private int maxTimeMs;
    private int minCellCount;
    private int minAngleCount;
    private int angleResolution;

    private List<Cell> cells;
    private long startTime;
    private long latestUpdateTime;
    private LearningEvaluatorListener listener;
    private boolean isFinished;
    private int progress;
    private int iteration;

    private ProgressListener progressListener;

    public LearningEvaluator(Config config) {
        minTimeMs = config.getInt(LearningEvaluatorConstants.MIN_TIME_S) * 1000;
        maxTimeMs = config.getInt(LearningEvaluatorConstants.MAX_TIME_S) * 1000;
        minCellCount = config.getInt(LearningEvaluatorConstants.MIN_CELL_COUNT);
        minAngleCount = config.getInt(LearningEvaluatorConstants.MIN_ANGLE_COUNT);
        angleResolution = config.getInt(LearningEvaluatorConstants.ANGLE_RESOLUTION);
    }

    public void addLearningEvaluatorListener(final LearningEvaluatorListener listener){
        this.listener = listener;
        isFinished = false;
        iteration++;
        start();
    }

    @Override
    public synchronized void onValidPose(TangoPoseData pose) {
        if (System.currentTimeMillis() - latestUpdateTime < 100 ||  isFinished){
            return;
        }
        latestUpdateTime = System.currentTimeMillis();

        int x = (int)(pose.translation[0]/Cell.CELL_SIZE_M);
        int y = (int)(pose.translation[1]/Cell.CELL_SIZE_M);
        Cell c = new Cell();
        c.x = x;
        c.y = y;
        Quaternion q = new Quaternion(pose.rotation[0],pose.rotation[1],pose.rotation[2],pose.rotation[3]);
        double yaw = Math.toDegrees(q.getYaw());
        if (yaw < 0) yaw += 360;
        int angleIndex = ((int)(yaw / 360 * angleResolution)) % angleResolution;
        if (!c.angleVisited[angleIndex]) {
            c.angleVisited[angleIndex] = true;
            progress++;
        }
        int index = cells.indexOf(c);
        if (index == -1){
            cells.add(c);
            progress++;
        } else {
            cells.get(index).angleVisited[angleIndex] = true;
        }
        //Log.d(TAG, "pose = " + pose + " yaw = " + yaw + ". getAngleCount = " + getAngleCount() + " size = " + cells.size() + "cells : " +cells);

        progressListener.onProgressUpdate(this, getProgress());


        if (canFinish() && !isFinished) {
            isFinished = true;
            progressListener.onProgressUpdate(this, 1);
            Log.d(TAG, "pose = " + pose + " yaw = " + yaw + ". getAngleCount = " + getAngleCount() + " size = " + cells.size() + "cells : " +cells);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    listener.onLearningFinish();
                }
            }).start();
        }
    }

    private double getProgress(){
        double n = ((double)progress)/(minCellCount+minAngleCount);
        double gavlili = 0;
        for (int i=0; i<iteration; i++){
            gavlili += (1-gavlili) * RATIO;
        }
        gavlili += (1-gavlili) * RATIO * n;
        return gavlili;
    }

    private boolean canFinish() {
        int angleCount = getAngleCount();
        int size = cells.size();
        long time = System.currentTimeMillis() - startTime;
        return angleCount >= minAngleCount && size >= minCellCount && time > minTimeMs || time > maxTimeMs;
    }

    private int getAngleCount(){
        int res = 0;
        for (Cell c : cells){
            for (int i = 0; i<angleResolution; i++){
                if (c.angleVisited[i]) res++;
            }
        }
        return res;
    }

    public LearningEvaluator start(){
        cells = new ArrayList<>();
        startTime = System.currentTimeMillis();
        latestUpdateTime = startTime;
        progress = 0;
        return this;
    }

    public LearningEvaluator stop() {
        Log.d(TAG, "stop() called");
        listener.onLearningFailed();
        return this;
    }

    @Override
    public void forceReport() {
        progressListener.onProgressUpdate(this, 1);
    }

    @Override
    public void addProgressListener(ProgressListener listener) {
        progressListener = listener;
    }


    public interface LearningEvaluatorListener{
        void onLearningFinish();
        void onLearningFailed();
    }

    class Cell{
        public static final double CELL_SIZE_M = 1; //Cell size in meters.
        public int x;
        public int y;
        public boolean[] angleVisited = new boolean[angleResolution];

        @Override
        public boolean equals(Object o){
            if (!(o instanceof Cell)) {
                return false;
            }
            Cell c = (Cell) o;
            return c.x == x && c.y == y;
        }

        @Override
        public String toString() {
            return "(" + x + "," +y + ")";
        }
    }
}