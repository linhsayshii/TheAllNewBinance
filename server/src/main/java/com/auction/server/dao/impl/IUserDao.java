package com.auction.server.dao.impl;

import com.auction.core.users.User;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IUserDao {
    public boolean registerUser(User user);

    public boolean updateUserInformation(User user);

    public boolean changePassword(User user);

    public User findById(Integer id);

    public User findByUsername(String username);

    public boolean holdBalance(Integer userId, double amount);

    public boolean refundBalance(Integer userId, double amount);

    public User findByIdForUpdate(Connection conn, Integer id) throws SQLException;

    public boolean updateBalanceAndLockedBalance(Connection conn, User user) throws SQLException;

    public boolean insertTransactionRecord(
            Connection conn,
            Integer userId,
            String type,
            BigDecimal amount,
            String status,
            String refId)
            throws SQLException;

    public List<Map<String, Object>> getWalletTransactionsByUserId(Integer userId);

    public List<User> findAll();

    public boolean updateActiveStatus(Integer userId, boolean isActive);
}
