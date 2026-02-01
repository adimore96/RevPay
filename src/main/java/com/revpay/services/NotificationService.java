package com.revpay.services;

import com.revpay.dao.NotificationDAO;
import com.revpay.models.Notification;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class NotificationService {
    private NotificationDAO notificationDAO;
    private Scanner scanner;
    private boolean isAllNotifications = false;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
        this.scanner = new Scanner(System.in);
    }

    public void viewNotifications(int userId) {
        try {
            List<Notification> notifications;
            if(isAllNotifications){
                notifications  = notificationDAO.getAllNotificationsByUserId(userId);
            }else{

                notifications  = notificationDAO.getNotificationsByUserId(userId);
            }

            if (notifications.isEmpty()) {
                System.out.println("No notifications found.");
                return;
            }

            System.out.println("\n--- Notifications ---");
            System.out.println("ID       |Date                  | Type       | Title                    | Read");
            System.out.println("-------------------------------------------------------------------------------");

            int unreadCount = 0;
            int id = 1;
            for (Notification notification : notifications) {

                String date = notification.getCreatedAt().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String type = notification.getType();
                String title = notification.getTitle().length() > 25 ?
                        notification.getTitle().substring(0, 25) + "..." : notification.getTitle();
                String read = notification.isRead() ? "Yes" : "No";

                if (!notification.isRead()) {
                    unreadCount++;
                }

                System.out.printf("%-10s |%-20s | %-10s | %-25s | %s%n",
                        id++, date, type, title, read);
            }

            System.out.println("\nTotal: " + notifications.size() + " notifications");
            System.out.println("Unread: " + unreadCount);

            // Show options
            System.out.println("\nOptions:");
            System.out.println("1. View notification details");
            System.out.println("2. Mark all as read");
            System.out.println("3. Delete old notifications");
            System.out.println("4. View all notifications");
            System.out.println("5. Back to main menu");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    viewNotificationDetails(notifications);
                    break;
                case 2:
                    markAllAsRead(userId);
                    break;
                case 3:
                    deleteOldNotifications();
                    break;
                case 4:
                    isAllNotifications = true;
                    viewNotifications(userId);
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid option.");
            }

        } catch (SQLException e) {
            System.out.println("Error viewing notifications: " + e.getMessage());
        }
    }

    private void viewNotificationDetails(List<Notification> notifications) {
        System.out.print("Enter notification number to view: ");
        int index = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (index < 1 || index > notifications.size()) {
            System.out.println("Invalid notification number.");
            return;
        }

        Notification notification = notifications.get(index - 1);

        System.out.println("\n=== Notification Details ===");
        System.out.println("Title: " + notification.getTitle());
        System.out.println("Message: " + notification.getMessage());
        System.out.println("Type: " + notification.getType());
        System.out.println("Date: " + notification.getCreatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Read: " + (notification.isRead() ? "Yes" : "No"));

        if (!notification.isRead()) {
            try {
                notificationDAO.markNotificationAsRead(notification.getId());
                System.out.println("Notification marked as read.");
            } catch (SQLException e) {
                System.out.println("Error marking notification as read: " + e.getMessage());
            }
        }
    }

    private void markAllAsRead(int userId) {
        try {
            boolean success = notificationDAO.markAllNotificationsAsRead(userId);
            if (success) {
                System.out.println("All notifications marked as read.");
            } else {
                System.out.println("No notifications to mark as read.");
            }
        } catch (SQLException e) {
            System.out.println("Error marking notifications as read: " + e.getMessage());
        }
    }

    private void deleteOldNotifications() {
        System.out.print("Delete notifications older than how many days? (default 30): ");
        String daysStr = scanner.nextLine();

        int days = 30;
        if (!daysStr.isEmpty()) {
            try {
                days = Integer.parseInt(daysStr);
                if (days < 1) {
                    System.out.println("Days must be at least 1.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Using default 30 days.");
            }
        }

        try {
            notificationDAO.deleteOldNotifications(days);
            System.out.println("Old notifications deleted.");
        } catch (SQLException e) {
            System.out.println("Error deleting old notifications: " + e.getMessage());
        }
    }

    public int getUnreadNotificationsCount(int userId) {
        try {
            return notificationDAO.getUnreadNotificationCount(userId);
        } catch (SQLException e) {
            System.out.println("Error getting unread notification count: " + e.getMessage());
            return 0;
        }
    }

    public void viewNotificationsByType(int userId, String type) {
        try {
            List<Notification> notifications = notificationDAO.getNotificationsByType(userId, type);

            if (notifications.isEmpty()) {
                System.out.println("No " + type.toLowerCase() + " notifications found.");
                return;
            }

            System.out.println("\n--- " + type + " Notifications ---");
            for (Notification notification : notifications) {
                System.out.println("Title: " + notification.getTitle());
                System.out.println("Message: " + notification.getMessage());
                System.out.println("Date: " + notification.getCreatedAt().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                System.out.println("Read: " + (notification.isRead() ? "Yes" : "No"));
                System.out.println("------------------------");
            }

        } catch (SQLException e) {
            System.out.println("Error viewing notifications by type: " + e.getMessage());
        }
    }

    public void createLowBalanceAlert(int userId, double currentBalance, double threshold) {
        try {
            String title = "Low Balance Alert";
            String message = String.format(
                    "Your wallet balance ($%.2f) is below the threshold ($%.2f). " +
                            "Please add funds to avoid service interruptions.",
                    currentBalance, threshold);

            notificationDAO.createAlertNotification(userId, title, message);
        } catch (SQLException e) {
            System.out.println("Error creating low balance alert: " + e.getMessage());
        }
    }

    public void close() {
        scanner.close();
    }
}