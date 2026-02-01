package com.revpay.dao;

import com.revpay.models.PaymentMethod;
import com.revpay.services.EncryptionService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodDAO {
    private Connection connection;
    private EncryptionService encryptionService;

    public PaymentMethodDAO() {
        this.connection = DatabaseConnection.getConnection();
        this.encryptionService = new EncryptionService();
    }

    public PaymentMethod createPaymentMethod(PaymentMethod paymentMethod) throws SQLException {
        String sql = "INSERT INTO payment_methods (user_id, card_type, card_number_encrypted, " +
                "card_holder_name, expiry_month, expiry_year, cvv_encrypted, " +
                "is_default, is_active, bank_name, account_number_encrypted, " +
                "routing_number, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, paymentMethod.getUserId());
            stmt.setString(2, paymentMethod.getCardType());
            stmt.setString(3, paymentMethod.getCardNumberEncrypted());
            stmt.setString(4, paymentMethod.getCardHolderName());
            stmt.setInt(5, paymentMethod.getExpiryMonth());
            stmt.setInt(6, paymentMethod.getExpiryYear());
            stmt.setString(7, paymentMethod.getCvvEncrypted());
            System.out.println("[PaymentMethodDAO] Encrypted CVV: "+paymentMethod.getCvvEncrypted());
            stmt.setBoolean(8, paymentMethod.isDefault());
            stmt.setBoolean(9, paymentMethod.isActive());
            stmt.setString(10, paymentMethod.getBankName());
            stmt.setString(11, paymentMethod.getAccountNumberEncrypted());
            stmt.setString(12, paymentMethod.getRoutingNumber());
            stmt.setTimestamp(13, Timestamp.valueOf(paymentMethod.getCreatedAt()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payment method failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    paymentMethod.setId(generatedKeys.getInt(1));
                }
            }
        }
        return paymentMethod;
    }

    public List<PaymentMethod> getPaymentMethodsByUserId(int userId) throws SQLException {
        List<PaymentMethod> methods = new ArrayList<>();
        String sql = "SELECT * FROM payment_methods WHERE user_id = ? AND is_active = TRUE ORDER BY is_default DESC, created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    methods.add(mapResultSetToPaymentMethod(rs));
                }
            }
        }
        return methods;
    }

    public PaymentMethod getPaymentMethodById(int id) throws SQLException {
        String sql = "SELECT * FROM payment_methods WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaymentMethod(rs);
                }
            }
        }
        return null;
    }

    public PaymentMethod getDefaultPaymentMethod(int userId) throws SQLException {
        String sql = "SELECT * FROM payment_methods WHERE user_id = ? AND is_default = TRUE AND is_active = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaymentMethod(rs);
                }
            }
        }
        return null;
    }

    public boolean updatePaymentMethod(PaymentMethod paymentMethod) throws SQLException {
        String sql = "UPDATE payment_methods SET card_type = ?, card_number_encrypted = ?, " +
                "card_holder_name = ?, expiry_month = ?, expiry_year = ?, " +
                "cvv_encrypted = ?, is_default = ?, is_active = ?, bank_name = ?, " +
                "account_number_encrypted = ?, routing_number = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, paymentMethod.getCardType());
            stmt.setString(2, paymentMethod.getCardNumberEncrypted());
            stmt.setString(3, paymentMethod.getCardHolderName());
            stmt.setInt(4, paymentMethod.getExpiryMonth());
            stmt.setInt(5, paymentMethod.getExpiryYear());
            stmt.setString(6, paymentMethod.getCvvEncrypted());
            stmt.setBoolean(7, paymentMethod.isDefault());
            stmt.setBoolean(8, paymentMethod.isActive());
            stmt.setString(9, paymentMethod.getBankName());
            stmt.setString(10, paymentMethod.getAccountNumberEncrypted());
            stmt.setString(11, paymentMethod.getRoutingNumber());
            stmt.setInt(12, paymentMethod.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean setDefaultPaymentMethod(int userId, int paymentMethodId) throws SQLException {
        // First, reset all payment methods for this user to non-default
        String resetSql = "UPDATE payment_methods SET is_default = FALSE WHERE user_id = ?";

        try (PreparedStatement resetStmt = connection.prepareStatement(resetSql)) {
            resetStmt.setInt(1, userId);
            resetStmt.executeUpdate();
        }

        // Then set the specified method as default
        String setSql = "UPDATE payment_methods SET is_default = TRUE WHERE id = ? AND user_id = ?";

        try (PreparedStatement setStmt = connection.prepareStatement(setSql)) {
            setStmt.setInt(1, paymentMethodId);
            setStmt.setInt(2, userId);

            return setStmt.executeUpdate() > 0;
        }
    }

    public boolean deactivatePaymentMethod(int paymentMethodId) throws SQLException {
        String sql = "UPDATE payment_methods SET is_active = FALSE WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, paymentMethodId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deletePaymentMethod(int paymentMethodId) throws SQLException {
        String sql = "DELETE FROM payment_methods WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, paymentMethodId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean hasActivePaymentMethods(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payment_methods WHERE user_id = ? AND is_active = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private PaymentMethod mapResultSetToPaymentMethod(ResultSet rs) throws SQLException {
        PaymentMethod method = new PaymentMethod();
        method.setId(rs.getInt("id"));
        method.setUserId(rs.getInt("user_id"));
        method.setCardType(rs.getString("card_type"));
        method.setCardNumberEncrypted(rs.getString("card_number_encrypted"));
        method.setCardHolderName(rs.getString("card_holder_name"));
        method.setExpiryMonth(rs.getInt("expiry_month"));
        method.setExpiryYear(rs.getInt("expiry_year"));
        method.setCvvEncrypted(rs.getString("cvv_encrypted"));
        method.setDefault(rs.getBoolean("is_default"));
        method.setActive(rs.getBoolean("is_active"));
        method.setBankName(rs.getString("bank_name"));
        method.setAccountNumberEncrypted(rs.getString("account_number_encrypted"));
        method.setRoutingNumber(rs.getString("routing_number"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            method.setCreatedAt(createdAt.toLocalDateTime());
        }

        return method;
    }

    public PaymentMethod encryptPaymentMethodDetails(PaymentMethod method, String cardNumber, String cvv) {
        method.setCardNumberEncrypted(encryptionService.encrypt(cardNumber));
        method.setCvvEncrypted(encryptionService.encrypt(cvv));
        return method;
    }

    public String decryptCardNumber(String encryptedCardNumber) {
        return encryptionService.decrypt(encryptedCardNumber);
    }

    public String decryptCVV(String encryptedCVV) {
        return encryptionService.decrypt(encryptedCVV);
    }

    public boolean validateCardExpiry(int month, int year) {
        if (month < 1 || month > 12) return false;

        int currentYear = java.time.Year.now().getValue();
        int currentMonth = java.time.LocalDate.now().getMonthValue();

        if (year < currentYear) return false;
        if (year == currentYear && month < currentMonth) return false;

        return true;
    }

    public double getWalletBalance(int id)  {

//        System.out.println("[PaymentMethodDAO] id: "+id);
        String sql = "SELECT wallet_balance FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("wallet_balance"); // or rs.getDouble(1)
            } else {
                throw new RuntimeException("No user found with id: " + id);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}