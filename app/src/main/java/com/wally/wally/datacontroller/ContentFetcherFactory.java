package com.wally.wally.datacontroller;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.wally.wally.datacontroller.callbacks.FetchResultCallback;
import com.wally.wally.datacontroller.content.Content;
import com.wally.wally.datacontroller.fetchers.ContentFetcher;
import com.wally.wally.datacontroller.fetchers.FilteredFetcher;
import com.wally.wally.datacontroller.fetchers.KeyPager;
import com.wally.wally.datacontroller.fetchers.PagerChain;
import com.wally.wally.datacontroller.fetchers.QueryContentFetcher;
import com.wally.wally.datacontroller.fetchers.ValuePager;
import com.wally.wally.datacontroller.firebase.geofire.GeoHashQuery;
import com.wally.wally.datacontroller.firebase.geofire.GeoUtils;
import com.wally.wally.datacontroller.queries.AuthorQuery;
import com.wally.wally.datacontroller.queries.ContentQuery;
import com.wally.wally.datacontroller.queries.FirebaseQuery;
import com.wally.wally.datacontroller.queries.SharedWithQuery;
import com.wally.wally.datacontroller.user.User;
import com.wally.wally.datacontroller.utils.Predicate;

import java.util.Collections;
import java.util.Set;

public class ContentFetcherFactory {
    private final User current;
    private DatabaseReference contents;
    private DatabaseReference publicContents;
    private DatabaseReference sharedContents;


    public ContentFetcherFactory(DatabaseReference contents, User current) {
        this.current = current;
        this.contents = contents;
        this.publicContents = contents.child("Public");
        this.sharedContents = contents.child("Shared");
    }

    public ContentFetcher createForPrivate() {
        return new KeyPager(contents.child(current.getId().getId()));
    }

    public ContentFetcher createForPublic(User user) {
        FirebaseQuery authorQuery = new AuthorQuery(user.getId());
        ContentQuery query = new ContentQuery(authorQuery, publicContents);
        return new QueryContentFetcher(query);
    }

    public ContentFetcher createForPublic(LatLng center, double radiusKm) {
        ContentFetcher fetcher = createForLocation(center, radiusKm, publicContents);
        if (fetcher == null) { fetcher = new KeyPager(publicContents); }
        return fetcher;
    }

    public ContentFetcher createForSharedByMe() {
        FirebaseQuery authorQuery = new AuthorQuery(current.getId());
        ContentQuery query = new ContentQuery(authorQuery, sharedContents);
        return new QueryContentFetcher(query);
    }

    public ContentFetcher createForSharedWithMe(LatLng center, double radiusKm) {
        FirebaseQuery sharedWithQuery = new SharedWithQuery(current.getGgId());
        Predicate<Content> predicate = isLocationInRangePredicate(center, radiusKm);
        ContentQuery query = new ContentQuery(sharedWithQuery, sharedContents, predicate);
        return new QueryContentFetcher(query);
    }

    public ContentFetcher createForSharedWithMe(User user) {
        ContentFetcher sharedContentFetcher = createForSharedWithMe();
        Predicate<Content> hasAuthorPredicate = hasAuthorPredicate(user.getId().getId());
        return new FilteredFetcher(sharedContentFetcher, hasAuthorPredicate);
    }

    public ContentFetcher createForSharedWithMe() {
        FirebaseQuery sharedWithQuery = new SharedWithQuery(current.getGgId());
        ContentQuery query = new ContentQuery(sharedWithQuery, sharedContents);
        return new QueryContentFetcher(query);
    }

    private ContentFetcher createForLocation(
            LatLng center, double radiusKm, DatabaseReference target) {
        // We decided that too big radius (>2500 km)
        // means we don't need to filter by location
        if (radiusKm > Config.RADIUS_MAX_KM) { return null; }
        if (radiusKm <= 0) { return createTrivial(); }

        final double radius = radiusKm * 1000; // Convert to meters
        Set<GeoHashQuery> queries = GeoHashQuery.queriesAtLocation(center, radius);
        PagerChain chain = new PagerChain();
        for (GeoHashQuery query : queries) {
            String startKey = query.getStartValue();
            String endKey = query.getEndValue();
            chain.addPager(new ValuePager(target, "hash", startKey, endKey));
        }
        return new FilteredFetcher(chain, isLocationInRangePredicate(center, radiusKm));
    }

    public ContentFetcher createTrivial() {
        return new ContentFetcher() {
            @Override
            public void fetchPrev(int i, FetchResultCallback callback) {
                callback.onResult(Collections.<Content>emptySet());
            }

            @Override
            public void fetchNext(int i, FetchResultCallback callback) {
                callback.onResult(Collections.<Content>emptySet());
            }
        };
    }

    private Predicate<Content> isLocationInRangePredicate(
            final LatLng center, final double radiusKm) {
        return new Predicate<Content>() {
            private double radius = radiusKm * 1000;

            @Override
            public boolean test(Content target) {
                return GeoUtils.distance(target.getLocation(), center) < radius;
            }
        };
    }

    private Predicate<Content> hasAuthorPredicate(final String userId) {
        return new Predicate<Content>() {
            @Override
            public boolean test(Content target) {
                return userId.equals(target.getAuthorId());
            }
        };
    }
}