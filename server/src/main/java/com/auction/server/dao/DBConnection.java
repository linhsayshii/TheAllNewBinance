package com.auction.server.dao;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DB_NAME = "theallnewbinance"; // Database name
    private static final String USER = "binance"; // MySQL username
    private static final String PASSWORD = "PasswordCucManh!"; // MySQL password
    private static final String URL = "jdbc:mysql://localhost:3306/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // ThreadLocal để quản lý kết nối
    private static final ThreadLocal<Connection> realConnectionHolder = new ThreadLocal<>();
    private static final ThreadLocal<Connection> proxyConnectionHolder = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> inTransactionHolder = ThreadLocal.withInitial(() -> false);

    private DBConnection() {}

    public static Connection getConnection() {
        Connection realConn = realConnectionHolder.get();
        try {
            if (realConn == null || realConn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                realConn = DriverManager.getConnection(URL, USER, PASSWORD);
                realConnectionHolder.set(realConn);

                final Connection target = realConn;
                Connection proxyConn = (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[] { Connection.class },
                        (proxy, method, args) -> {
                            if ("close".equals(method.getName())) {
                                if (inTransactionHolder.get()) {
                                    return null;
                                } else {
                                    proxyConnectionHolder.remove();
                                    realConnectionHolder.remove();
                                    return method.invoke(target, args);
                                }
                            }
                            return method.invoke(target, args);
                        });
                proxyConnectionHolder.set(proxyConn);
            }
        } catch (Exception e) {
            System.err.println("Error: Cannot connect to DB! " + e.getMessage());
        }
        return proxyConnectionHolder.get();
    }

    public static void beginTransaction() throws SQLException {
        Connection conn = getConnection();
        if (conn != null) {
            inTransactionHolder.set(true);
            // Phải dùng real connection để setAutoCommit vì tránh interceptor hoặc lỗi
            Connection realConn = realConnectionHolder.get();
            realConn.setAutoCommit(false);
        }
    }

    public static void commitTransaction() throws SQLException {
        Connection realConn = realConnectionHolder.get();
        if (realConn != null && !realConn.isClosed()) {
            realConn.commit();
            realConn.setAutoCommit(true);
            realConn.close();
            realConnectionHolder.remove();
            proxyConnectionHolder.remove();
            inTransactionHolder.set(false);
        }
    }

    public static void rollbackTransaction() {
        Connection realConn = realConnectionHolder.get();
        try {
            if (realConn != null && !realConn.isClosed()) {
                realConn.rollback();
                realConn.setAutoCommit(true);
                realConn.close();
                realConnectionHolder.remove();
                proxyConnectionHolder.remove();
                inTransactionHolder.set(false);
            }
        } catch (SQLException e) {
            System.err.println("Rollback error: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        Connection realConn = realConnectionHolder.get();
        try {
            if (realConn != null && !realConn.isClosed()) {
                realConn.close();
            }
        } catch (SQLException e) {
            System.err.println("Close connection error: " + e.getMessage());
        } finally {
            realConnectionHolder.remove();
            proxyConnectionHolder.remove();
            inTransactionHolder.set(false);
        }
    }
}
