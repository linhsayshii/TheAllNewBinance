package com.auction.server.dao;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DB_NAME = "theallnewbinance"; // Database name
    private static final String USER = "binance"; // MySQL username
    private static final String PASSWORD = "PasswordCucManh!"; // MySQL password
    private static final String URL =
            "jdbc:mysql://localhost:3306/"
                    + DB_NAME
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";

    // ThreadLocal to manage database connections
    private static final ThreadLocal<Connection> REAL_CONNECTION_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Connection> PROXY_CONNECTION_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_TRANSACTION_HOLDER =
            ThreadLocal.withInitial(() -> false);

    private DBConnection() {}

    public static Connection getConnection() {
        Connection realConn = REAL_CONNECTION_HOLDER.get();
        try {
            if (realConn == null || realConn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                realConn = DriverManager.getConnection(URL, USER, PASSWORD);
                REAL_CONNECTION_HOLDER.set(realConn);

                final Connection target = realConn;
                Connection proxyConn =
                        (Connection)
                                Proxy.newProxyInstance(
                                        Connection.class.getClassLoader(),
                                        new Class<?>[] {Connection.class},
                                        (proxy, method, args) -> {
                                            if ("close".equals(method.getName())) {
                                                if (IN_TRANSACTION_HOLDER.get()) {
                                                    return null;
                                                } else {
                                                    PROXY_CONNECTION_HOLDER.remove();
                                                    REAL_CONNECTION_HOLDER.remove();
                                                    return method.invoke(target, args);
                                                }
                                            }
                                            return method.invoke(target, args);
                                        });
                PROXY_CONNECTION_HOLDER.set(proxyConn);
            }
        } catch (Exception e) {
            System.err.println("Error: Cannot connect to DB! " + e.getMessage());
        }
        return PROXY_CONNECTION_HOLDER.get();
    }

    public static void beginTransaction() throws SQLException {
        Connection conn = getConnection();
        if (conn != null) {
            IN_TRANSACTION_HOLDER.set(true);
            // Must use real connection to setAutoCommit to avoid interceptor or errors
            Connection realConn = REAL_CONNECTION_HOLDER.get();
            realConn.setAutoCommit(false);
        }
    }

    public static void commitTransaction() throws SQLException {
        Connection realConn = REAL_CONNECTION_HOLDER.get();
        if (realConn != null && !realConn.isClosed()) {
            realConn.commit();
            realConn.setAutoCommit(true);
            realConn.close();
            REAL_CONNECTION_HOLDER.remove();
            PROXY_CONNECTION_HOLDER.remove();
            IN_TRANSACTION_HOLDER.set(false);
        }
    }

    public static void rollbackTransaction() {
        Connection realConn = REAL_CONNECTION_HOLDER.get();
        try {
            if (realConn != null && !realConn.isClosed()) {
                realConn.rollback();
                realConn.setAutoCommit(true);
                realConn.close();
                REAL_CONNECTION_HOLDER.remove();
                PROXY_CONNECTION_HOLDER.remove();
                IN_TRANSACTION_HOLDER.set(false);
            }
        } catch (SQLException e) {
            System.err.println("Rollback error: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        Connection realConn = REAL_CONNECTION_HOLDER.get();
        try {
            if (realConn != null && !realConn.isClosed()) {
                realConn.close();
            }
        } catch (SQLException e) {
            System.err.println("Close connection error: " + e.getMessage());
        } finally {
            REAL_CONNECTION_HOLDER.remove();
            PROXY_CONNECTION_HOLDER.remove();
            IN_TRANSACTION_HOLDER.set(false);
        }
    }
}
