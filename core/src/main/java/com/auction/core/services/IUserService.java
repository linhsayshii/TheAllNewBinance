package com.auction.core.services;

import com.auction.core.dto.userservicedto.LoginRequest;
import com.auction.core.dto.userservicedto.RegisterRequest;
import com.auction.core.users.User;

public interface IUserService {
    User registerUser(RegisterRequest request);
    User login(LoginRequest request);
    void updateProfile(Integer userId, String username, String fullName, String email);
    void changePassword(Integer userId, String oldPassword, String newPassword);
}
