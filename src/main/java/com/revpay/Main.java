package com.revpay;

import com.revpay.dao.DatabaseConnection;
import com.revpay.models.User;
import com.revpay.services.AuthService;
import com.revpay.services.PaymentService;
import com.revpay.services.NotificationService;
import com.revpay.utils.LoggerUtil;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static AuthService authService = new AuthService();
    private static PaymentService paymentService = new PaymentService();
    private static NotificationService notificationService = new NotificationService();
    private static User currentUser = null;
//    for logging
    private static final Logger logger = LoggerUtil.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // Initialize database connection
            DatabaseConnection.initialize();

            logger.info("Starting RevPay application...");

            System.out.println("\n=========================================");
            System.out.println("      WELCOME TO REVPAY FINANCIAL");
            System.out.println("=========================================\n");

            boolean running = true;
            while (running) {
                if (currentUser == null) {
                    showWelcomeMenu();
                } else {
                    showMainMenu();
                }
            }

        } catch (Exception e) {
            logger.error("Application failed to start", e);
        } finally {
            DatabaseConnection.closeConnection();
            scanner.close();
        }
    }

    private static void showWelcomeMenu() {
        System.out.println("\n--- Welcome Menu ---");
        System.out.println("1. Login");
        System.out.println("2. Register Personal Account");
        System.out.println("3. Register Business Account");
        System.out.println("4. Forgot Password");
        System.out.println("5. Exit");
        System.out.print("Choose an option: ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        switch (choice) {
            case 1:
                login();
                break;
            case 2:
                registerPersonalAccount();
                break;
            case 3:
                registerBusinessAccount();
                break;
            case 4:
                forgotPassword();
                break;
            case 5:
                System.out.println("Thank you for using RevPay. Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    private static void showMainMenu() {
        if (currentUser == null) return;

        System.out.println("\n=========================================");
        System.out.println("   Welcome, " + currentUser.getFullName());
        System.out.println("   Balance: $" + paymentService.getWalletBalance(currentUser.getId()));
        System.out.println("=========================================\n");

        System.out.println("--- Main Menu ---");
        System.out.println("1. Send Money");
        System.out.println("2. Request Money");
        System.out.println("3. View Transaction History");
        System.out.println("4. Manage Payment Methods");
        System.out.println("5. Manage Wallet Balance");
        System.out.println("6. View Notifications");

        if (currentUser.getAccountType().equals("BUSINESS")) {
            System.out.println("7. Business Features");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    sendMoney();
                    break;
                case 2:
                    requestMoney();
                    break;
                case 3:
                    viewTransactions();
                    break;
                case 4:
                    managePaymentMethods();
                    break;
                case 5:
                    manageWallet();
                    break;
                case 6:
                    viewNotifications();
                    break;
                case 7:
                    showBusinessMenu();
                    break;
                case 8:
                    logout();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        } else {
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    sendMoney();
                    break;
                case 2:
                    requestMoney();
                    break;
                case 3:
                    viewTransactions();
                    break;
                case 4:
                    managePaymentMethods();
                    break;
                case 5:
                    manageWallet();
                    break;
                case 6:
                    viewNotifications();
                    break;
                case 7:
                    logout();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void showBusinessMenu() {
        System.out.println("\n--- Business Menu ---");
        System.out.println("1. Create Invoice");
        System.out.println("2. View Invoices");
        System.out.println("3. Apply for Loan");
        System.out.println("4. View Loan Applications");
        System.out.println("5. Business Analytics");
        System.out.println("6. Accept Payments");
        System.out.println("7. Back to Main Menu");
        System.out.print("Choose an option: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                createInvoice();
                break;
            case 2:
                viewInvoices();
                break;
            case 3:
                applyForLoan();
                break;
            case 4:
                viewLoanApplications();
                break;
            case 5:
                viewBusinessAnalytics();
                break;
            case 6:
                acceptPayments();
                break;
            case 7:
                return;
            default:
                System.out.println("Invalid option.");
        }
    }

    // Authentication Methods
    private static void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Enter email/phone: ");
        String identifier = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            currentUser = authService.login(identifier, password);
            if (currentUser != null) {
                System.out.println("Login successful!");
                // Show unread notifications count
                int unreadCount = notificationService.getUnreadNotificationsCount(currentUser.getId());
                if (unreadCount > 0) {
                    System.out.println("You have " + unreadCount + " unread notifications.");
                }
            } else {
                System.out.println("Login failed. Please check your credentials.");
            }
        } catch (Exception e) {
            System.out.println("Error during login: " + e.getMessage());
        }
    }

    private static void registerPersonalAccount() {
        System.out.println("\n--- Register Personal Account ---");
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Phone Number: ");
        String phone = scanner.nextLine();
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password (min 8 chars): ");
        String password = scanner.nextLine();
        System.out.print("Security Question 1: ");
        String q1 = scanner.nextLine();
        System.out.print("Answer 1: ");
        String a1 = scanner.nextLine();
        System.out.print("Security Question 2: ");
        String q2 = scanner.nextLine();
        System.out.print("Answer 2: ");
        String a2 = scanner.nextLine();

        try {
            boolean success = authService.registerPersonalUser(
                    fullName, email, phone, username, password, q1, a1, q2, a2);
            if (success) {
                System.out.println("Registration successful! Please login.");
            } else {
                System.out.println("Registration failed.");
            }
        } catch (Exception e) {
            System.out.println("Error during registration: " + e.getMessage());
        }
    }

    private static void registerBusinessAccount() {
        System.out.println("\n--- Register Business Account ---");
        System.out.print("Business Name: ");
        String businessName = scanner.nextLine();
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Phone Number: ");
        String phone = scanner.nextLine();
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Business Type: ");
        String businessType = scanner.nextLine();
        System.out.print("Tax ID: ");
        String taxId = scanner.nextLine();
        System.out.print("Business Address: ");
        String address = scanner.nextLine();

        try {
            boolean success = authService.registerBusinessUser(
                    fullName, email, phone, username, password,
                    businessName, businessType, taxId, address);
            if (success) {
                System.out.println("Business registration successful! Please login.");
            } else {
                System.out.println("Registration failed.");
            }
        } catch (Exception e) {
            System.out.println("Error during registration: " + e.getMessage());
        }
    }

    private static void forgotPassword() {
        System.out.println("\n--- Forgot Password ---");
        System.out.print("Enter email/phone: ");
        String identifier = scanner.nextLine();

        try {
            boolean success = authService.initiatePasswordReset(identifier);
            if (success) {
                System.out.println("Password reset instructions sent to your email/phone.");
            } else {
                System.out.println("Account not found.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void logout() {
        authService.logout(currentUser);
        currentUser = null;
        System.out.println("Logged out successfully.");
    }

    // Transaction Methods
    private static void sendMoney() {
        System.out.println("\n--- Send Money ---");
        System.out.print("Enter recipient (username/email/phone): ");
        String recipientIdentifier = scanner.nextLine();
        System.out.print("Amount: $");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline
        System.out.print("Note (optional): ");
        String note = scanner.nextLine();

        try {
            boolean success = paymentService.sendMoney(
                    currentUser, recipientIdentifier, amount, note);
            if (success) {
                System.out.println("Money sent successfully!");
            } else {
                System.out.println("Failed to send money.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void requestMoney() {
        System.out.println("\n--- Request Money ---");
        System.out.print("Enter requester (username/email/phone): ");
        String requesterIdentifier = scanner.nextLine();
        System.out.print("Amount: $");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline
        System.out.print("Note (optional): ");
        String note = scanner.nextLine();

        try {
            boolean success = paymentService.requestMoney(
                    currentUser, requesterIdentifier, amount, note);
            if (success) {
                System.out.println("Money request sent!");
            } else {
                System.out.println("Failed to send request.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewTransactions() {
        System.out.println("\n--- Transaction History ---");
        try {
            paymentService.viewTransactionHistory(currentUser.getId());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void managePaymentMethods() {
        System.out.println("\n--- Payment Methods ---");
        System.out.println("1. Add Card");
        System.out.println("2. View Cards");
        System.out.println("3. Set Default Card");
        System.out.println("4. Remove Card");
        System.out.println("5. Set/Update Transaction Pin");
        System.out.print("Choose an option: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        try {
            switch (choice) {
                case 1:
                    addPaymentMethod();
                    break;
                case 2:
                    viewPaymentMethods();
                    break;
                case 3:
                    setDefaultPaymentMethod();
                    break;
                case 4:
                    removePaymentMethod();
                    break;
                case 5:
                    setTransactionPin();
                default:
                    System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addPaymentMethod() {
        System.out.println("\n--- Add Payment Method ---");
        System.out.print("Card Number: ");
        String cardNumber = scanner.nextLine();
        System.out.print("Card Holder Name: ");
        String holderName = scanner.nextLine();
        System.out.print("Expiry Month (MM): ");
        int expiryMonth = scanner.nextInt();
        System.out.print("Expiry Year (YYYY): ");
        int expiryYear = scanner.nextInt();
        scanner.nextLine();
        System.out.print("CVV: ");
        String cvv = scanner.nextLine();
//        scanner.nextLine(); // Consume newline
        System.out.print("Card Type (CREDIT/DEBIT): ");
        String cardType = scanner.nextLine();

        try {
            boolean success = paymentService.addPaymentMethod(
                    currentUser.getId(), cardNumber, holderName,
                    expiryMonth, expiryYear, cvv, cardType);
            if (success) {
                System.out.println("Payment method added!");
            } else {
                System.out.println("Failed to add payment method.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewPaymentMethods() {
        try {
            paymentService.viewPaymentMethods(currentUser.getId());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void setDefaultPaymentMethod() {
        System.out.print("Enter payment method ID to set as default: ");
        int paymentMethodId = scanner.nextInt();
        scanner.nextLine();

        try {
            boolean success = paymentService.setDefaultPaymentMethod(
                    currentUser.getId(), paymentMethodId);
            if (success) {
                System.out.println("Default payment method updated!");
            } else {
                System.out.println("Failed to update default payment method.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void removePaymentMethod() {
        System.out.print("Enter payment method ID to remove: ");
        int paymentMethodId = scanner.nextInt();
        scanner.nextLine();

        try {
            boolean success = paymentService.removePaymentMethod(
                    currentUser.getId(), paymentMethodId);
            if (success) {
                System.out.println("Payment method removed!");
            } else {
                System.out.println("Failed to remove payment method.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void manageWallet() {
        System.out.println("\n--- Wallet Management ---");
        System.out.println("1. Add Money from Card");
        System.out.println("2. Withdraw to Bank Account");
        System.out.println("3. View Balance");
        System.out.print("Choose an option: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        try {
            switch (choice) {
                case 1:
                    addMoneyToWallet();
                    break;
                case 2:
                    withdrawFromWallet();
                    break;
                case 3:
                    viewWalletBalance();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addMoneyToWallet() {
        System.out.print("Amount to add: $");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        System.out.print("Payment Method ID: ");
        int paymentMethodId = scanner.nextInt();
        scanner.nextLine();

        try {
            boolean success = paymentService.addMoneyToWallet(
                    currentUser.getId(), amount, paymentMethodId);
            if (success) {
                System.out.println("Money added to wallet!");
            } else {
                System.out.println("Failed to add money.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void withdrawFromWallet() {
        System.out.print("Amount to withdraw: $");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        try {
            boolean success = paymentService.withdrawFromWallet(
                    currentUser.getId(), amount);
            if (success) {
                System.out.println("Withdrawal initiated!");
            } else {
                System.out.println("Failed to withdraw.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewWalletBalance() {
        System.out.println("\n--- Wallet Balance ---");
        System.out.println("Current Balance: $" + currentUser.getWalletBalance());
    }

    private static void viewNotifications() {
        System.out.println("\n--- Notifications ---");
        try {
            notificationService.viewNotifications(currentUser.getId());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Business Methods
    private static void createInvoice() {
        System.out.println("\n--- Create Invoice ---");
        System.out.print("Customer Email: ");
        String customerEmail = scanner.nextLine();
        System.out.print("Customer Name: ");
        String customerName = scanner.nextLine();
        System.out.print("Amount: $");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline
        System.out.print("Description: ");
        String description = scanner.nextLine();
        System.out.print("Due Date (YYYY-MM-DD): ");
        String dueDateStr = scanner.nextLine();

        try {
            boolean success = paymentService.createInvoice(
                    currentUser.getId(), customerEmail, customerName,
                    amount, description, dueDateStr);
            if (success) {
                System.out.println("Invoice created successfully!");
            } else {
                System.out.println("Failed to create invoice.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewInvoices() {
        System.out.println("\n--- Invoices ---");
        try {
            paymentService.viewInvoices(currentUser.getId());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void applyForLoan() {
        System.out.println("\n--- Apply for Loan ---");
        System.out.print("Loan Amount: $");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline
        System.out.print("Purpose: ");
        String purpose = scanner.nextLine();
        System.out.print("Term (months): ");
        int term = scanner.nextInt();
        scanner.nextLine();

        try {
            boolean success = paymentService.applyForLoan(
                    currentUser.getId(), amount, purpose, term);
            if (success) {
                System.out.println("Loan application submitted!");
            } else {
                System.out.println("Failed to submit loan application.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewLoanApplications() {
        System.out.println("\n--- Loan Applications ---");
        try {
            paymentService.viewLoanApplications(currentUser.getId());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewBusinessAnalytics() {
        System.out.println("\n--- Business Analytics ---");
        try {
            paymentService.viewBusinessAnalytics(currentUser.getId());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void acceptPayments() {
        System.out.println("\n--- Accept Payments ---");
        System.out.print("Enter customer identifier: ");
        String customerIdentifier = scanner.nextLine();
        System.out.print("Amount: $");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        try {
            boolean success = paymentService.acceptPayment(
                    currentUser.getId(), customerIdentifier, amount);
            if (success) {
                System.out.println("Payment accepted!");
            } else {
                System.out.println("Failed to accept payment.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

//    Set/Update Transaction Pin
    public static void setTransactionPin(){
        System.out.print("Enter 6 digit Transaction Pin: ");
        String pin = scanner.nextLine();

        authService.setTransactionPin(currentUser,pin);
    }
}