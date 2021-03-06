package com.wally.wally.userManager;

import com.wally.wally.datacontroller.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Meravici on 5/12/2016. yea
 */
public class CompoundUser implements SocialUser {
    private List<SocialUser> socialUsers;

    public CompoundUser() {
        socialUsers = new ArrayList<>();
    }

    public User getBaseUser() {
        if (socialUsers.size() > 0) {
            return socialUsers.get(0).getBaseUser();
        }
        return null;
    }

    public void addSocialUser(SocialUser user) {
        socialUsers.add(user);
    }

    @Override
    public String getDisplayName() {
        if(socialUsers.size() > 0){
            return socialUsers.get(0).getDisplayName();
        }
        return null;
    }

    @Override
    public String getFirstName() {
        if(socialUsers.size() > 0){
            return socialUsers.get(0).getFirstName();
        }
        return null;
    }

    @Override
    public String getAvatarUrl() {
        if (socialUsers.size() > 0) {
            return socialUsers.get(0).getAvatarUrl();
        }
        return null;
    }

    @Override
    public String getAvatarUrl(int size) {
        if (socialUsers.size() > 0) {
            return socialUsers.get(0).getAvatarUrl(size);
        }
        return null;
    }

    @Override
    public String getCoverUrl() {
        if (socialUsers.size() > 0) {
            return socialUsers.get(0).getCoverUrl();
        }
        return null;
    }

    @Override
    public List<SocialUser> getFriends() {
        final List<SocialUser> result = new ArrayList<>();
        for (SocialUser user : socialUsers) {
            result.addAll(user.getFriends());
        }
        return result;
    }

//    @Override
//    public void getFriends(final FriendsLoadListener friendsLoadListener) {
//        new AsyncTask<Void, Void, List<SocialUser>>() {
//            @Override
//            protected List<SocialUser> doInBackground(Void... params) {
//                final List<SocialUser> result = new ArrayList<>();
//                CountDownLatch latch = new CountDownLatch(socialUsers.size());
//                for (SocialUser user : socialUsers) {
//                    user.getFriends(new FriendsLoadListener() {
//                        @Override
//                        public void onFriendsLoad(List<SocialUser> friends) {
//                            updateFriends(result, friends);
//                        }
//                    });
//                }
//                try {
//                    latch.await();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return result;
//            }
//
//            @Override
//            protected void onPostExecute(List<SocialUser> result) {
//                super.onPostExecute(socialUsers);
//                friendsLoadListener.onFriendsLoad(result);
//            }
//        };
//    }

    @Override
    public SocialUser withDisplayName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocialUser withFirstName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocialUser withAvatar(String avatarUrl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocialUser withCover(String coverUrl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocialUser withFriends(List<SocialUser> friends) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "CompoundUser{" +
                "socialUsers=" + socialUsers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompoundUser that = (CompoundUser) o;

        return socialUsers.equals(that.socialUsers);

    }

    @Override
    public int hashCode() {
        return socialUsers.hashCode();
    }
}