package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.auction.core.users.User;
import com.auction.server.dao.impl.IUserDao;

public class UserDao implements IUserDao {
    @Override
    public boolean registerUser(User user) {
        String sql = "INSERT INTO users (username, password, full_name, email, balance, role, is_active, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword()); // Password đã được hash ở user services
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setDouble(5, user.getBalance());
            stmt.setString(6, user.getRole().name());
            stmt.setBoolean(7, user.getIsActive());
            stmt.setTimestamp(8, Timestamp.valueOf(user.getCreatedAt()));

            int rowsInserted = stmt.executeUpdate();
            
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot save User!" + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateUserInformation(User user) {
        String sql = "UPDATE users SET username = ?, full_name = ?, email = ?, is_active = ?, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            stmt.setBoolean(4, user.getIsActive());
            stmt.setTimestamp(5, Timestamp.valueOf(user.getUpdatedAt()));
            stmt.setInt(6, user.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot update User!" + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean changePassword(User user) {
        String sql = "UPDATE users SET password = ?, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getPassword());
            stmt.setTimestamp(2, Timestamp.valueOf(user.getUpdatedAt()));
            stmt.setInt(3, user.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot change password!" + e.getMessage());
        }
        return false;
    }

    @Override
    public User findById(Integer id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getDouble("balance"),
                        User.Role.valueOf(rs.getString("role")),
                        rs.getBoolean("is_active")
                    );
                    user.setLockedBalance(rs.getDouble("locked_balance"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot find User!" + e.getMessage());
        }
        return null;
    }

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getDouble("balance"),
                        User.Role.valueOf(rs.getString("role")),
                        rs.getBoolean("is_active")
                    );
                    user.setLockedBalance(rs.getDouble("locked_balance"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("No user found" + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean holdBalance(Integer userId, double amount) {
        String sql = "UPDATE users SET balance = balance - ?, locked_balance = locked_balance + ?, updated_at = ? WHERE user_id = ? AND balance >= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(4, userId);
            stmt.setDouble(5, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot hold balance! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean refundBalance(Integer userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ?, locked_balance = locked_balance - ?, updated_at = ? WHERE user_id = ? AND locked_balance >= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(4, userId);
            stmt.setDouble(5, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot refund balance! " + e.getMessage());
        }
        return false;
    }
}
