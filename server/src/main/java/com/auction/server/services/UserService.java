package com.auction.server.services;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import com.auction.core.dao.IUserDao;
import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.user.UpdatePasswordRequest;
import com.auction.core.dto.user.UpdateProfileRequest;
import com.auction.core.services.IUserService;
import com.auction.core.users.StandardUser;
import com.auction.core.users.User;
import com.auction.core.utils.PasswordHasher;

public class UserService implements IUserService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final IUserDao userDao;

    public UserService(IUserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public CompletableFuture<User> registerUser(RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            validateRegisterRequest(request);

            String username = request.getUsername().trim();
            if (userDao.findByUsername(username) != null) {
                throw new IllegalArgumentException("Username already exists");
            }

            User user = new StandardUser(
                    null,
                    username,
                    PasswordHasher.hash(request.getPassword()),
                    request.getFullname().trim(),
                    request.getEmail().trim(),
                    0.0
            );

            if (!userDao.registerUser(user)) {
                throw new IllegalStateException("Failed to register user");
            }
            return user;
        });
    }

    @Override
    public CompletableFuture<User> login(LoginRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
                return null;
            }
            User user = userDao.findByUsername(request.getUsername().trim());
            if (user != null && PasswordHasher.verify(request.getPassword(), user.getPassword())) {
                return user;
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> updateProfile(UpdateProfileRequest request) {
        return CompletableFuture.runAsync(() -> {
            validateUpdateProfileRequest(request);

            User user = userDao.findById(request.getUserId());
            if (user == null) throw new IllegalArgumentException("User not found");

            String newUsername = request.getUsername().trim();
            User existingUser = userDao.findByUsername(newUsername);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Username already exists");
            }

            user.setUsername(newUsername);
            user.setFullName(request.getFullName().trim());
            user.setEmail(request.getEmail().trim());

            if (!userDao.updateUserInformation(user)) {
                throw new IllegalStateException("Failed to update user profile");
            }
        });
    }

    @Override
    public CompletableFuture<Void> changePassword(UpdatePasswordRequest request) {
        return CompletableFuture.runAsync(() -> {
            validateChangePasswordRequest(request);

            User user = userDao.findById(request.getUserId());
            if (user == null) throw new IllegalArgumentException("User not found");

            if (!PasswordHasher.verify(request.getOldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Old password is incorrect");
            }

            if (request.getOldPassword().equals(request.getNewPassword())) {
                throw new IllegalArgumentException("New password must be different from old password");
            }

            user.setPassword(PasswordHasher.hash(request.getNewPassword()));
            if (!userDao.changePassword(user)) {
                throw new IllegalStateException("Failed to change password");
            }
        });
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) throw new IllegalArgumentException("Request payload is required");
        if (isBlank(request.getUsername())) throw new IllegalArgumentException("Username is required");
        if (isBlank(request.getPassword())) throw new IllegalArgumentException("Password is required");
        if (request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (isBlank(request.getFullname())) throw new IllegalArgumentException("Full name is required");
        if (!isValidEmail(request.getEmail())) throw new IllegalArgumentException("Email format is invalid");
    }

    private void validateUpdateProfileRequest(UpdateProfileRequest request) {
        if (request == null) throw new IllegalArgumentException("Request payload is required");
        if (request.getUserId() <= 0) throw new IllegalArgumentException("User ID is invalid");
        if (isBlank(request.getUsername())) throw new IllegalArgumentException("Username is required");
        if (isBlank(request.getFullName())) throw new IllegalArgumentException("Full name is required");
        if (!isValidEmail(request.getEmail())) throw new IllegalArgumentException("Email format is invalid");
    }

    private void validateChangePasswordRequest(UpdatePasswordRequest request) {
        if (request == null) throw new IllegalArgumentException("Request payload is required");
        if (request.getUserId() <= 0) throw new IllegalArgumentException("User ID is invalid");
        if (isBlank(request.getOldPassword())) throw new IllegalArgumentException("Old password is required");
        if (isBlank(request.getNewPassword())) throw new IllegalArgumentException("New password is required");
        if (request.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }

    private boolean isValidEmail(String email) {
        return !isBlank(email) && SIMPLE_EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
