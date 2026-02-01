package com.revpay.dao;

import com.revpay.utils.LoggerUtil;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static Connection connection = null;
    private static Properties properties = new Properties();
    private static final Logger logger = LoggerUtil.getLogger(DatabaseConnection.class);
    private static volatile boolean isInitializing = false; // Add this flag

    public static void initialize() {
        // Prevent multiple simultaneous initializations
        if (isInitializing) {
            logger.debug("Another thread is already initializing, waiting...");
            return;
        }

        isInitializing = true;
        try {
            if (connection != null && !connection.isClosed()) {
                logger.debug("Connection already exists and is open");
                return;
            }

            logger.info("Initializing database connection...");

            InputStream input = DatabaseConnection.class.getClassLoader()
                    .getResourceAsStream("application.properties");
            if (input == null) {
                logger.error("Unable to find application.properties file");
                throw new RuntimeException("Unable to find application.properties");
            }

            properties.load(input);
            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");

            logger.debug("Attempting to connect to: {}", url);

            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, username, password);

            logger.info("Database connection established successfully");

        } catch (IOException e) {
            logger.error("Failed to load application.properties", e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC driver not found", e);
            throw new RuntimeException(e);
        } catch (SQLException e) {
            logger.error("Failed to establish database connection", e);
            logger.error("SQL State: {}, Error Code: {}", e.getSQLState(), e.getErrorCode());
            throw new RuntimeException(e);
        } finally {
            isInitializing = false;
        }
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            logger.error("Error checking connection status, reinitializing...", e);
            initialize();
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.info("Database connection closed");
                }
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            } finally {
                connection = null; // Reset connection
            }
        }
    }

    public static Properties getProperties() {
        return properties;
    }

    // Method for JDBC appender (optional)
    public static Connection getLoggingConnection() throws SQLException {
        // Use a separate connection for logging to avoid conflicts
        String url = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");
        return DriverManager.getConnection(url, username, password);
    }
}