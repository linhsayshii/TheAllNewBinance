package com.auction.client.page.login;

public class LoginPageViewModel {

    public boolean validateCredentials(String username, String password) {
        return username != null && !username.isBlank() && password != null && password.length() >= 4;
    }
}