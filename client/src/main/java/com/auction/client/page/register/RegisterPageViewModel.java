package com.auction.client.page.register;

public class RegisterPageViewModel {

    public boolean validateRegistration(String username, String email, String password) {
        return username != null && !username.isBlank()
                && email != null && email.contains("@")
                && password != null && password.length() >= 6;
    }
}