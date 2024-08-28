package com.dimon.catanbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Autowired
    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    //TODO CHANGE HERE
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        // Here you can extract user information and handle your logic
        // e.g., checking if the user exists in your database, etc.

        return oAuth2User;
    }
}
