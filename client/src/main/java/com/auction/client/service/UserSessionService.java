package com.auction.client.service;

import com.auction.core.users.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Holds the authenticated user's data for the current session. Use login(user) after a successful
 * LOGIN response and logout() on sign-out.
 */
public class UserSessionService {

    private static UserSessionService instance;

    private ObjectProperty<User> currentUserProperty = new SimpleObjectProperty<>();

    private UserSessionService() {}

    public static synchronized UserSessionService getInstance() {
        if (instance == null) {
            instance = new UserSessionService();
        }
        return instance;
    }

    public boolean isAuthenticated() {
        return currentUserProperty.get() != null;
    }

    public User getCurrentUser() {
        return currentUserProperty.get();
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUserProperty;
    }

    /** Call after receiving a successful LOGIN/REGISTER response from the server. */
    public void login(User user) {
        this.currentUserProperty.set(user);
    }

    public void logout() {
        try {
            if (isAuthenticated()) {
                Integer userId = getCurrentUser().getId();
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                if (userId != null) {
                    payload.put("userId", userId);
                }
                com.auction.client.service.NetworkService.getInstance()
                        .sendRequest(com.auction.core.protocol.EventType.LOGOUT, payload);
            }
        } catch (Exception e) {
            System.err.println("Error during network logout: " + e.getMessage());
        } finally {
            this.currentUserProperty.set(null);
        }
    }
}
