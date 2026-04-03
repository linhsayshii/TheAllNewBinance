package com.auction.server.services;

import com.auction.core.dao.IUserDao;
import com.auction.core.dto.userservicedto.LoginRequest;
import com.auction.core.dto.userservicedto.RegisterRequest;
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
    public User registerUser(RegisterRequest request) {
        User user = new StandardUser(null, request.getUsername(), PasswordHasher.hash(request.getPassword()), request.getFullname(), request.getEmail(), 0.0);
        userDao.registerUser(user);
        return user;
    }

    @Override
    public User login(LoginRequest request) {
        User user = userDao.findByUsername(request.getUsername());
        if (user != null && user.getPassword().equals(PasswordHasher.hash(request.getPassword()))) {
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
