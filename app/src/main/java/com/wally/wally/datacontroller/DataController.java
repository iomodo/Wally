package com.wally.wally.datacontroller;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wally.wally.datacontroller.firebase.FirebaseDAL;
import com.wally.wally.datacontroller.user.*;
import com.wally.wally.datacontroller.queries.*;
import com.wally.wally.datacontroller.callbacks.*;
import com.wally.wally.datacontroller.content.Content;
import com.wally.wally.datacontroller.content.FirebaseContent;

public class DataController {
    public static final String TAG = DataController.class.getSimpleName();

    private static final String DATABASE_ROOT = "Develop";
    private static final String STORAGE_ROOT = DATABASE_ROOT;
    private static final String USERS_NODE = "Users";
    private static final String CONTENTS_NODE = "Contents";

    private static DataController instance;

    private User currentUser;
    private StorageReference storage;
    private DatabaseReference users, contents;

    private DataController(DatabaseReference database, StorageReference storage) {
        this.storage = storage;
        users = database.child(USERS_NODE);
        contents = database.child(CONTENTS_NODE);
    }

    public static DataController create() {
        if (instance == null) {
            instance = new DataController(
                    FirebaseDatabase.getInstance().getReference().child(DATABASE_ROOT),
                    FirebaseStorage.getInstance().getReference().child(STORAGE_ROOT)
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

    public void save(final Content c) {
        final DatabaseReference ref;

        if (c.getId() == null) {
            ref = contents.push();
        } else {
            ref = contents.child(c.getId());
        }

        uploadImage(
                c.getImageUri(),
                ref.getKey(),
                new Callback<String>() {
                    @Override
                    public void onResult(String result) {
                        c.withImageUri(result);
                        new FirebaseContent(c).save(ref);
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
        new FirebaseContent(c).delete(contents);
    }

    public void fetchByBounds(LatLngBounds bounds, FetchResultCallback callback) {
//        new LatLngQuery(bounds).fetch(contents, new FirebaseFetchResultCallback(callback));
        // TODO stub implementation
        fetchPublicContent(callback);
    }

    public void fetchByUUID(String uuid, FetchResultCallback callback) {
        new UUIDQuery(uuid).fetch(contents, new FirebaseFetchResultCallback(callback));
    }

    public void fetchByAuthor(Id authorId, FetchResultCallback callback) {
        new AuthorQuery(authorId).fetch(contents, new FirebaseFetchResultCallback(callback));
    }

    public void fetchByAuthor(User author, FetchResultCallback resultCallback) {
        fetchByAuthor(author.getId(), resultCallback);
    }

    public void fetchPublicContent(FetchResultCallback callback) {
        new PublicityQuery(FirebaseContent.PUBLIC)
                .fetch(contents, new FirebaseFetchResultCallback(callback));
    }

    public void fetchAccessibleContent(User user, FetchResultCallback callback) {
        // TODO Stub implementation
        fetchPublicContent(callback);
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
}