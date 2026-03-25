package com.auction.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DB_NAME = "";   // Database name
    private static final String USER = "";      // MySQL username
    private static final String PASSWORD = "";  // MySQL password
    private static final String URL = "jdbc:mysql://localhost:3306/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static Connection connection;

    private DBConnection() {}


    // Singleton Pattern
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Load Driver cho MySQL 8+
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Connected to DB!");
            }
        // For Console log purpose
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Cannot find MySQL Driver!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error: Cannot connect to DB!");
            e.printStackTrace();
        }
        return connection;
    }
}
