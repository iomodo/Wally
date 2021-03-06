package com.wally.wally.datacontroller.fetchers;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.wally.wally.datacontroller.DBController.Fetcher;
import com.wally.wally.datacontroller.DBController.ResultCallback;
import com.wally.wally.datacontroller.callbacks.FirebaseFetchResultCallback;
import com.wally.wally.datacontroller.queries.FirebaseQuery;
import com.wally.wally.objects.content.Content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ValuePager implements Fetcher {

    private final DatabaseReference contents;

    private boolean hasNext;
    private String nextKey, endKey;
    private String child;

    public ValuePager(DatabaseReference contents, String child, String startKey, String endKey) {
        this.contents = contents;
        hasNext = true;
        nextKey = "";
        this.child = child;
        this.endKey = endKey;
        if (startKey != null) {
            nextKey = startKey;
        }
    }

    /**
     * Warning: external synchronization is needed!
     * @param count number of following entries
     * @param callback that should be called with result
     */
    @Override
    public void fetchNext(final int count, final ResultCallback callback) {

        if (!hasNext) {
            callback.onResult(Collections.<Content>emptySet());
            return;
        }

        new FirebaseQuery() {
            @Override
            public Query getTarget(DatabaseReference ref) {
                Query target = ref.orderByChild(child).limitToFirst(count + 1).startAt(nextKey);
                return endKey == null ? target : target.endAt(endKey);
            }
        }.fetch(contents, new FirebaseFetchResultCallback(
                new ResultCallback() {
                    @Override
                    public void onResult(Collection<Content> result) {
                        List<Content> contents = new ArrayList<>();
                        contents.addAll(result);
                        if (result.size() < count) {
                            hasNext = false;
                        } else {
                            nextKey = contents.remove(contents.size() - 1).getId();
                        }
                        callback.onResult(contents);
                    }
                })
        );
    }
}