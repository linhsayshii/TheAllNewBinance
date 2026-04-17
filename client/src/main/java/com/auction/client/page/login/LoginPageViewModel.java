package com.auction.client.page.login;

import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;

import java.util.Map;

public class LoginPageViewModel {

    public boolean validateCredentials(String username, String password) {
        return username != null && !username.isBlank()
            && password != null && password.length() >= 4;
    }

    /**
     * Parse the raw JSON response from the server into a User object.
     * Returns null if the response indicates failure or data is missing.
     */
    public User parseLoginResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null || Boolean.FALSE.equals(response.get("success"))) {
                return null;
            }
            String dataJson = JsonMapper.toJson(response.get("data"));
            return JsonMapper.fromJson(dataJson, User.class);
        } catch (Exception e) {
            System.err.println("Failed to parse login response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract the error message from a failed server response.
     */
    public String parseErrorMessage(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response != null && response.containsKey("message")) {
                return String.valueOf(response.get("message"));
            }
        } catch (Exception ignored) {}
        return "Đăng nhập thất bại. Vui lòng thử lại.";
    }
}