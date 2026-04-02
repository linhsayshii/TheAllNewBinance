package com.auction.core.services;

import com.auction.core.users.User;

public interface IUserService {
    User registerUser(String username, String password, String fullName, String email);
    User login(String username, String password);
    void updateProfile(Integer userId, String username, String fullName, String email);
    void changePassword(Integer userId, String oldPassword, String newPassword);
}
