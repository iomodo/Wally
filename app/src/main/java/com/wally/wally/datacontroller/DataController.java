package com.wally.wally.datacontroller;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wally.wally.datacontroller.callbacks.AggregatorCallback;
import com.wally.wally.datacontroller.callbacks.Callback;
import com.wally.wally.datacontroller.callbacks.FetchResultCallback;
import com.wally.wally.datacontroller.content.Content;
import com.wally.wally.datacontroller.content.FirebaseContent;
import com.wally.wally.datacontroller.fetchers.ContentFetcher;
import com.wally.wally.datacontroller.fetchers.KeyPager;
import com.wally.wally.datacontroller.fetchers.PagerChain;
import com.wally.wally.datacontroller.firebase.FirebaseDAL;
import com.wally.wally.datacontroller.firebase.geofire.GeoHashQuery;
import com.wally.wally.datacontroller.queries.FirebaseQuery;
import com.wally.wally.datacontroller.user.User;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DataController {
    public static final String TAG = DataController.class.getSimpleName();

    private static DataController instance;

    private User currentUser;
    private StorageReference storage;
    private DatabaseReference users, contents, rooms;

    private DataController(DatabaseReference database, StorageReference storage) {
        this.storage = storage;
        users = database.child(Config.USERS_NODE);
        contents = database.child(Config.CONTENTS_NODE);
        rooms = database.child(Config.ROOMS_NODE);

        // Debug calls will be deleted in the end
//        DebugUtils.refreshContents(contents, this);
        DebugUtils.sanityCheck(this);
    }

    public static DataController create() {
        if (instance == null) {
            instance = new DataController(
                    FirebaseDatabase.getInstance().getReference().child(Config.DATABASE_ROOT),
                    FirebaseStorage.getInstance().getReference().child(Config.STORAGE_ROOT)
            );
        }
        return instance;
    }

    private void uploadImage(String imagePath, String folder, final Callback<String> callback) {
        if (imagePath != null && imagePath.startsWith(Content.UPLOAD_URI_PREFIX)) {
            String imgUriString = imagePath.substring(Content.UPLOAD_URI_PREFIX.length());
            FirebaseDAL.uploadFile(storage.child(folder), imgUriString, callback);
        } else {
            callback.onResult(imagePath);
        }
    }

    private DatabaseReference getContentReference(Content c) {
        DatabaseReference target;
        if (c.isPublic()) {
            target = contents.child("Public");
        } else if (c.isPrivate()) {
            target = contents.child(c.getAuthorId());
        } else {
            target = contents.child("Shared");
        }
        return target.child(c.getId());
    }

    public void save(final Content c) {
        if (c.getId() != null) { delete(c); }
        final FirebaseContent content = new FirebaseContent(c);
        final DatabaseReference ref = getContentReference(c);
        uploadImage(
                c.getImageUri(),
                ref.getKey(),
                new Callback<String>() {
                    @Override
                    public void onResult(String result) {
                        c.withImageUri(result);
                        c.withId(content.save(ref));
                        String extendedId = ref.getParent().getKey() + ":" + ref.getKey();
                        rooms.child(c.getUuid()).child(extendedId).setValue(true);
                    }

                    @Override
                    public void onError(Exception e) {
                        // Omitted Implementation:
                        // If we are here image upload failed somehow
                        // We decided to leave this case for now!
                    }
                }
        );
    }

    public void delete(Content c) {
        if (c.getId() == null) {
            throw new IllegalArgumentException("Id of the content is null");
        }
        String extendedPublicId = "Public:" + c.getId();
        String extendedSharedId = "Shared:" + c.getId();
        String extendedPrivateId = c.getAuthorId() + ":" + c.getId();
        rooms.child(c.getUuid()).child(extendedPublicId).removeValue();
        rooms.child(c.getUuid()).child(extendedSharedId).removeValue();
        rooms.child(c.getUuid()).child(extendedPrivateId).removeValue();
        // TODO rewrite using .replace(':', '/') calls
        contents.child("Public").child(c.getId()).removeValue();
        contents.child("Shared").child(c.getId()).removeValue();
        contents.child(c.getAuthorId()).child(c.getId()).removeValue();
    }

    public void fetchByUUID(String uuid, final FetchResultCallback callback) {
        rooms.child(uuid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, Boolean>> indicator =
                        new GenericTypeIndicator<Map<String, Boolean>>(){};
                Map<String, Boolean> extendedIds = dataSnapshot.getValue(indicator);
                if (extendedIds == null) {
                    callback.onResult(Collections.<Content>emptySet());
                    return;
                }
                AggregatorCallback aggregator = new AggregatorCallback(callback)
                        .withExpectedCallbacks(extendedIds.size());
                for (String key : extendedIds.keySet()) {
                    fetchContentAt(key.replace(':', '/'), contents, aggregator);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    /**
     *
     * @deprecated use {@link #createFetcherForPublicContent()} ()} instead.
     */
    @Deprecated
    public ContentFetcher createPublicContentFetcher() {
        return createFetcherForPublicContent();
    }


    public ContentFetcher createFetcherForPublicContent() {
        return new KeyPager(contents.child("Public"));
    }

    public ContentFetcher createFetcherForPrivateContent() {
        User current = getCurrentUser();
        return new KeyPager(contents.child(current.getId().getId()));
    }

    public ContentFetcher createFetcherForVisibleContent(LatLng center, double r) {
        User current = getCurrentUser();
        // TODO stub implementation
        return createPublicContentFetcher(center, r);
    }

    public ContentFetcher createFetcherForMyContent(LatLng center, double r) {
        User current = getCurrentUser();
        // TODO stub implementation
        return createPublicContentFetcher(center, r);
    }

    public ContentFetcher createFetcherForUserContent(User user, LatLng center, double r) {
        User current = getCurrentUser();
        // TODO stub implementation
        return createPublicContentFetcher(center, r);
    }

    public User getCurrentUser() {
        if (currentUser != null) return currentUser;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        String id = user.getUid();
        // .get(1) assumes only one provider (Google)
        String ggId = user.getProviderData().get(1).getUid();
        currentUser = new User(id).withGgId(ggId);
        users.child(id).setValue(currentUser);
        return currentUser;
    }

    public void fetchUser(String id, final Callback<User> callback) {
        users.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                callback.onResult(dataSnapshot.getValue(User.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    ContentFetcher createPublicContentFetcher(LatLng center, double radiusKm) {
        if (radiusKm > Config.RADIUS_MAX_KM) {
            // We decided that too big radius (2500 km)
            // means we don't need to filter by location
            return createFetcherForPublicContent();
        }
        DatabaseReference target = contents.child("Public");
        final double radius = radiusKm * 1000; // Convert to meters
        Set<GeoHashQuery> queries = GeoHashQuery.queriesAtLocation(center, radius);
        PagerChain chain = new PagerChain();
        for (GeoHashQuery query : queries) {
            chain.addPager(new KeyPager(
                    target, query.getStartValue(), query.getEndValue()));
        }
        return chain;
    }

    private void fetchContentAt(String path, DatabaseReference ref,
                                final FetchResultCallback callback) {
        ref.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Content content = FirebaseQuery.firebaseContentFromSnapshot(dataSnapshot).toContent();
                callback.onResult(Collections.singleton(content));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }
}