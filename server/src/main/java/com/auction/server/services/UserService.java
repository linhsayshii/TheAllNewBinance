package com.auction.server.services;

import com.auction.core.dao.IUserDao;
import com.auction.core.services.IUserService;
import com.auction.core.users.StandardUser;
import com.auction.core.users.User;
import com.auction.core.utils.PasswordHasher;

public class UserService implements IUserService {
    private final IUserDao userDao;
    public UserService(IUserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User registerUser(String username, String password, String fullName, String email) {
        User user = new StandardUser(null, username, PasswordHasher.hash(password), fullName, email, 0.0);
        userDao.registerUser(user);
        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user != null && user.getPassword().equals(PasswordHasher.hash(password))) {
            return user;
        }
        return null;
    }

    @Override
    public void updateProfile(Integer userId, String username, String fullName, String email) {
        User user = userDao.findById(userId);
        if (user != null) {
            user.setUsername(username);
            user.setFullName(fullName);
            user.setEmail(email);
            userDao.updateUserInformation(user);
        }
    }

    @Override
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userDao.findById(userId);
        if (user != null && user.getPassword().equals(PasswordHasher.hash(oldPassword))) {
            user.setPassword(PasswordHasher.hash(newPassword));
            userDao.changePassword(user);
        }
    }
}
