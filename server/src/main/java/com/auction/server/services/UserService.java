package com.auction.server.services;

import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.user.UpdatePasswordRequest;
import com.auction.core.dto.user.UpdateProfileRequest;
import com.auction.core.dto.wallet.DepositRequest;
import com.auction.core.dto.wallet.WithdrawRequest;
import com.auction.core.exception.DomainException;
import com.auction.core.exception.user.UserNotFoundException;
import com.auction.core.exception.wallet.InvalidAmountException;
import com.auction.core.exception.wallet.WalletTransactionException;
import com.auction.core.services.IUserService;
import com.auction.core.users.User;
import com.auction.core.users.UserFactory;
import com.auction.core.utils.PasswordHasher;
import com.auction.server.concurrency.DBExecutor;
import com.auction.server.dao.DBConnection;
import com.auction.server.dao.impl.IUserDao;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class UserService implements IUserService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern SIMPLE_EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final IUserDao userDao;

    public UserService(IUserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public CompletableFuture<User> registerUser(RegisterRequest request) {
        return CompletableFuture.supplyAsync(
                () -> {
                    validateRegisterRequest(request);

                    String username = request.getUsername().trim();
                    if (userDao.findByUsername(username) != null) {
                        throw new IllegalArgumentException("Username already exists");
                    }

                    User user =
                            UserFactory.createNewStandard(
                                    username,
                                    PasswordHasher.hash(request.getPassword()),
                                    request.getFullname().trim(),
                                    request.getEmail().trim());

                    if (!userDao.registerUser(user)) {
                        throw new IllegalStateException("Failed to register user");
                    }
                    return user;
                });
    }

    @Override
    public CompletableFuture<User> login(LoginRequest request) {
        return CompletableFuture.supplyAsync(
                () -> {
                    if (request == null
                            || isBlank(request.getUsername())
                            || isBlank(request.getPassword())) {
                        return null;
                    }
                    final User user = userDao.findByUsername(request.getUsername().trim());
                    if (user != null
                            && PasswordHasher.verify(request.getPassword(), user.getPassword())) {
                        if (!Boolean.TRUE.equals(user.getIsActive())) {
                            throw new IllegalArgumentException(
                                    "Tài khoản của bạn đã bị vô hiệu hóa bởi Admin.");
                        }
                        return user;
                    }
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> updateProfile(UpdateProfileRequest request) {
        return CompletableFuture.runAsync(
                () -> {
                    validateUpdateProfileRequest(request);

                    User user = userDao.findById(request.getUserId());
                    if (user == null) {
                        throw new IllegalArgumentException("User not found");
                    }

                    String newUsername = request.getUsername().trim();
                    User existingUser = userDao.findByUsername(newUsername);
                    if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                        throw new IllegalArgumentException("Username already exists");
                    }

                    user.setUsername(newUsername);
                    user.setFullName(request.getFullName().trim());
                    user.setEmail(request.getEmail().trim());

                    if (!userDao.updateUserInformation(user)) {
                        throw new IllegalStateException("Failed to update user profile");
                    }
                });
    }

    @Override
    public CompletableFuture<Void> changePassword(UpdatePasswordRequest request) {
        return CompletableFuture.runAsync(
                () -> {
                    validateChangePasswordRequest(request);

                    User user = userDao.findById(request.getUserId());
                    if (user == null) {
                        throw new IllegalArgumentException("User not found");
                    }

                    if (!PasswordHasher.verify(request.getOldPassword(), user.getPassword())) {
                        throw new IllegalArgumentException("Old password is incorrect");
                    }

                    if (request.getOldPassword().equals(request.getNewPassword())) {
                        throw new IllegalArgumentException(
                                "New password must be different from old password");
                    }

                    user.setPassword(PasswordHasher.hash(request.getNewPassword()));
                    if (!userDao.changePassword(user)) {
                        throw new IllegalStateException("Failed to change password");
                    }
                });
    }

    @Override
    public CompletableFuture<Void> logout(Integer userId) {
        return CompletableFuture.runAsync(
                () -> {
                    if (userId == null || userId <= 0) {
                        throw new IllegalArgumentException("User ID is invalid");
                    }
                    User user = userDao.findById(userId);
                    if (user == null) {
                        throw new IllegalArgumentException("User not found");
                    }
                });
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload is required");
        }
        if (isBlank(request.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }
        if (isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (isBlank(request.getFullname())) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email format is invalid");
        }
    }

    private void validateUpdateProfileRequest(UpdateProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload is required");
        }
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("User ID is invalid");
        }
        if (isBlank(request.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }
        if (isBlank(request.getFullName())) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email format is invalid");
        }
    }

    private void validateChangePasswordRequest(UpdatePasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload is required");
        }
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("User ID is invalid");
        }
        if (isBlank(request.getOldPassword())) {
            throw new IllegalArgumentException("Old password is required");
        }
        if (isBlank(request.getNewPassword())) {
            throw new IllegalArgumentException("New password is required");
        }
        if (request.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }

    private boolean isValidEmail(String email) {
        return !isBlank(email) && SIMPLE_EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public CompletableFuture<Void> deposit(DepositRequest request) {
        return CompletableFuture.runAsync(
                () -> {
                    if (request == null
                            || request.getAmount() == null
                            || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidAmountException("Yêu cầu nạp tiền không hợp lệ");
                    }

                    try {
                        DBConnection.beginTransaction();
                        Connection conn = DBConnection.getConnection();

                        User user = userDao.findByIdForUpdate(conn, request.getUserId());
                        if (user == null) {
                            throw new UserNotFoundException("Đầu dùng không tồn tại");
                        }

                        user.deposit(request.getAmount());

                        if (!userDao.updateBalanceAndLockedBalance(conn, user)) {
                            throw new WalletTransactionException("Cập nhật số dư thất bại");
                        }

                        if (!userDao.insertTransactionRecord(
                                conn,
                                user.getId(),
                                "DEPOSIT",
                                request.getAmount(),
                                "SUCCESS",
                                UUID.randomUUID().toString())) {
                            throw new WalletTransactionException("Lưu vết giao dịch thất bại");
                        }

                        DBConnection.commitTransaction();
                    } catch (Exception e) {
                        DBConnection.rollbackTransaction();
                        if (e instanceof DomainException) {
                            throw (RuntimeException) e;
                        }
                        throw new WalletTransactionException(e.getMessage());
                    } finally {
                        DBConnection.closeConnection();
                    }
                },
                DBExecutor.getExecutor());
    }

    @Override
    public CompletableFuture<Void> withdraw(WithdrawRequest request) {
        return CompletableFuture.runAsync(
                () -> {
                    if (request == null
                            || request.getAmount() == null
                            || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidAmountException("Yêu cầu rút tiền không hợp lệ");
                    }

                    try {
                        DBConnection.beginTransaction();
                        Connection conn = DBConnection.getConnection();

                        User user = userDao.findByIdForUpdate(conn, request.getUserId());
                        if (user == null) {
                            throw new UserNotFoundException("Đầu dùng không tồn tại");
                        }

                        user.withdraw(request.getAmount());

                        if (!userDao.updateBalanceAndLockedBalance(conn, user)) {
                            throw new WalletTransactionException("Cập nhật số dư thất bại");
                        }

                        if (!userDao.insertTransactionRecord(
                                conn,
                                user.getId(),
                                "WITHDRAW",
                                request.getAmount(),
                                "SUCCESS",
                                UUID.randomUUID().toString())) {
                            throw new WalletTransactionException("Lưu vết giao dịch thất bại");
                        }

                        DBConnection.commitTransaction();
                    } catch (Exception e) {
                        DBConnection.rollbackTransaction();
                        if (e instanceof DomainException) {
                            throw (RuntimeException) e;
                        }
                        throw new WalletTransactionException(e.getMessage());
                    } finally {
                        DBConnection.closeConnection();
                    }
                },
                DBExecutor.getExecutor());
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> getWalletTransactions(Integer userId) {
        return CompletableFuture.supplyAsync(
                () -> userDao.getWalletTransactionsByUserId(userId), DBExecutor.getExecutor());
    }

    @Override
    public CompletableFuture<List<User>> getAllUsers() {
        return CompletableFuture.supplyAsync(
                () -> userDao.findAll(), DBExecutor.getExecutor());
    }

    @Override
    public CompletableFuture<Boolean> toggleUserStatus(Integer targetUserId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    final User user = userDao.findById(targetUserId);
                    if (user == null) {
                        throw new IllegalArgumentException("Không tìm thấy người dùng.");
                    }
                    if (user.getRole() == User.Role.ADMIN) {
                        throw new IllegalArgumentException(
                                "Không thể vô hiệu hóa tài khoản Admin hệ thống.");
                    }
                    final boolean newStatus = !Boolean.TRUE.equals(user.getIsActive());
                    final boolean success = userDao.updateActiveStatus(targetUserId, newStatus);
                    if (success && !newStatus) {
                        com.auction.server.network.BroadcastBroker.getInstance()
                                .forceLogoutAndDisconnectUser(targetUserId);
                    }
                    return success;
                },
                DBExecutor.getExecutor());
    }
}
