package com.wally.wally.datacontroller.content;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.wally.wally.datacontroller.Callback;
import com.wally.wally.datacontroller.DataAccessLayer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FirebaseDAL implements DataAccessLayer<Content, FirebaseQuery> {
    private Firebase fb;

    public FirebaseDAL(Firebase inner) {
        this.fb = inner;
    }

    @Override
    public void save(final Content c, final Callback<Boolean> statusCallback) {
        fb.push().setValue(c, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    c.setId(firebase.getKey());
                    statusCallback.call(true, null);
                } else {
                    statusCallback.call(false, firebaseError.toException());
                }
            }
        });
    }

    @Override
    public void save(Content c) {
        if (c.getId() == null) {
            Firebase reference = fb.push();
            reference.setValue(c);
            c.setId(reference.getKey());
        } else {
            fb.child(c.getId()).setValue(c);
        }
    }

    @Override
    public void delete(Content c, final Callback<Boolean> statusCallback) {
        fb.child(c.getId()).removeValue(new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    statusCallback.call(true, null);
                } else {
                    statusCallback.call(false, firebaseError.toException());
                }
            }
        });

    }

    @Override
    public void delete(Content c) {
        fb.child(c.getId()).removeValue();
    }

    @Override
    public void fetch(final FirebaseQuery query,
                      final Callback<Collection<Content>> resultCallback) {

        query.getTarget(fb).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Set<Content> result = new HashSet<>();
                for (DataSnapshot contentSnapshot: snapshot.getChildren()) {
                    Content content = contentSnapshot.getValue(Content.class);
                    content.setId(contentSnapshot.getKey());
                    result.add(content);
                }
                resultCallback.call(result, null);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                resultCallback.call(null, firebaseError.toException());
            }

        });
    }
}