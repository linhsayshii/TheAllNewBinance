package com.auction.server.dao;

import com.auction.core.users.User;
import com.auction.core.users.UserFactory;
import com.auction.server.dao.impl.IUserDao;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class UserDao implements IUserDao {
    @Override
    public boolean registerUser(User user) {
        String sql =
                "INSERT INTO users (username, password, full_name, email, balance, role, is_active,"
                        + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword()); // Password đã được hash ở user services
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setBigDecimal(5, user.getBalance());
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
        String sql =
                "UPDATE users SET username = ?, full_name = ?, email = ?, is_active = ?, updated_at"
                        + " = ? WHERE user_id = ?";

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
                    return mapUser(rs);
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
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("No user found" + e.getMessage());
        }
        return null;
    }

    /**
     * Khóa dòng vật lý tại Database Engine dành cho Transaction nguyên khối.
     * Bắt buộc sử dụng Connection dùng chung của Transaction hiện hành (FOR UPDATE).
     */
    public User findByIdForUpdate(Connection conn, Integer id) throws SQLException {
        String sql =
                "SELECT user_id, username, password, full_name, email, balance,"
                        + " locked_balance, role, is_active"
                        + " FROM users WHERE user_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Đồng bộ hóa số dư và số dư đóng băng từ Domain Model RAM xuống Database.
     * Bắt buộc sử dụng Connection dùng chung của Transaction hiện hành.
     */
    public boolean updateBalanceAndLockedBalance(Connection conn, User user) throws SQLException {
        String sql =
                "UPDATE users SET balance = ?, locked_balance = ?, updated_at = ?"
                        + " WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, user.getBalance());
            stmt.setBigDecimal(2, user.getLockedBalance());
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(4, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean holdBalance(Integer userId, double amount) {
        String sql =
                "UPDATE users SET balance = balance - ?, locked_balance = locked_balance + ?,"
                        + " updated_at = ? WHERE user_id = ? AND balance >= ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, BigDecimal.valueOf(amount));
            stmt.setBigDecimal(2, BigDecimal.valueOf(amount));
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(4, userId);
            stmt.setBigDecimal(5, BigDecimal.valueOf(amount));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot hold balance! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean refundBalance(Integer userId, double amount) {
        String sql =
                "UPDATE users SET balance = balance + ?, locked_balance = locked_balance - ?,"
                        + " updated_at = ? WHERE user_id = ? AND locked_balance >= ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, BigDecimal.valueOf(amount));
            stmt.setBigDecimal(2, BigDecimal.valueOf(amount));
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(4, userId);
            stmt.setBigDecimal(5, BigDecimal.valueOf(amount));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot refund balance! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean insertTransactionRecord(
            Connection conn,
            Integer userId,
            String type,
            BigDecimal amount,
            String status,
            String refId) throws SQLException {
        String sql =
                "INSERT INTO wallet_transactions (user_id, transaction_type, amount, status,"
                        + " reference_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, type);
            stmt.setBigDecimal(3, amount);
            stmt.setString(4, status);
            stmt.setString(5, refId);
            return stmt.executeUpdate() > 0;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return UserFactory.rehydrateUser(
                rs.getString("role"),
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getBigDecimal("balance"),        // Đọc dữ liệu dạng BigDecimal chính xác
                rs.getBigDecimal("locked_balance"), // Đọc dữ liệu dạng BigDecimal chính xác
                rs.getBoolean("is_active"));
    }
}
