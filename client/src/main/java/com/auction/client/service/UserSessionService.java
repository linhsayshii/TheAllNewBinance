package com.auction.client.service;

public class UserSessionService {

    private boolean authenticated;
    private String username = "guest";

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getUsername() {
        return username;
    }

    public void login(String username) {
        this.authenticated = true;
        this.username = username;
    }

    public void logout() {
        this.authenticated = false;
        this.username = "guest";
    }
}