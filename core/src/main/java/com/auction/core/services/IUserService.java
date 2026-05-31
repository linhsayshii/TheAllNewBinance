package com.auction.core.services;

import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.user.UpdatePasswordRequest;
import com.auction.core.dto.user.UpdateProfileRequest;
import com.auction.core.dto.wallet.DepositRequest;
import com.auction.core.dto.wallet.WithdrawRequest;
import com.auction.core.users.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IUserService {
    CompletableFuture<User> registerUser(RegisterRequest request);

    CompletableFuture<User> login(LoginRequest request);

    CompletableFuture<Void> updateProfile(UpdateProfileRequest request);

    CompletableFuture<Void> changePassword(UpdatePasswordRequest request);

    CompletableFuture<Void> logout(Integer userId);

    CompletableFuture<Void> deposit(DepositRequest request);

    CompletableFuture<Void> withdraw(WithdrawRequest request);

    CompletableFuture<List<Map<String, Object>>> getWalletTransactions(Integer userId);

    CompletableFuture<List<User>> getAllUsers();

    CompletableFuture<Boolean> toggleUserStatus(Integer targetUserId);
}
