package com.revpay.services;

import com.revpay.dao.UserDAO;
import com.revpay.dao.NotificationDAO;
import com.revpay.models.User;
import com.revpay.models.PersonalUser;
import com.revpay.models.BusinessUser;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Scanner;

import com.revpay.utils.LoggerUtil;
import org.apache.logging.log4j.Logger;

public class AuthService {
    private UserDAO userDAO;
    private NotificationDAO notificationDAO;
    private Properties properties;
    private Scanner scanner;
    private Logger logger = LoggerUtil.getLogger(AuthService.class);

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.notificationDAO = new NotificationDAO();
        this.properties = userDAO.getProperties();
        this.scanner = new Scanner(System.in);
        logger.info("AuthService initialized");
    }

    public User login(String identifier, String password) {
        logger.info("Login attempt for identifier: {}", identifier);

        try {
            User user = userDAO.getUserByEmailOrPhone(identifier);

            if (user == null) {
                logger.warn("Login failed - User not found: {}", identifier);
                return null;
            }

            if (user.isLocked()) {
                System.out.println("Account is locked. Please contact support.");
                return null;
            }

            if (userDAO.verifyPassword(password, user.getPasswordHash())) {
                // Successful login
                userDAO.resetFailedAttempts(user.getId());
                userDAO.updateLastLogin(user.getId());

                // Update user object
                user.setFailedLoginAttempts(0);
                user.setLocked(false);
                user.setLastLogin(LocalDateTime.now());

                logger.info("Login successful for user: {} (ID: {})",
                        user.getUsername(), user.getId());

                // Log audit trail
                LoggerUtil.logAudit(String.valueOf(user.getId()), "LOGIN",
                        null, "SUCCESS", "Authentication successful");

                // Create login notification
                notificationDAO.createAlertNotification(
                        user.getId(),
                        "Successful Login",
                        "You have successfully logged into your account."
                );

                return user;
            } else {
                // Failed login
                userDAO.incrementFailedAttempts(user.getId());
                int attempts = user.getFailedLoginAttempts() + 1;

                logger.warn("Login failed - Invalid password for user: {} (Attempt: {}/{})",
                        user.getUsername(), attempts, MAX_LOGIN_ATTEMPTS);

                if (attempts >= MAX_LOGIN_ATTEMPTS) {
                    userDAO.lockUserAccount(user.getId());
                    logger.error("Account locked due to too many failed attempts: {}", identifier);

                    // Log audit trail
                    LoggerUtil.logAudit(String.valueOf(user.getId()), "ACCOUNT_LOCKED",
                            null, "SECURITY", "Max failed attempts reached");
                } else {
                    System.out.println("Invalid password. Attempts remaining: " +
                            (MAX_LOGIN_ATTEMPTS - attempts));
                }
                return null;
            }
        } catch (SQLException e) {
            LoggerUtil.logError(logger, "Error during login", e,
                    "Identifier", identifier);
            return null;
        }
    }

    public boolean registerPersonalUser(String fullName, String email, String phone,
                                        String username, String password,
                                        String securityQuestion1, String securityAnswer1,
                                        String securityQuestion2, String securityAnswer2) {
        try {
            // Check if user already exists
            if (userDAO.checkIfUserExists(email) ||
                    userDAO.checkIfUserExists(phone) ||
                    userDAO.checkIfUserExists(username)) {
                System.out.println("User with this email, phone, or username already exists.");
                return false;
            }

            // Validate password strength
            if (password.length() < 8) {
                System.out.println("Password must be at least 8 characters long.");
                return false;
            }

            // Create personal user
            PersonalUser user = new PersonalUser();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhoneNumber(phone);
            user.setUsername(username);
            user.setPasswordHash(userDAO.hashPassword(password));
            user.setSecurityQuestion1(securityQuestion1);
            user.setSecurityAnswer1Hash(userDAO.hashSecurityAnswer(securityAnswer1));
            user.setSecurityQuestion2(securityQuestion2);
            user.setSecurityAnswer2Hash(userDAO.hashSecurityAnswer(securityAnswer2));
            user.setWalletBalance(0.0);
            user.setVerified(false);
            user.setLocked(false);

            // Save to database
            userDAO.createUser(user);

            // Create welcome notification
            notificationDAO.createAlertNotification(
                    user.getId(),
                    "Welcome to RevPay!",
                    "Thank you for registering with RevPay. Your personal account has been created successfully."
            );

            System.out.println("Personal account created successfully for " + fullName);
            return true;

        } catch (SQLException e) {
            System.out.println("Error during registration: " + e.getMessage());
            return false;
        }
    }

    public boolean registerBusinessUser(String fullName, String email, String phone,
                                        String username, String password,
                                        String businessName, String businessType,
                                        String taxId, String address) {
        try {
            // Check if user already exists
            if (userDAO.checkIfUserExists(email) ||
                    userDAO.checkIfUserExists(phone) ||
                    userDAO.checkIfUserExists(username)) {
                System.out.println("User with this email, phone, or username already exists.");
                return false;
            }

            // Validate password strength
            if (password.length() < 8) {
                System.out.println("Password must be at least 8 characters long.");
                return false;
            }

            // Create business user
            BusinessUser user = new BusinessUser();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhoneNumber(phone);
            user.setUsername(username);
            user.setPasswordHash(userDAO.hashPassword(password));
            user.setBusinessName(businessName);
            user.setBusinessType(businessType);
            user.setTaxId(taxId);
            user.setBusinessAddress(address);
            user.setWalletBalance(0.0);
            user.setVerified(false);
            user.setLocked(false);

            // Save to database
            userDAO.createUser(user);

            // Create welcome notification
            notificationDAO.createAlertNotification(
                    user.getId(),
                    "Welcome to RevPay Business!",
                    "Thank you for registering your business with RevPay. Your business account has been created successfully."
            );

            System.out.println("Business account created successfully for " + businessName);
            return true;

        } catch (SQLException e) {
            System.out.println("Error during registration: " + e.getMessage());
            return false;
        }
    }

    public boolean initiatePasswordReset(String identifier) {
        try {
            User user = userDAO.getUserByEmailOrPhone(identifier);

            if (user == null) {
                System.out.println("No account found with that email or phone.");
                return false;
            }

            // Ask security questions
            System.out.println("\n--- Password Reset ---");
            System.out.println("Security Question 1: " + user.getSecurityQuestion1());
            System.out.print("Answer: ");
            String answer1 = scanner.nextLine();

            System.out.println("Security Question 2: " + user.getSecurityQuestion2());
            System.out.print("Answer: ");
            String answer2 = scanner.nextLine();

            // Verify answers
            if (userDAO.verifySecurityAnswer(answer1, user.getSecurityAnswer1Hash()) &&
                    userDAO.verifySecurityAnswer(answer2, user.getSecurityAnswer2Hash())) {

                System.out.print("Enter new password: ");
                String newPassword = scanner.nextLine();

                if (newPassword.length() < 8) {
                    System.out.println("Password must be at least 8 characters long.");
                    return false;
                }

                // Update password
                String newPasswordHash = userDAO.hashPassword(newPassword);
                boolean success = userDAO.updatePassword(user.getId(), newPasswordHash);

                if (success) {
                    notificationDAO.createAlertNotification(
                            user.getId(),
                            "Password Reset",
                            "Your password has been reset successfully."
                    );
                    System.out.println("Password reset successfully.");
                    return true;
                }
            } else {
                System.out.println("Security answers incorrect.");
            }

            return false;

        } catch (SQLException e) {
            System.out.println("Error during password reset: " + e.getMessage());
            return false;
        }
    }

    public boolean changePassword(User user, String currentPassword, String newPassword) {
        try {
            // Verify current password
            if (!userDAO.verifyPassword(currentPassword, user.getPasswordHash())) {
                System.out.println("Current password is incorrect.");
                return false;
            }

            // Validate new password
            if (newPassword.length() < 8) {
                System.out.println("New password must be at least 8 characters long.");
                return false;
            }

            // Update password
            String newPasswordHash = userDAO.hashPassword(newPassword);
            boolean success = userDAO.updatePassword(user.getId(), newPasswordHash);

            if (success) {
                user.setPasswordHash(newPasswordHash);
                notificationDAO.createAlertNotification(
                        user.getId(),
                        "Password Changed",
                        "Your password has been changed successfully."
                );
                System.out.println("Password changed successfully.");
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.out.println("Error changing password: " + e.getMessage());
            return false;
        }
    }

    public boolean setTransactionPin(User user, String pin) {
        try {
            if (pin.length() != 6 || !pin.matches("\\d+")) {
                System.out.println("PIN must be 6 digits.");
                return false;
            }

            String pinHash = userDAO.hashPassword(pin);
            boolean success = userDAO.updateTransactionPin(user.getId(), pinHash);

            if (success) {
                user.setTransactionPinHash(pinHash);
                notificationDAO.createAlertNotification(
                        user.getId(),
                        "Transaction PIN Set",
                        "Your transaction PIN has been set successfully."
                );
                System.out.println("Transaction PIN set successfully.");
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.out.println("Error setting transaction PIN: " + e.getMessage());
            return false;
        }
    }

    public boolean verifyTransactionPin(User user, String pin) {
        try {
            if (user.getTransactionPinHash() == null) {
                System.out.println("Transaction PIN not set. Please set a PIN first.");
                return false;
            }

            return userDAO.verifyPassword(pin, user.getTransactionPinHash());

        } catch (Exception e) {
            System.out.println("Error verifying transaction PIN: " + e.getMessage());
            return false;
        }
    }

    public void logout(User user) {
        try {
            notificationDAO.createAlertNotification(
                    user.getId(),
                    "Logout",
                    "You have been logged out of your account."
            );
        } catch (SQLException e) {
            System.out.println("Error during logout: " + e.getMessage());
        }
    }

    public boolean updateProfile(User user) {
        try {
            System.out.println("\n--- Update Profile ---");
            System.out.print("Full Name (" + user.getFullName() + "): ");
            String fullName = scanner.nextLine();
            if (!fullName.isEmpty()) {
                user.setFullName(fullName);
            }

            System.out.print("Phone Number (" + user.getPhoneNumber() + "): ");
            String phone = scanner.nextLine();
            if (!phone.isEmpty()) {
                user.setPhoneNumber(phone);
            }

            if (user.getAccountType().equals("BUSINESS")) {
                System.out.print("Business Address (" + user.getBusinessAddress() + "): ");
                String address = scanner.nextLine();
                if (!address.isEmpty()) {
                    user.setBusinessAddress(address);
                }
            }

            boolean success = userDAO.updateUser(user);

            if (success) {
                notificationDAO.createAlertNotification(
                        user.getId(),
                        "Profile Updated",
                        "Your profile information has been updated."
                );
                System.out.println("Profile updated successfully.");
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.out.println("Error updating profile: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        scanner.close();
    }
}