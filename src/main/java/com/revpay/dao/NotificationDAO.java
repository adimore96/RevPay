package com.revpay.dao;

import com.revpay.models.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {
    private Connection connection;

    public NotificationDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public Notification createNotification(Notification notification) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, type, title, message, " +
                "is_read, related_id, related_type, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, notification.getUserId());
            stmt.setString(2, notification.getType());
            stmt.setString(3, notification.getTitle());
            stmt.setString(4, notification.getMessage());
            stmt.setBoolean(5, notification.isRead());

            if (notification.getRelatedId() != null) {
                stmt.setInt(6, notification.getRelatedId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            stmt.setString(7, notification.getRelatedType());
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating notification failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getInt(1));
                }
            }
        }
        return notification;
    }

    public List<Notification> getNotificationsByUserId(int userId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC limit 10";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        }
        return notifications;
    }

//    View all notifications
    public List<Notification> getAllNotificationsByUserId(int userId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        }
        return notifications;
    }

    public List<Notification> getUnreadNotificationsByUserId(int userId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE " +
                "ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        }
        return notifications;
    }

    public int getUnreadNotificationCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";

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

    public boolean markNotificationAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean markAllNotificationsAsRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteNotification(int notificationId) throws SQLException {
        String sql = "DELETE FROM notifications WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);

            return stmt.executeUpdate() > 0;
        }
    }

    public void deleteOldNotifications(int days) throws SQLException {
        String sql = "DELETE FROM notifications WHERE created_at < ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            stmt.setTimestamp(1, Timestamp.valueOf(cutoffDate));

            stmt.executeUpdate();
        }
    }

    public List<Notification> getNotificationsByType(int userId, String type) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND type = ? " +
                "ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, type);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        }
        return notifications;
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setRead(rs.getBoolean("is_read"));

        int relatedId = rs.getInt("related_id");
        if (!rs.wasNull()) {
            notification.setRelatedId(relatedId);
        }

        notification.setRelatedType(rs.getString("related_type"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }

        return notification;
    }

    public void createTransactionNotification(int userId, String transactionId,
                                              double amount, String type) throws SQLException {
        String title = "Transaction " + type;
        String message = String.format("Your transaction %s of $%.2f has been %s",
                transactionId, amount, type.toLowerCase());

        Notification notification = new Notification(userId, "TRANSACTION", title, message);
        createNotification(notification);
    }

    public void createMoneyRequestNotification(int userId, String requestId,
                                               double amount, boolean isIncoming) throws SQLException {
        String title = isIncoming ? "Money Request Received" : "Money Request Sent";
        String message = String.format("Money request %s for $%.2f has been %s",
                requestId, amount, isIncoming ? "received" : "sent");

        Notification notification = new Notification(userId, "REQUEST", title, message);
        createNotification(notification);
    }

//    MoneyRequestToClient
    public void createMoneyRequestReceiverNotification(int userId, String name, String requestId,
                                               double amount, boolean isIncoming) throws SQLException {
        String title = isIncoming ? "Money Request Received" : "Money Request Sent";
        String message = String.format("Money request %s for $%.2f has been %s from %s",
                requestId, amount, isIncoming ? "received" : "sent", name);

        Notification notification = new Notification(userId, "REQUEST", title, message);
        createNotification(notification);
    }

    public void createInvoiceNotification(int userId, String invoiceNumber,
                                          double amount, String status) throws SQLException {
        String title = "Invoice " + status;
        String message = String.format("Invoice %s for $%.2f has been %s",
                invoiceNumber, amount, status.toLowerCase());

        Notification notification = new Notification(userId, "INVOICE", title, message);
        createNotification(notification);
    }

    public void createLoanNotification(int userId, String applicationId,
                                       String status) throws SQLException {
        String title = "Loan Application Update";
        String message = String.format("Your loan application %s has been %s",
                applicationId, status.toLowerCase());

        Notification notification = new Notification(userId, "LOAN", title, message);
        createNotification(notification);
    }

    public void createAlertNotification(int userId, String title, String message) throws SQLException {
        Notification notification = new Notification(userId, "ALERT", title, message);
        createNotification(notification);
    }
}