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

public class UserController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    public String login(String request) {
        if (request == null) {
            return JsonMapper.toJson(errorResponse("Request payload is required"));
        }

        try {
            LoginRequest loginRequest = JsonMapper.fromJson(request, LoginRequest.class);
            if (loginRequest == null) {
                return JsonMapper.toJson(errorResponse("Invalid login payload"));
            }

            User user = userService.login(loginRequest).join();
            if (user == null) {
                return JsonMapper.toJson(errorResponse("Invalid username or password"));
            }

            return JsonMapper.toJson(successResponse(user));
        } catch (IllegalArgumentException ex) {
            return JsonMapper.toJson(errorResponse(ex.getMessage()));
        } catch (Exception ex) {
            return JsonMapper.toJson(errorResponse("Login failed"));
        }
    }

    public String register(String request) {
        if (request == null) {
            return JsonMapper.toJson(errorResponse("Request payload is required"));
        }

        try {
            RegisterRequest registerRequest = JsonMapper.fromJson(request, RegisterRequest.class);
            if (registerRequest == null) {
                return JsonMapper.toJson(errorResponse("Invalid register payload"));
            }

            User user = userService.registerUser(registerRequest).join();
            return JsonMapper.toJson(successResponse(user));
        } catch (IllegalArgumentException ex) {
            return JsonMapper.toJson(errorResponse(ex.getMessage()));
        } catch (Exception ex) {
            return JsonMapper.toJson(errorResponse("Register failed"));
        }
    }

    public String updateProfile(String request) {
        if (request == null) {
            return JsonMapper.toJson(errorResponse("Request payload is required"));
        }

        try {
            UpdateProfileRequest updateProfileRequest = JsonMapper.fromJson(request, UpdateProfileRequest.class);
            if (updateProfileRequest == null) {
                return JsonMapper.toJson(errorResponse("Invalid update profile payload"));
            }

            userService.updateProfile(updateProfileRequest).join();
            return JsonMapper.toJson(successResponse(updateProfileRequest));
        } catch (IllegalArgumentException ex) {
            return JsonMapper.toJson(errorResponse(ex.getMessage()));
        } catch (Exception ex) {
            return JsonMapper.toJson(errorResponse("Update profile failed"));
        }
    }

    public String changePassword(String request) {
        if (request == null) {
            return JsonMapper.toJson(errorResponse("Request payload is required"));
        }

        try {
            UpdatePasswordRequest updatePasswordRequest = JsonMapper.fromJson(request, UpdatePasswordRequest.class);
            if (updatePasswordRequest == null) {
                return JsonMapper.toJson(errorResponse("Invalid change password payload"));
            }

            userService.changePassword(updatePasswordRequest).join();
            return JsonMapper.toJson(successResponse(updatePasswordRequest));
        } catch (IllegalArgumentException ex) {
            return JsonMapper.toJson(errorResponse(ex.getMessage()));
        } catch (Exception ex) {
            return JsonMapper.toJson(errorResponse("Change password failed"));
        }
    }

    public String logout(String request) {
        if (request == null) {
            return JsonMapper.toJson(errorResponse("Request payload is required"));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = JsonMapper.fromJson(request, Map.class);
            if (payload == null || !payload.containsKey("userId")) {
                return JsonMapper.toJson(errorResponse("Invalid logout payload"));
            }

            Integer userId = ((Number) payload.get("userId")).intValue();
            userService.logout(userId).join();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logged out successfully");
            return JsonMapper.toJson(response);
        } catch (IllegalArgumentException ex) {
            return JsonMapper.toJson(errorResponse(ex.getMessage()));
        } catch (Exception ex) {
            return JsonMapper.toJson(errorResponse("Logout failed"));
        }
    }

    private Map<String, Object> successResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("fullName", user.getFullName());
        safeUser.put("email", user.getEmail());
        safeUser.put("balance", user.getBalance());
        safeUser.put("role", user.getRole());
        safeUser.put("isActive", user.getIsActive());

        response.put("success", true);
        response.put("data", safeUser);
        return response;
    }

    private Map<String, Object> successResponse(UpdateProfileRequest updateProfileRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", updateProfileRequest);
        return response;
    }

    private Map<String, Object> successResponse(UpdatePasswordRequest updatePasswordRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", updatePasswordRequest);
        return response;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
