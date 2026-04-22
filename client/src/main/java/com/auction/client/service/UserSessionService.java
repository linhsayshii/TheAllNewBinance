package com.auction.client.service;

import com.auction.core.users.User;

/**
 * Holds the authenticated user's data for the current session.
 * Use login(user) after a successful LOGIN response and logout() on sign-out.
 */
public class UserSessionService {

    private static UserSessionService instance;

    private User currentUser;

    private UserSessionService() {}

    public static synchronized UserSessionService getInstance() {
        if (instance == null) {
            instance = new UserSessionService();
        }
        return instance;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /** Call after receiving a successful LOGIN/REGISTER response from the server. */
    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }
}