package com.wally.wally.dal;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DummyDAL implements DataAccessLayer {
    private Set<Content> db;
    private Content mGiosLocation = new Content(new LatLng(41.71196838230613,44.75304298102856));
    public DummyDAL(int nContents) {
        db = new HashSet<>();
        for (int i = 0; i < nContents; i++) {
            generateDummyContent();
        }
    }

    public DummyDAL() {
        this(100);
    }

    private void generateDummyContent(){
        Random rand = new Random();
        double lat = 41.71 + rand.nextDouble()/10;
        double lng = 44.75 + rand.nextDouble()/10;
        save(new Content(new LatLng(lat, lng)));
    }

    @Override
    public void save(@NonNull Content c, @NonNull Callback<Boolean> statusCallback) {
        db.add(c);
        statusCallback.call(true, null);
    }

    @Override
    public void save(@NonNull Content c) {
        db.add(c);
    }

    @Override
    public void delete(@NonNull Content c, @NonNull Callback<Boolean> statusCallback) {
        db.remove(c);
        statusCallback.call(true, null);
    }

    @Override
    public void delete(@NonNull Content c) {
        db.remove(c);
    }

    @Override
    public void fetch(@NonNull Query query, @NonNull Callback<Collection<Content>> resultCallback) {
        if(!db.contains(mGiosLocation))
            db.add(mGiosLocation);
        resultCallback.call(new HashSet<Content>(db), null);
    }
}
