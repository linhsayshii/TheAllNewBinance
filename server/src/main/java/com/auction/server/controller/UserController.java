package com.auction.server.controller;

import java.util.HashMap;
import java.util.Map;

import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.user.UpdatePasswordRequest;
import com.auction.core.dto.user.UpdateProfileRequest;
import com.auction.core.services.IUserService;
import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;

public class UserController extends BaseController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    public String login(String request) {
        return handleSync(request, LoginRequest.class, req -> {
            User user = userService.login(req).join();
            if (user == null) {
                throw new IllegalArgumentException("Invalid username or password");
            }
            return toSafeUser(user);
        }, "Login failed");
    }

    public String register(String request) {
        return handleSync(request, RegisterRequest.class, req -> {
            User user = userService.registerUser(req).join();
            return toSafeUser(user);
        }, "Register failed");
    }

    public String updateProfile(String request) {
        return handleSync(request, UpdateProfileRequest.class, req -> {
            userService.updateProfile(req).join();
            return req;
        }, "Update profile failed");
    }

    public String changePassword(String request) {
        return handleSync(request, UpdatePasswordRequest.class, req -> {
            userService.changePassword(req).join();
            return req;
        }, "Change password failed");
    }

    public String logout(String request) {
        if (request == null) {
            return ApiResponse.error("Request payload is required");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = JsonMapper.fromJson(request, Map.class);
            if (payload == null || !payload.containsKey("userId")) {
                return ApiResponse.error("Invalid logout payload");
            }

            Integer userId = ((Number) payload.get("userId")).intValue();
            userService.logout(userId).join();
            return ApiResponse.successMessage("Logged out successfully");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.error("Logout failed");
        }
    }

    /**
     * Maps a User entity to a safe Map that excludes sensitive fields (password).
     * Solves Feature Envy — this mapping logic stays in the controller
     * as a presentation concern, but is now extracted to a single reusable method.
     */
    private Map<String, Object> toSafeUser(User user) {
        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("fullName", user.getFullName());
        safeUser.put("email", user.getEmail());
        safeUser.put("balance", user.getBalance());
        safeUser.put("role", user.getRole());
        safeUser.put("isActive", user.getIsActive());
        return safeUser;
    }
}
