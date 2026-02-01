package com.revpay.dao;

import com.revpay.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import com.revpay.utils.LoggerUtil;
import org.apache.logging.log4j.Logger;

public class UserDAO {
    private Connection connection;
    private Properties properties;
    private static final Logger logger = LoggerUtil.getLogger(UserDAO.class);
    private static boolean daoInitialized = false; // Add this

    public UserDAO() {
        this.connection = DatabaseConnection.getConnection();
        this.properties = DatabaseConnection.getProperties();
        logger.debug("UserDAO initialized");
    }

    public Properties getProperties(){
        return this.properties;
    }

    public User createUser(User user) throws SQLException {
        LoggerUtil.logMethodEntry(logger, "createUser", user.getUsername(), user.getEmail(), user.getAccountType());

        String sql = "INSERT INTO users (username, email, phone_number, password_hash, " +
                "account_type, full_name, business_name, business_type, tax_id, " +
                "business_address, security_question1, security_answer1_hash, " +
                "security_question2, security_answer2_hash, wallet_balance, " +
                "created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhoneNumber());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getAccountType());
            stmt.setString(6, user.getFullName());
            stmt.setString(7, user.getBusinessName());
            stmt.setString(8, user.getBusinessType());
            stmt.setString(9, user.getTaxId());
            stmt.setString(10, user.getBusinessAddress());
            stmt.setString(11, user.getSecurityQuestion1());
            stmt.setString(12, user.getSecurityAnswer1Hash());
            stmt.setString(13, user.getSecurityQuestion2());
            stmt.setString(14, user.getSecurityAnswer2Hash());
            stmt.setDouble(15, user.getWalletBalance());
            stmt.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(17, Timestamp.valueOf(LocalDateTime.now()));

            logger.debug("Executing SQL: {}", sql);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                logger.error("Creating user failed, no rows affected for user: {}", user.getUsername());
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                    logger.info("User created successfully - ID: {}, Username: {}, Type: {}",
                            user.getId(), user.getUsername(), user.getAccountType());

                    // Log audit trail
                    LoggerUtil.logAudit(String.valueOf(user.getId()), "USER_CREATED",
                            null, "SUCCESS", "Account type: " + user.getAccountType());


                } else {
                    logger.error("Creating user failed, no ID obtained for user: {}", user.getUsername());
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }catch (SQLException e) {
            logger.error("Error creating user: {}", user.getUsername(), e);
            LoggerUtil.logError(logger, "Failed to create user", e,
                    "Username", user.getUsername(), "Email", user.getEmail());
            throw e;
        }

        LoggerUtil.logMethodExit(logger, "createUser", user.getId());
        return user;
    }

    public User getUserByEmailOrPhone(String identifier) throws SQLException {
        logger.debug("Looking up user by identifier: {}", identifier);

        String sql = "SELECT * FROM users WHERE email = ? OR phone_number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    logger.debug("User found - ID: {}, Username: {}", user.getId(), user.getUsername());
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving user by identifier: {}", identifier, e);
            throw e;
        }

        logger.debug("No user found for identifier: {}", identifier);
        return null;
    }

    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ?, phone_number = ?, " +
                "password_hash = ?, transaction_pin_hash = ?, account_type = ?, " +
                "full_name = ?, business_name = ?, business_type = ?, tax_id = ?, " +
                "business_address = ?, verification_documents = ?, wallet_balance = ?, " +
                "security_question1 = ?, security_answer1_hash = ?, " +
                "security_question2 = ?, security_answer2_hash = ?, is_verified = ?, " +
                "is_locked = ?, failed_login_attempts = ?, last_login = ?, " +
                "updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhoneNumber());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getTransactionPinHash());
            stmt.setString(6, user.getAccountType());
            stmt.setString(7, user.getFullName());
            stmt.setString(8, user.getBusinessName());
            stmt.setString(9, user.getBusinessType());
            stmt.setString(10, user.getTaxId());
            stmt.setString(11, user.getBusinessAddress());
            stmt.setString(12, user.getVerificationDocuments());
            stmt.setDouble(13, user.getWalletBalance());
            stmt.setString(14, user.getSecurityQuestion1());
            stmt.setString(15, user.getSecurityAnswer1Hash());
            stmt.setString(16, user.getSecurityQuestion2());
            stmt.setString(17, user.getSecurityAnswer2Hash());
            stmt.setBoolean(18, user.isVerified());
            stmt.setBoolean(19, user.isLocked());
            stmt.setInt(20, user.getFailedLoginAttempts());
            stmt.setTimestamp(21, user.getLastLogin() != null ?
                    Timestamp.valueOf(user.getLastLogin()) : null);
            stmt.setTimestamp(22, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(23, user.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateWalletBalance(int userId, double amount) throws SQLException {
        String sql = "UPDATE users SET wallet_balance = wallet_balance + ?, " +
                "updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean incrementFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, " +
                "updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean resetFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = 0, " +
                "is_locked = FALSE, updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean lockUserAccount(int userId) throws SQLException {
        String sql = "UPDATE users SET is_locked = TRUE, updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(int userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateTransactionPin(int userId, String pinHash) throws SQLException {
        String sql = "UPDATE users SET transaction_pin_hash = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pinHash);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setTransactionPinHash(rs.getString("transaction_pin_hash"));
        user.setAccountType(rs.getString("account_type"));
        user.setFullName(rs.getString("full_name"));
        user.setBusinessName(rs.getString("business_name"));
        user.setBusinessType(rs.getString("business_type"));
        user.setTaxId(rs.getString("tax_id"));
        user.setBusinessAddress(rs.getString("business_address"));
        user.setVerificationDocuments(rs.getString("verification_documents"));
        user.setWalletBalance(rs.getDouble("wallet_balance"));
        user.setSecurityQuestion1(rs.getString("security_question1"));
        user.setSecurityAnswer1Hash(rs.getString("security_answer1_hash"));
        user.setSecurityQuestion2(rs.getString("security_question2"));
        user.setSecurityAnswer2Hash(rs.getString("security_answer2_hash"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setLocked(rs.getBoolean("is_locked"));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return user;
    }

    public boolean checkIfUserExists(String identifier) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? OR phone_number = ? OR username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            stmt.setString(3, identifier);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean verifyPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    public String hashSecurityAnswer(String answer) {
        return BCrypt.hashpw(answer.toLowerCase(), BCrypt.gensalt());
    }

    public boolean verifySecurityAnswer(String answer, String hash) {
        return BCrypt.checkpw(answer.toLowerCase(), hash);
    }
}