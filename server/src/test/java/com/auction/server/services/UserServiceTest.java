package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.auction.core.dto.user.LoginRequest;
import com.auction.core.dto.user.RegisterRequest;
import com.auction.core.dto.wallet.DepositRequest;
import com.auction.core.dto.wallet.WithdrawRequest;
import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.exception.wallet.WalletTransactionException;
import com.auction.core.users.User;
import com.auction.core.users.UserFactory;
import com.auction.core.utils.PasswordHasher;
import com.auction.server.dao.DBConnection;
import com.auction.server.dao.impl.IUserDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private IUserDao userDao;
    @Mock private Connection mockConnection;

    private UserService userService;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserService(userDao);

        testUser = UserFactory.rehydrateUser(
                "STANDARD", 10, "buyer", PasswordHasher.hash("oldPassword123"),
                "Buyer Ten", "buyer10@test.com",
                BigDecimal.valueOf(100.0), BigDecimal.ZERO, true
        );

        // Inject mock connection to bypass static thread-local DBConnection restrictions
        injectMockConnection();
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanupMockConnection();
    }

    private void injectMockConnection() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);

        java.lang.reflect.Field realHolderField = DBConnection.class.getDeclaredField("REAL_CONNECTION_HOLDER");
        Object realBase = unsafe.staticFieldBase(realHolderField);
        long realOffset = unsafe.staticFieldOffset(realHolderField);

        ThreadLocal<Connection> customHolder = ThreadLocal.withInitial(() -> mockConnection);
        unsafe.putObject(realBase, realOffset, customHolder);

        java.lang.reflect.Field proxyHolderField = DBConnection.class.getDeclaredField("PROXY_CONNECTION_HOLDER");
        Object proxyBase = unsafe.staticFieldBase(proxyHolderField);
        long proxyOffset = unsafe.staticFieldOffset(proxyHolderField);
        unsafe.putObject(proxyBase, proxyOffset, customHolder);
    }

    private void cleanupMockConnection() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);

        java.lang.reflect.Field realHolderField = DBConnection.class.getDeclaredField("REAL_CONNECTION_HOLDER");
        Object realBase = unsafe.staticFieldBase(realHolderField);
        long realOffset = unsafe.staticFieldOffset(realHolderField);
        unsafe.putObject(realBase, realOffset, new ThreadLocal<Connection>());

        java.lang.reflect.Field proxyHolderField = DBConnection.class.getDeclaredField("PROXY_CONNECTION_HOLDER");
        Object proxyBase = unsafe.staticFieldBase(proxyHolderField);
        long proxyOffset = unsafe.staticFieldOffset(proxyHolderField);
        unsafe.putObject(proxyBase, proxyOffset, new ThreadLocal<Connection>());
    }

    /**
     * Happy Path: Register user successfully.
     */
    @Test
    void should_RegisterUserSuccessfully_when_PayloadIsValid() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setPassword("password123");
        request.setFullname("New User");
        request.setEmail("newuser@test.com");

        when(userDao.findByUsername("newUser")).thenReturn(null);
        when(userDao.registerUser(any(User.class))).thenReturn(true);

        // Act
        User result = userService.registerUser(request).join();

        // Assert
        assertNotNull(result);
        assertEquals("newUser", result.getUsername());
        verify(userDao, times(1)).registerUser(any(User.class));
    }

    /**
     * Negative Path: Throw exception when username already exists.
     */
    @Test
    void should_ThrowException_when_RegisteringDuplicateUsername() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("buyer");
        request.setPassword("password123");
        request.setFullname("New User");
        request.setEmail("newuser@test.com");

        when(userDao.findByUsername("buyer")).thenReturn(testUser);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            userService.registerUser(request).join();
        });
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Username already exists", exception.getCause().getMessage());
    }

    /**
     * Happy Path: Authenticate user successfully when credentials are correct.
     */
    @Test
    void should_AuthenticateUser_when_CredentialsAreCorrect() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("buyer");
        request.setPassword("oldPassword123");

        when(userDao.findByUsername("buyer")).thenReturn(testUser);

        // Act
        User result = userService.login(request).join();

        // Assert
        assertNotNull(result);
        assertEquals("buyer", result.getUsername());
    }

    /**
     * Happy Path: Deposit balance successfully.
     */
    @Test
    void should_DepositBalanceAtomically_when_AmountIsPositive() throws SQLException {
        // Arrange
        DepositRequest request = new DepositRequest(10, BigDecimal.valueOf(250.0));

        when(userDao.findByIdForUpdate(any(Connection.class), eq(10)))
                .thenReturn(testUser);
        when(userDao.updateBalanceAndLockedBalance(any(Connection.class), any(User.class)))
                .thenReturn(true);
        when(userDao.insertTransactionRecord(
                any(), eq(10), eq("DEPOSIT"), eq(BigDecimal.valueOf(250.0)), eq("SUCCESS"), any()))
                .thenReturn(true);

        // Act
        userService.deposit(request).join();

        // Assert
        assertEquals(BigDecimal.valueOf(350.0), testUser.getBalance()); // 100 + 250
        verify(mockConnection, times(1)).commit();
    }

    /**
     * Negative Path: Rollback deposit when transaction log failed to insert.
     */
    @Test
    void should_RollbackDeposit_when_TransactionLoggerFails() throws SQLException {
        // Arrange
        DepositRequest request = new DepositRequest(10, BigDecimal.valueOf(250.0));

        when(userDao.findByIdForUpdate(any(Connection.class), eq(10)))
                .thenReturn(testUser);
        when(userDao.updateBalanceAndLockedBalance(any(Connection.class), any(User.class)))
                .thenReturn(true);
        when(userDao.insertTransactionRecord(
                any(), eq(10), eq("DEPOSIT"), eq(BigDecimal.valueOf(250.0)), eq("SUCCESS"), any()))
                .thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            userService.deposit(request).join();
        });
        assertTrue(exception.getCause() instanceof WalletTransactionException);
        assertEquals("Lưu vết giao dịch thất bại", exception.getCause().getMessage());

        verify(mockConnection, times(1)).rollback();
        verify(mockConnection, never()).commit();
    }

    /**
     * Boundary/Negative Path: Throw insufficient balance when withdrawing over balance limit.
     */
    @Test
    void should_ThrowInsufficientBalance_when_WithdrawingOverLimit() throws SQLException {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(10, BigDecimal.valueOf(150.0)); // 150 > 100 balance

        when(userDao.findByIdForUpdate(any(Connection.class), eq(10)))
                .thenReturn(testUser);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            userService.withdraw(request).join();
        });
        assertTrue(exception.getCause() instanceof InsufficientBalanceException);
        assertTrue(exception.getCause().getMessage().contains("Số dư khả dụng không đủ"));

        verify(mockConnection, times(1)).rollback();
        verify(mockConnection, never()).commit();
    }
}
