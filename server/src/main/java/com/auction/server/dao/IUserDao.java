package com.auction.server.dao;

import com.auction.core.users.User;

public interface IUserDao {
    public boolean registerUser(User user);
    public boolean updateUserInformation(User user);
    public boolean changePassword(User user);
    public User findById(Integer id);
    public User findByUsername(String username);
    public boolean holdBalance(Integer userId, double amount);
    public boolean refundBalance(Integer userId, double amount);
}
