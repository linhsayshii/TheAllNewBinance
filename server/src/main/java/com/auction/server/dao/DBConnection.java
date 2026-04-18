package com.auction.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DB_NAME = "theallnewbinance";   // Database name
    private static final String USER = "binance";      // MySQL username
    private static final String PASSWORD = "PasswordCucManh!";  // MySQL password
    private static final String URL = "jdbc:mysql://localhost:3306/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // ThreadLocal for concurrent safe connection
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    private DBConnection() {}

    public static Connection getConnection() {
        Connection connection = connectionHolder.get();
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                connectionHolder.set(connection);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Cannot find MySQL Driver!");
        } catch (SQLException e) {
            System.err.println("Error: Cannot connect to DB!");
        }
        return connection;
    }

    public static void beginTransaction() throws SQLException {
        Connection conn = getConnection();
        if (conn != null) {
            conn.setAutoCommit(false);
        }
    }

    public static void commitTransaction() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn != null && !conn.isClosed()) {
            conn.commit();
            conn.setAutoCommit(true);
            conn.close();
            connectionHolder.remove();
        }
    }

    public static void rollbackTransaction() {
        Connection conn = connectionHolder.get();
        try {
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                conn.setAutoCommit(true);
                conn.close();
                connectionHolder.remove();
            }
        } catch (SQLException e) {
            System.err.println("Rollback error: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        Connection conn = connectionHolder.get();
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Close connection error: " + e.getMessage());
        } finally {
            connectionHolder.remove();
        }
    }
}
