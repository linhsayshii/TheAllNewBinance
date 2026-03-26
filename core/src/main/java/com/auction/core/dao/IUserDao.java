package com.auction.core.dao;

import com.auction.core.users.User;

public interface IUserDao {
    public boolean registerUser(User user);
    public boolean updateUserInformation(User user);
    public boolean changePassword(User user);
    public User findById(Integer id);
    public User findByUsername(String username);
}
