package com.auction.server.controller;

import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.user.UpdatePasswordRequest;
import com.auction.core.dto.user.UpdateProfileRequest;
import com.auction.core.dto.wallet.DepositRequest;
import com.auction.core.dto.wallet.WithdrawRequest;
import com.auction.core.services.IUserService;
import com.auction.core.users.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
        return handleSync(
                request,
                Map.class,
                req -> {
                    if (req == null || !req.containsKey("userId")) {
                        throw new IllegalArgumentException("Invalid logout payload");
                    }
                    Integer userId = ((Number) req.get("userId")).intValue();
                    userService.logout(userId).join();
                    return "Logged out successfully";
                },
                "Logout failed");
    }

    /** Deposits funds into the authenticated user's wallet. */
    public CompletableFuture<String> deposit(String request) {
        return handleAsync(
                request,
                DepositRequest.class,
                req -> userService.deposit(req).thenApply(v -> (Object) req),
                "Nạp tiền thất bại");
    }

    /** Withdraws funds from the authenticated user's wallet. */
    public CompletableFuture<String> withdraw(String request) {
        return handleAsync(
                request,
                WithdrawRequest.class,
                req -> userService.withdraw(req).thenApply(v -> (Object) req),
                "Rút tiền thất bại");
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

    /**
     * Returns all users for admin management. Filters sensitive fields via toSafeUser().
     */
    public String getAllUsersForAdmin(String payload) {
        String requestPayload = (payload == null || payload.isBlank() || "null".equals(payload)) ? "{}" : payload;
        return handleSync(
                requestPayload,
                Map.class,
                req -> {
                    List<User> users = userService.getAllUsers().join();
                    return users.stream().map(this::toSafeUser).toList();
                },
                "Không thể tải danh sách người dùng.");
    }

    /**
     * Toggles the active/banned status of a target user. Admin-only endpoint.
     * Validates that targetUserId is present and valid before delegating to service layer.
     */
    public String toggleUserStatus(String payload) {
        return handleSync(
                payload,
                Map.class,
                req -> {
                    if (req == null || !req.containsKey("targetUserId")) {
                        throw new IllegalArgumentException("Thiếu mã định danh người dùng đích.");
                    }
                    Integer targetUserId = ((Number) req.get("targetUserId")).intValue();
                    boolean success = userService.toggleUserStatus(targetUserId).join();
                    if (!success) {
                        throw new IllegalStateException("Cập nhật trạng thái thất bại.");
                    }
                    return "Cập nhật trạng thái người dùng thành công.";
                },
                "Cập nhật trạng thái thất bại.");
    }
}
