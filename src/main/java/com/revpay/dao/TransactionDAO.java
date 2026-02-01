package com.revpay.dao;

import com.revpay.models.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private Connection connection;

    public TransactionDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public Transaction createTransaction(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (transaction_id, sender_id, receiver_id, " +
                "amount, transaction_type, status, description, payment_method_id, " +
                "transaction_fee, is_recurring, recurring_frequency, invoice_id, " +
                "created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, transaction.getTransactionId());
            stmt.setInt(2, transaction.getSenderId());
            stmt.setInt(3, transaction.getReceiverId());
            stmt.setDouble(4, transaction.getAmount());
            stmt.setString(5, transaction.getTransactionType());
            stmt.setString(6, transaction.getStatus());
            stmt.setString(7, transaction.getDescription());

            if (transaction.getPaymentMethodId() != null) {
                stmt.setInt(8, transaction.getPaymentMethodId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.setDouble(9, transaction.getTransactionFee());
            stmt.setBoolean(10, transaction.isRecurring());
            stmt.setString(11, transaction.getRecurringFrequency());

            if (transaction.getInvoiceId() != null) {
                stmt.setInt(12, transaction.getInvoiceId());
            } else {
                stmt.setNull(12, Types.INTEGER);
            }

            stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getInt(1));
                }
            }
        }
        return transaction;
    }

    public List<Transaction> getTransactionsByUserId(int userId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE sender_id = ? OR receiver_id = ? " +
                "ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    public List<Transaction> getTransactionsByUserIdAndType(int userId, String type) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE (sender_id = ? OR receiver_id = ?) " +
                "AND transaction_type = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setString(3, type);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    public List<Transaction> getTransactionsByDateRange(int userId, Date startDate, Date endDate) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE (sender_id = ? OR receiver_id = ?) " +
                "AND DATE(created_at) BETWEEN ? AND ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setDate(3, startDate);
            stmt.setDate(4, endDate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    public Transaction getTransactionById(String transactionId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transactionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
            }
        }
        return null;
    }

    public boolean updateTransactionStatus(String transactionId, String status) throws SQLException {
        String sql = "UPDATE transactions SET status = ? WHERE transaction_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, transactionId);

            return stmt.executeUpdate() > 0;
        }
    }

    public double getTotalSentAmount(int userId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                "WHERE sender_id = ? AND status = 'COMPLETED' " +
                "AND created_at BETWEEN ? AND ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            stmt.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double getTotalReceivedAmount(int userId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                "WHERE receiver_id = ? AND status = 'COMPLETED' " +
                "AND created_at BETWEEN ? AND ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            stmt.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public int getTransactionCount(int userId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions " +
                "WHERE (sender_id = ? OR receiver_id = ?) AND status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setString(3, status);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Transaction> searchTransactions(int userId, String searchTerm) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT t.* FROM transactions t " +
                "JOIN users u1 ON t.sender_id = u1.id " +
                "JOIN users u2 ON t.receiver_id = u2.id " +
                "WHERE (t.sender_id = ? OR t.receiver_id = ?) " +
                "AND (t.transaction_id LIKE ? OR t.description LIKE ? " +
                "OR u1.username LIKE ? OR u2.username LIKE ?) " +
                "ORDER BY t.created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            String searchPattern = "%" + searchTerm + "%";
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            stmt.setString(5, searchPattern);
            stmt.setString(6, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getInt("id"));
        transaction.setTransactionId(rs.getString("transaction_id"));
        transaction.setSenderId(rs.getInt("sender_id"));
        transaction.setReceiverId(rs.getInt("receiver_id"));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setTransactionType(rs.getString("transaction_type"));
        transaction.setStatus(rs.getString("status"));
        transaction.setDescription(rs.getString("description"));

        int paymentMethodId = rs.getInt("payment_method_id");
        if (!rs.wasNull()) {
            transaction.setPaymentMethodId(paymentMethodId);
        }

        transaction.setTransactionFee(rs.getDouble("transaction_fee"));
        transaction.setRecurring(rs.getBoolean("is_recurring"));
        transaction.setRecurringFrequency(rs.getString("recurring_frequency"));

        int invoiceId = rs.getInt("invoice_id");
        if (!rs.wasNull()) {
            transaction.setInvoiceId(invoiceId);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            transaction.setCreatedAt(createdAt.toLocalDateTime());
        }

        return transaction;
    }

    public String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}