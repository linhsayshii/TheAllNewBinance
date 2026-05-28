package com.auction.server.controller;

import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.user.UpdatePasswordRequest;
import com.auction.core.dto.user.UpdateProfileRequest;
import com.auction.core.dto.wallet.DepositRequest;
import com.auction.core.dto.wallet.WithdrawRequest;
import com.auction.core.exception.DomainException;
import com.auction.core.services.IUserService;
import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class UserController extends BaseController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    public String login(String request) {
        return handleSync(
                request,
                LoginRequest.class,
                req -> {
                    User user = userService.login(req).join();
                    if (user == null) {
                        throw new IllegalArgumentException("Invalid username or password");
                    }
                    return toSafeUser(user);
                },
                "Login failed");
    }

    public String register(String request) {
        return handleSync(
                request,
                RegisterRequest.class,
                req -> {
                    User user = userService.registerUser(req).join();
                    return toSafeUser(user);
                },
                "Register failed");
    }

    public String updateProfile(String request) {
        return handleSync(
                request,
                UpdateProfileRequest.class,
                req -> {
                    userService.updateProfile(req).join();
                    return req;
                },
                "Update profile failed");
    }

    public String changePassword(String request) {
        return handleSync(
                request,
                UpdatePasswordRequest.class,
                req -> {
                    userService.changePassword(req).join();
                    return req;
                },
                "Change password failed");
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

    /** Deposits funds into the authenticated user's wallet. */
    public String deposit(String request) {
        try {
            return handleAsync(
                            request,
                            DepositRequest.class,
                            req -> userService.deposit(req).thenApply(v -> req),
                            "Nạp tiền thất bại")
                    .join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof DomainException domainEx) {
                return ApiResponse.error(domainEx.getErrorCode(), domainEx.getMessage());
            }
            String msg = cause.getMessage() != null ? cause.getMessage() : "Nạp tiền thất bại";
            return ApiResponse.error(msg);
        }
    }

    /** Withdraws funds from the authenticated user's wallet. */
    public String withdraw(String request) {
        try {
            return handleAsync(
                            request,
                            WithdrawRequest.class,
                            req -> userService.withdraw(req).thenApply(v -> req),
                            "Rút tiền thất bại")
                    .join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof DomainException domainEx) {
                return ApiResponse.error(domainEx.getErrorCode(), domainEx.getMessage());
            }
            String msg = cause.getMessage() != null ? cause.getMessage() : "Rút tiền thất bại";
            return ApiResponse.error(msg);
        }
    }

    /** Returns wallet transaction history for the authenticated user. */
    public CompletableFuture<String> getWalletTransactions(Integer authenticatedUserId) {
        if (authenticatedUserId == null) {
            return CompletableFuture.completedFuture(ApiResponse.error("Unauthorized"));
        }
        return userService
                .getWalletTransactions(authenticatedUserId)
                .thenApply(ApiResponse::success)
                .exceptionally(ex -> ApiResponse.error("Failed to get wallet transactions"));
    }

    /**
     * Maps a User entity to a safe Map that excludes sensitive fields (password). Solves Feature
     * Envy — this mapping logic stays in the controller as a presentation concern, but is now
     * extracted to a single reusable method.
     */
    private Map<String, Object> toSafeUser(User user) {
        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("id", user.getId());
        safeUser.put("username", user.getUsername());
        safeUser.put("fullName", user.getFullName());
        safeUser.put("email", user.getEmail());
        safeUser.put("balance", user.getBalance());
        safeUser.put("lockedBalance", user.getLockedBalance());
        safeUser.put("role", user.getRole());
        safeUser.put("isActive", user.getIsActive());
        return safeUser;
    }
}
