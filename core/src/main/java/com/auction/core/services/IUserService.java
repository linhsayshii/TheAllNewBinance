package com.auction.core.services;

import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.user.UpdatePasswordRequest;
import com.auction.core.dto.user.UpdateProfileRequest;
import com.auction.core.users.User;
import java.util.concurrent.CompletableFuture;

public interface IUserService {
    CompletableFuture<User> registerUser(RegisterRequest request);

    CompletableFuture<User> login(LoginRequest request);

    CompletableFuture<Void> updateProfile(UpdateProfileRequest request);

    CompletableFuture<Void> changePassword(UpdatePasswordRequest request);

    CompletableFuture<Void> logout(Integer userId);
}
