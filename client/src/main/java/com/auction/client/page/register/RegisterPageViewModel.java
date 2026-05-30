package com.auction.client.page.register;

import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;
import java.util.Map;

public class RegisterPageViewModel {

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public boolean validateRegistration(String username, String email, String password) {
        return username != null
                && !username.isBlank()
                && email != null
                && EMAIL_PATTERN.matcher(email.trim()).matches()
                && password != null
                && password.length() >= 8;
    }

    public User parseRegisterResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null || Boolean.FALSE.equals(response.get("success"))) {
                return null;
            }
            String dataJson = JsonMapper.toJson(response.get("data"));
            return JsonMapper.fromJson(dataJson, User.class);
        } catch (Exception e) {
            System.err.println("Failed to parse register response: " + e.getMessage());
            return null;
        }
    }

    public String parseErrorMessage(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response != null && response.containsKey("message")) {
                return String.valueOf(response.get("message"));
            }
        } catch (Exception ignored) {
        }
        return "Registration failed. Please try again.";
    }
}
