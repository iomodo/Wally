package com.wally.wally.userManager;

import com.wally.wally.datacontroller.user.User;

import java.util.List;

/**
 * Created by Meravici on 5/12/2016. yea
 */
public class FacebookUser extends AbstractSocialUser {
    public FacebookUser(User baseUser) {
        super(baseUser);
    }

    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFirstName() {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getAvatarUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAvatarUrl(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCoverUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SocialUser> getFriends() {
        throw new UnsupportedOperationException();
    }

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
}