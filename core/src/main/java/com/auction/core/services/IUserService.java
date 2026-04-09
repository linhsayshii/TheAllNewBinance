package com.auction.core.services;

import com.auction.core.dto.UserService.LoginRequest;
import com.auction.core.dto.UserService.RegisterRequest;
import com.auction.core.dto.UserService.UpdatePasswordRequest;
import com.auction.core.dto.UserService.UpdateProfileRequest;
import com.auction.core.users.User;

public interface IUserService {
    User registerUser(RegisterRequest request);
    User login(LoginRequest request);
    void updateProfile(UpdateProfileRequest request);
    void changePassword(UpdatePasswordRequest request);
}
