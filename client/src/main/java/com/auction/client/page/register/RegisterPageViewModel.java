package com.auction.client.page.register;

import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;

import java.util.Map;

public class RegisterPageViewModel {

    public boolean validateRegistration(String username, String email, String password) {
        return username != null && !username.isBlank()
                && email != null && email.contains("@")
                && password != null && password.length() >= 6;
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
        } catch (Exception ignored) {}
        return "Đăng ký thất bại. Vui lòng thử lại.";
    }
}