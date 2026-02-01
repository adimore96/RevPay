package com.revpay.dao;

import com.revpay.models.MoneyRequest;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MoneyRequestDAO {
    private Connection connection;

    public MoneyRequestDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public MoneyRequest createMoneyRequest(MoneyRequest request) throws SQLException {
        String sql = "INSERT INTO money_requests (request_id, requester_id, recipient_id, " +
                "amount, status, description, expires_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, request.getRequestId());
            stmt.setInt(2, request.getRequesterId());
            stmt.setInt(3, request.getRecipientId());
            stmt.setDouble(4, request.getAmount());
            stmt.setString(5, request.getStatus());
            stmt.setString(6, request.getDescription());

            if (request.getExpiresAt() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(request.getExpiresAt()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }

            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating money request failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    request.setId(generatedKeys.getInt(1));
                }
            }
        }
        return request;
    }

    public List<MoneyRequest> getMoneyRequestsByRequesterId(int requesterId) throws SQLException {
        List<MoneyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM money_requests WHERE requester_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, requesterId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToMoneyRequest(rs));
                }
            }
        }
        return requests;
    }

    public List<MoneyRequest> getMoneyRequestsByRecipientId(int recipientId) throws SQLException {
        List<MoneyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM money_requests WHERE recipient_id = ? AND status = 'PENDING' " +
                "ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, recipientId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToMoneyRequest(rs));
                }
            }
        }
        return requests;
    }

    public MoneyRequest getMoneyRequestById(String requestId) throws SQLException {
        String sql = "SELECT * FROM money_requests WHERE request_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, requestId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMoneyRequest(rs);
                }
            }
        }
        return null;
    }

    public boolean updateMoneyRequestStatus(String requestId, String status) throws SQLException {
        String sql = "UPDATE money_requests SET status = ?, updated_at = ? WHERE request_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, requestId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean cancelMoneyRequest(String requestId, int requesterId) throws SQLException {
        String sql = "UPDATE money_requests SET status = 'CANCELLED', updated_at = ? " +
                "WHERE request_id = ? AND requester_id = ? AND status = 'PENDING'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, requestId);
            stmt.setInt(3, requesterId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean declineMoneyRequest(String requestId, int recipientId) throws SQLException {
        String sql = "UPDATE money_requests SET status = 'DECLINED', updated_at = ? " +
                "WHERE request_id = ? AND recipient_id = ? AND status = 'PENDING'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, requestId);
            stmt.setInt(3, recipientId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean acceptMoneyRequest(String requestId, int recipientId) throws SQLException {
        String sql = "UPDATE money_requests SET status = 'ACCEPTED', updated_at = ? " +
                "WHERE request_id = ? AND recipient_id = ? AND status = 'PENDING'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, requestId);
            stmt.setInt(3, recipientId);

            return stmt.executeUpdate() > 0;
        }
    }

    public void expireOldRequests() throws SQLException {
        String sql = "UPDATE money_requests SET status = 'EXPIRED', updated_at = ? " +
                "WHERE status = 'PENDING' AND expires_at < ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();
        }
    }

    public int getPendingRequestCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM money_requests " +
                "WHERE recipient_id = ? AND status = 'PENDING'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private MoneyRequest mapResultSetToMoneyRequest(ResultSet rs) throws SQLException {
        MoneyRequest request = new MoneyRequest();
        request.setId(rs.getInt("id"));
        request.setRequestId(rs.getString("request_id"));
        request.setRequesterId(rs.getInt("requester_id"));
        request.setRecipientId(rs.getInt("recipient_id"));
        request.setAmount(rs.getDouble("amount"));
        request.setStatus(rs.getString("status"));
        request.setDescription(rs.getString("description"));

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            request.setExpiresAt(expiresAt.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            request.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            request.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return request;
    }

    public String generateRequestId() {
        return "REQ" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    public LocalDateTime getDefaultExpiryDate() {
        return LocalDateTime.now().plusDays(7);
    }
}