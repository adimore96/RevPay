package com.revpay.services;

import com.revpay.dao.*;
import com.revpay.models.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class PaymentService {
    private UserDAO userDAO;
    private PaymentMethodDAO paymentMethodDAO;
    private TransactionDAO transactionDAO;
    private MoneyRequestDAO moneyRequestDAO;
    private InvoiceDAO invoiceDAO;
    private LoanDAO loanDAO;
    private NotificationDAO notificationDAO;
    private EncryptionService encryptionService;
    private Properties properties;
    private Scanner scanner;

    private static final double TRANSACTION_FEE_PERCENTAGE = 1.5;
    private static final double MIN_TRANSACTION_AMOUNT = 1.00;
    private static final double MAX_TRANSACTION_AMOUNT = 10000.00;

    public PaymentService() {
        this.userDAO = new UserDAO();
        this.paymentMethodDAO = new PaymentMethodDAO();
        this.transactionDAO = new TransactionDAO();
        this.moneyRequestDAO = new MoneyRequestDAO();
        this.invoiceDAO = new InvoiceDAO();
        this.loanDAO = new LoanDAO();
        this.notificationDAO = new NotificationDAO();
        this.encryptionService = new EncryptionService();
        this.properties = userDAO.getProperties();
        this.scanner = new Scanner(System.in);
    }

    public boolean sendMoney(User sender, String recipientIdentifier, double amount, String note) {
        try {
            // Validate amount
            if (amount < MIN_TRANSACTION_AMOUNT) {
                System.out.println("Minimum transaction amount is $" + MIN_TRANSACTION_AMOUNT);
                return false;
            }

            if (amount > MAX_TRANSACTION_AMOUNT) {
                System.out.println("Maximum transaction amount is $" + MAX_TRANSACTION_AMOUNT);
                return false;
            }

            // Check sender balance
            if (sender.getWalletBalance() < amount) {
                System.out.println("Insufficient balance. Current balance: $" + sender.getWalletBalance());
                return false;
            }

            // Find recipient
            User recipient = userDAO.getUserByEmailOrPhone(recipientIdentifier);
            if (recipient == null) {
                recipient = userDAO.getUserByUsername(recipientIdentifier);
            }

            if (recipient == null) {
                System.out.println("Recipient not found.");
                return false;
            }

            if (recipient.getId() == sender.getId()) {
                System.out.println("Cannot send money to yourself.");
                return false;
            }

            // Calculate transaction fee
            double fee = (amount * TRANSACTION_FEE_PERCENTAGE) / 100;
            double totalAmount = amount + fee;

            // Verify transaction PIN
            System.out.print("Enter transaction PIN: ");
            String pin = scanner.nextLine();
            if(sender.getTransactionPinHash()==null){
                System.out.println("Set transaction PIN.");
                return false;
            }
            if (!userDAO.verifyPassword(pin, sender.getTransactionPinHash())) {
                System.out.println("Invalid transaction PIN.");
                return false;
            }

            // Create transaction
            String transactionId = transactionDAO.generateTransactionId();
            Transaction transaction = new Transaction(transactionId, sender.getId(),
                    recipient.getId(), amount, "SEND");
            transaction.setDescription(note);
            transaction.setTransactionFee(fee);
            transaction.setStatus("COMPLETED");

            // Update balances
            userDAO.updateWalletBalance(sender.getId(), -totalAmount);
            userDAO.updateWalletBalance(recipient.getId(), amount);

            // Save transaction
            transactionDAO.createTransaction(transaction);

            // Update sender and recipient wallet balances in memory
            sender.setWalletBalance(sender.getWalletBalance() - totalAmount);
            User updatedRecipient = userDAO.getUserById(recipient.getId());
            if (updatedRecipient != null) {
                recipient.setWalletBalance(updatedRecipient.getWalletBalance());
            }

            // Create notifications
            notificationDAO.createTransactionNotification(sender.getId(), transactionId, amount, "sent");
            notificationDAO.createTransactionNotification(recipient.getId(), transactionId, amount, "received");

            System.out.println("Successfully sent $" + amount + " to " + recipient.getFullName());
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("Fee: $" + fee);

            return true;

        } catch (SQLException e) {
            System.out.println("Error sending money: " + e.getMessage());
            return false;
        }
    }

    public boolean requestMoney(User requester, String recipientIdentifier, double amount, String note) {
        try {
            // Validate amount
            if (amount < MIN_TRANSACTION_AMOUNT) {
                System.out.println("Minimum request amount is $" + MIN_TRANSACTION_AMOUNT);
                return false;
            }

            if (amount > MAX_TRANSACTION_AMOUNT) {
                System.out.println("Maximum request amount is $" + MAX_TRANSACTION_AMOUNT);
                return false;
            }

            // Find recipient
            User recipient = userDAO.getUserByEmailOrPhone(recipientIdentifier);
            if (recipient == null) {
                recipient = userDAO.getUserByUsername(recipientIdentifier);
            }

            if (recipient == null) {
                System.out.println("Recipient not found.");
                return false;
            }

            if (recipient.getId() == requester.getId()) {
                System.out.println("Cannot request money from yourself.");
                return false;
            }

            // Create money request
            String requestId = moneyRequestDAO.generateRequestId();
            MoneyRequest request = new MoneyRequest(requestId, requester.getId(),
                    recipient.getId(), amount);
            request.setDescription(note);
            request.setExpiresAt(moneyRequestDAO.getDefaultExpiryDate());

            // Save request
            moneyRequestDAO.createMoneyRequest(request);

            // Create notifications
            notificationDAO.createMoneyRequestNotification(requester.getId(), requestId, amount, false);
            String requesterName = requester.getFullName()+", "+requester.getUsername() +" / "+requester.getEmail()+" / "+requester.getPhoneNumber();
            notificationDAO.createMoneyRequestReceiverNotification(recipient.getId(), requesterName, requestId, amount, true);

            System.out.println("Money request sent to " + recipient.getFullName());
            System.out.println("Request ID: " + requestId);
            System.out.println("Expires: " + request.getExpiresAt().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            return true;

        } catch (SQLException e) {
            System.out.println("Error requesting money: " + e.getMessage());
            return false;
        }
    }

    public boolean addPaymentMethod(int userId, String cardNumber, String holderName,
                                    int expiryMonth, int expiryYear, String cvv, String cardType) {
        try {
            // Validate card expiry
            if (!paymentMethodDAO.validateCardExpiry(expiryMonth, expiryYear)) {
                System.out.println("Invalid expiry date. Card may be expired.");
                return false;
            }

            // Validate card number (basic Luhn check)
            if (!isValidCardNumber(cardNumber)) {
                System.out.println("Invalid card number.");
                return false;
            }

            // Create payment method
            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setUserId(userId);
            paymentMethod.setCardType(cardType);
            paymentMethod.setCardHolderName(holderName);
            paymentMethod.setExpiryMonth(expiryMonth);
            paymentMethod.setExpiryYear(expiryYear);

            System.out.println("[PaymentService] CVV: "+cvv);

            // Encrypt sensitive data
            paymentMethod = paymentMethodDAO.encryptPaymentMethodDetails(paymentMethod, cardNumber, cvv);
            paymentMethod.setCreatedAt(LocalDateTime.now());

            // If this is the first payment method, set as default
            List<PaymentMethod> existingMethods = paymentMethodDAO.getPaymentMethodsByUserId(userId);
            if (existingMethods.isEmpty()) {
                paymentMethod.setDefault(true);
            }

//            setting payment method as active by default
            paymentMethod.setActive(true);

            // Save payment method
            paymentMethodDAO.createPaymentMethod(paymentMethod);

            // Create notification
            notificationDAO.createAlertNotification(
                    userId,
                    "Payment Method Added",
                    "A new " + cardType + " card has been added to your account."
            );

            System.out.println("Payment method added successfully.");
            return true;

        } catch (SQLException e) {
            System.out.println("Error adding payment method: " + e.getMessage());
            return false;
        }
    }

    public void viewPaymentMethods(int userId) {
        try {
            List<PaymentMethod> methods = paymentMethodDAO.getPaymentMethodsByUserId(userId);

            if (methods.isEmpty()) {
                System.out.println("No payment methods found.");
                return;
            }

            System.out.println("\n--- Your Payment Methods ---");
            for (PaymentMethod method : methods) {
                String decryptedNumber = paymentMethodDAO.decryptCardNumber(method.getCardNumberEncrypted());
                String maskedNumber = "**** **** **** " + decryptedNumber.substring(decryptedNumber.length() - 4);

                System.out.println("ID: " + method.getId());
                System.out.println("Type: " + method.getCardType());
                System.out.println("Card: " + maskedNumber);
                System.out.println("Holder: " + method.getCardHolderName());
                System.out.println("Expiry: " + method.getExpiryMonth() + "/" + method.getExpiryYear());
                System.out.println("Default: " + (method.isDefault() ? "Yes" : "No"));
                System.out.println("Active: " + (method.isActive() ? "Yes" : "No"));
                System.out.println("------------------------");
            }

        } catch (SQLException e) {
            System.out.println("Error viewing payment methods: " + e.getMessage());
        }
    }

    public boolean setDefaultPaymentMethod(int userId, int paymentMethodId) {
        try {
            boolean success = paymentMethodDAO.setDefaultPaymentMethod(userId, paymentMethodId);

            if (success) {
                notificationDAO.createAlertNotification(
                        userId,
                        "Default Payment Method Updated",
                        "Your default payment method has been updated."
                );
                System.out.println("Default payment method updated.");
                return true;
            } else {
                System.out.println("Payment method not found or doesn't belong to you.");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Error setting default payment method: " + e.getMessage());
            return false;
        }
    }

    public boolean removePaymentMethod(int userId, int paymentMethodId) {
        try {
            // Check if payment method exists and belongs to user
            PaymentMethod method = paymentMethodDAO.getPaymentMethodById(paymentMethodId);
            if (method == null || method.getUserId() != userId) {
                System.out.println("Payment method not found.");
                return false;
            }

            // Don't allow removal if it's the only payment method
            List<PaymentMethod> methods = paymentMethodDAO.getPaymentMethodsByUserId(userId);
            if (methods.size() == 1) {
                System.out.println("Cannot remove your only payment method. Add another one first.");
                return false;
            }

            // If it's the default, set another one as default
            if (method.isDefault()) {
                for (PaymentMethod m : methods) {
                    if (m.getId() != paymentMethodId) {
                        paymentMethodDAO.setDefaultPaymentMethod(userId, m.getId());
                        break;
                    }
                }
            }

            // Deactivate the payment method
            boolean success = paymentMethodDAO.deactivatePaymentMethod(paymentMethodId);

            if (success) {
                notificationDAO.createAlertNotification(
                        userId,
                        "Payment Method Removed",
                        "A payment method has been removed from your account."
                );
                System.out.println("Payment method removed.");
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.out.println("Error removing payment method: " + e.getMessage());
            return false;
        }
    }

    public boolean addMoneyToWallet(int userId, double amount, int paymentMethodId) {
        try {
            // Validate amount
            if (amount < MIN_TRANSACTION_AMOUNT) {
                System.out.println("Minimum amount is $" + MIN_TRANSACTION_AMOUNT);
                return false;
            }

            // Verify payment method belongs to user
            PaymentMethod method = paymentMethodDAO.getPaymentMethodById(paymentMethodId);
            if (method == null || method.getUserId() != userId || !method.isActive()) {
                System.out.println("Invalid payment method.");
                return false;
            }

            // Verify transaction PIN
            User user = userDAO.getUserById(userId);
            System.out.print("Enter transaction PIN: ");
            String pin = scanner.nextLine();

            if (!userDAO.verifyPassword(pin, user.getTransactionPinHash())) {
                System.out.println("Invalid transaction PIN.");
                return false;
            }

            // Create transaction - FIXED: Use userId as both sender and receiver for wallet top-up
            String transactionId = transactionDAO.generateTransactionId();
            Transaction transaction = new Transaction(transactionId, userId, userId, amount, "DEPOSIT");
            transaction.setPaymentMethodId(paymentMethodId);
            transaction.setDescription("Wallet top-up from card");
            transaction.setStatus("COMPLETED");

            // Update wallet balance
            userDAO.updateWalletBalance(userId, amount);

            // Save transaction
            transactionDAO.createTransaction(transaction);

            // Update user object
            user.setWalletBalance(user.getWalletBalance() + amount);

            // Create notification
            notificationDAO.createTransactionNotification(userId, transactionId, amount, "added to wallet");

            System.out.println("Successfully added $" + amount + " to your wallet.");
            System.out.println("New balance: $" + user.getWalletBalance());

            return true;

        } catch (SQLException e) {
            System.out.println("Error adding money to wallet: " + e.getMessage());
            return false;
        }
    }

    public boolean withdrawFromWallet(int userId, double amount) {
        try {
            // Validate amount
            if (amount < MIN_TRANSACTION_AMOUNT) {
                System.out.println("Minimum withdrawal amount is $" + MIN_TRANSACTION_AMOUNT);
                return false;
            }

            // Check balance
            User user = userDAO.getUserById(userId);
            if (user.getWalletBalance() < amount) {
                System.out.println("Insufficient balance. Current balance: $" + user.getWalletBalance());
                return false;
            }

            // Verify transaction PIN
            System.out.print("Enter transaction PIN: ");
            String pin = scanner.nextLine();

            if (!userDAO.verifyPassword(pin, user.getTransactionPinHash())) {
                System.out.println("Invalid transaction PIN.");
                return false;
            }

            // Create transaction - FIXED: Use userId as both sender and receiver for withdrawal
            String transactionId = transactionDAO.generateTransactionId();
            Transaction transaction = new Transaction(transactionId, userId, userId, amount, "WITHDRAWAL");
            transaction.setDescription("Withdrawal to bank account");
            transaction.setStatus("COMPLETED"); // For simulation

            // Update balance
            userDAO.updateWalletBalance(userId, -amount);

            // Save transaction
            transactionDAO.createTransaction(transaction);

            // Update user object
            user.setWalletBalance(user.getWalletBalance() - amount);

            // Create notification
            notificationDAO.createTransactionNotification(userId, transactionId, amount, "withdrawn from wallet");

            System.out.println("Withdrawal request submitted for $" + amount);
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("New balance: $" + user.getWalletBalance());
            System.out.println("Note: Withdrawals typically take 1-3 business days to process.");

            return true;

        } catch (SQLException e) {
            System.out.println("Error withdrawing from wallet: " + e.getMessage());
            return false;
        }
    }

    public void viewTransactionHistory(int userId) {
        try {
            List<Transaction> transactions = transactionDAO.getTransactionsByUserId(userId);

            if (transactions.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }

            System.out.println("\n--- Transaction History ---");
            System.out.println("Date                  | Type       | Amount   | Status     | Description");
            System.out.println("---------------------------------------------------------------------");

            for (Transaction t : transactions) {
                String date = t.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String type = t.getTransactionType();
                String amount = String.format("$%.2f", t.getAmount());
                String status = t.getStatus();
                String description = t.getDescription() != null ?
                        (t.getDescription().length() > 20 ? t.getDescription().substring(0, 20) + "..." : t.getDescription())
                        : "";

                // Determine if amount is incoming or outgoing
                if (t.getSenderId() == userId && t.getReceiverId() != userId) {
                    amount = "-" + amount; // Outgoing
                } else if (t.getReceiverId() == userId && t.getSenderId() != userId) {
                    amount = "+" + amount; // Incoming
                }

                System.out.printf("%-20s | %-10s | %-9s | %-10s | %s%n",
                        date, type, amount, status, description);
            }

            // Show summary
            double totalSent = transactionDAO.getTotalSentAmount(userId,
                    LocalDateTime.now().minusMonths(1), LocalDateTime.now());
            double totalReceived = transactionDAO.getTotalReceivedAmount(userId,
                    LocalDateTime.now().minusMonths(1), LocalDateTime.now());

            System.out.println("\n--- Last 30 Days Summary ---");
            System.out.println("Total Sent: $" + String.format("%.2f", totalSent));
            System.out.println("Total Received: $" + String.format("%.2f", totalReceived));

        } catch (SQLException e) {
            System.out.println("Error viewing transaction history: " + e.getMessage());
        }
    }

    // Business methods
    public boolean createInvoice(int businessUserId, String customerEmail, String customerName,
                                 double amount, String description, String dueDateStr) {
        try {
            // Validate business user
            User businessUser = userDAO.getUserById(businessUserId);
            if (businessUser == null || !businessUser.getAccountType().equals("BUSINESS")) {
                System.out.println("Business account required.");
                return false;
            }

            // Validate amount
            if (amount <= 0) {
                System.out.println("Invoice amount must be greater than 0.");
                return false;
            }

            // Parse due date
            LocalDate dueDate;
            if (dueDateStr != null && !dueDateStr.isEmpty()) {
                dueDate = LocalDate.parse(dueDateStr);
                if (dueDate.isBefore(LocalDate.now())) {
                    System.out.println("Due date cannot be in the past.");
                    return false;
                }
            } else {
                dueDate = invoiceDAO.getDefaultDueDate();
            }

            // Create invoice
            String invoiceNumber = invoiceDAO.generateInvoiceNumber();
            Invoice invoice = new Invoice(invoiceNumber, businessUserId, customerEmail, amount);
            invoice.setCustomerName(customerName);
            invoice.setDescription(description);
            invoice.setDueDate(dueDate);
            invoice.setStatus("SENT");
            invoice.setPaymentTerms("Net 30");

            // Save invoice
            invoiceDAO.createInvoice(invoice);

            // Create notification
            notificationDAO.createInvoiceNotification(businessUserId, invoiceNumber, amount, "created");

            System.out.println("Invoice created successfully.");
            System.out.println("Invoice Number: " + invoiceNumber);
            System.out.println("Customer: " + customerName + " (" + customerEmail + ")");
            System.out.println("Amount: $" + amount);
            System.out.println("Due Date: " + dueDate);

            return true;

        } catch (Exception e) {
            System.out.println("Error creating invoice: " + e.getMessage());
            return false;
        }
    }

    public void viewInvoices(int businessUserId) {
        try {
            List<Invoice> invoices = invoiceDAO.getInvoicesByBusinessUserId(businessUserId);

            if (invoices.isEmpty()) {
                System.out.println("No invoices found.");
                return;
            }

            System.out.println("\n--- Your Invoices ---");
            System.out.println("Invoice #       | Customer            | Amount     | Status   | Due Date");
            System.out.println("---------------------------------------------------------------------");

            for (Invoice invoice : invoices) {
                String invoiceNo = invoice.getInvoiceNumber();
                String customer = invoice.getCustomerName() != null ?
                        (invoice.getCustomerName().length() > 15 ?
                                invoice.getCustomerName().substring(0, 15) + "..." : invoice.getCustomerName())
                        : invoice.getCustomerEmail();
                String amount = String.format("$%.2f", invoice.getTotalAmount());
                String status = invoice.getStatus();
                String dueDate = invoice.getDueDate() != null ?
                        invoice.getDueDate().toString() : "N/A";

                System.out.printf("%-15s | %-20s | %-10s | %-8s | %s%n",
                        invoiceNo, customer, amount, status, dueDate);
            }

            // Show summary
            double outstanding = invoiceDAO.getTotalOutstandingAmount(businessUserId);
            int draftCount = invoiceDAO.getInvoiceCountByStatus(businessUserId, "DRAFT");
            int sentCount = invoiceDAO.getInvoiceCountByStatus(businessUserId, "SENT");
            int paidCount = invoiceDAO.getInvoiceCountByStatus(businessUserId, "PAID");

            System.out.println("\n--- Invoice Summary ---");
            System.out.println("Draft Invoices: " + draftCount);
            System.out.println("Sent Invoices: " + sentCount);
            System.out.println("Paid Invoices: " + paidCount);
            System.out.println("Total Outstanding: $" + String.format("%.2f", outstanding));

        } catch (SQLException e) {
            System.out.println("Error viewing invoices: " + e.getMessage());
        }
    }

    public boolean applyForLoan(int businessUserId, double amount, String purpose, int termMonths) {
        try {
            // Validate business user
            User businessUser = userDAO.getUserById(businessUserId);
            if (businessUser == null || !businessUser.getAccountType().equals("BUSINESS")) {
                System.out.println("Business account required.");
                return false;
            }

            // Validate amount
            double minLoan = Double.parseDouble(properties.getProperty("business.min.loan.amount", "1000.00"));
            double maxLoan = Double.parseDouble(properties.getProperty("business.max.loan.amount", "100000.00"));

            if (amount < minLoan) {
                System.out.println("Minimum loan amount is $" + minLoan);
                return false;
            }

            if (amount > maxLoan) {
                System.out.println("Maximum loan amount is $" + maxLoan);
                return false;
            }

            if (termMonths < 3 || termMonths > 60) {
                System.out.println("Loan term must be between 3 and 60 months.");
                return false;
            }

            // Create loan application
            String applicationId = loanDAO.generateApplicationId();
            LoanApplication loan = new LoanApplication(applicationId, businessUserId, amount, purpose, termMonths);
            loan.setStatus("PENDING");

            // Save application
            loanDAO.createLoanApplication(loan);

            // Create notification
            notificationDAO.createLoanNotification(businessUserId, applicationId, "submitted");

            System.out.println("Loan application submitted successfully.");
            System.out.println("Application ID: " + applicationId);
            System.out.println("Amount: $" + amount);
            System.out.println("Term: " + termMonths + " months");
            System.out.println("Purpose: " + purpose);
            System.out.println("Status: Under Review");

            return true;

        } catch (Exception e) {
            System.out.println("Error applying for loan: " + e.getMessage());
            return false;
        }
    }

    public void viewLoanApplications(int businessUserId) {
        try {
            List<LoanApplication> loans = loanDAO.getLoanApplicationsByUserId(businessUserId);

            if (loans.isEmpty()) {
                System.out.println("No loan applications found.");
                return;
            }

            System.out.println("\n--- Your Loan Applications ---");
            System.out.println("Application ID   | Amount     | Term    | Status      | Purpose");
            System.out.println("---------------------------------------------------------------------");

            for (LoanApplication loan : loans) {
                String appId = loan.getApplicationId();
                String amount = String.format("$%.2f", loan.getLoanAmount());
                String term = loan.getTermMonths() + " months";
                String status = loan.getStatus();
                String purpose = loan.getPurpose().length() > 20 ?
                        loan.getPurpose().substring(0, 20) + "..." : loan.getPurpose();

                System.out.printf("%-16s | %-10s | %-7s | %-11s | %s%n",
                        appId, amount, term, status, purpose);
            }

            // Show summary
            int pendingCount = loanDAO.getLoanApplicationCountByStatus(businessUserId, "PENDING");
            int approvedCount = loanDAO.getLoanApplicationCountByStatus(businessUserId, "APPROVED");
            double totalApproved = loanDAO.getTotalApprovedLoanAmount(businessUserId);

            System.out.println("\n--- Loan Summary ---");
            System.out.println("Pending Applications: " + pendingCount);
            System.out.println("Approved Applications: " + approvedCount);
            System.out.println("Total Approved Amount: $" + String.format("%.2f", totalApproved));

        } catch (SQLException e) {
            System.out.println("Error viewing loan applications: " + e.getMessage());
        }
    }

    public void viewBusinessAnalytics(int businessUserId) {
        try {
            User businessUser = userDAO.getUserById(businessUserId);
            if (businessUser == null || !businessUser.getAccountType().equals("BUSINESS")) {
                System.out.println("Business account required.");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime monthStart = now.minusMonths(1);
            LocalDateTime yearStart = now.minusYears(1);

            // Get transaction data
            double monthlySent = transactionDAO.getTotalSentAmount(businessUserId, monthStart, now);
            double monthlyReceived = transactionDAO.getTotalReceivedAmount(businessUserId, monthStart, now);
            double yearlyReceived = transactionDAO.getTotalReceivedAmount(businessUserId, yearStart, now);

            // Get invoice data
            double outstandingInvoices = invoiceDAO.getTotalOutstandingAmount(businessUserId);
            double monthlyPaidInvoices = invoiceDAO.getTotalPaidAmount(businessUserId, monthStart, now);

            // Get loan data
            double totalApprovedLoans = loanDAO.getTotalApprovedLoanAmount(businessUserId);

            System.out.println("\n--- Business Analytics ---");
            System.out.println("=== Financial Overview ===");
            System.out.println("Current Balance: $" + String.format("%.2f", businessUser.getWalletBalance()));
            System.out.println("Outstanding Invoices: $" + String.format("%.2f", outstandingInvoices));
            System.out.println("Total Approved Loans: $" + String.format("%.2f", totalApprovedLoans));

            System.out.println("\n=== Last 30 Days ===");
            System.out.println("Money Sent: $" + String.format("%.2f", monthlySent));
            System.out.println("Money Received: $" + String.format("%.2f", monthlyReceived));
            System.out.println("Invoice Payments: $" + String.format("%.2f", monthlyPaidInvoices));
            System.out.println("Net Cash Flow: $" + String.format("%.2f", (monthlyReceived + monthlyPaidInvoices - monthlySent)));

            System.out.println("\n=== Last 12 Months ===");
            System.out.println("Total Revenue: $" + String.format("%.2f", yearlyReceived));

            // Show transaction counts
            int completedCount = transactionDAO.getTransactionCount(businessUserId, "COMPLETED");
            int pendingCount = transactionDAO.getTransactionCount(businessUserId, "PENDING");

            System.out.println("\n=== Transaction Summary ===");
            System.out.println("Completed Transactions: " + completedCount);
            System.out.println("Pending Transactions: " + pendingCount);

            // Show invoice status counts
            int draftInvoices = invoiceDAO.getInvoiceCountByStatus(businessUserId, "DRAFT");
            int sentInvoices = invoiceDAO.getInvoiceCountByStatus(businessUserId, "SENT");
            int paidInvoices = invoiceDAO.getInvoiceCountByStatus(businessUserId, "PAID");

            System.out.println("\n=== Invoice Status ===");
            System.out.println("Draft: " + draftInvoices);
            System.out.println("Sent: " + sentInvoices);
            System.out.println("Paid: " + paidInvoices);

        } catch (SQLException e) {
            System.out.println("Error viewing business analytics: " + e.getMessage());
        }
    }

    public boolean acceptPayment(int businessUserId, String customerIdentifier, double amount) {
        try {
            // Validate business user
            User businessUser = userDAO.getUserById(businessUserId);
            if (businessUser == null || !businessUser.getAccountType().equals("BUSINESS")) {
                System.out.println("Business account required.");
                return false;
            }

            // Find customer
            User customer = userDAO.getUserByEmailOrPhone(customerIdentifier);
            if (customer == null) {
                customer = userDAO.getUserByUsername(customerIdentifier);
            }

            if (customer == null) {
                System.out.println("Customer not found.");
                return false;
            }

            // Validate amount
            if (amount < MIN_TRANSACTION_AMOUNT) {
                System.out.println("Minimum payment amount is $" + MIN_TRANSACTION_AMOUNT);
                return false;
            }

            // Check customer balance
            if (customer.getWalletBalance() < amount) {
                System.out.println("Customer has insufficient balance.");
                return false;
            }

            // Calculate transaction fee
            double fee = (amount * TRANSACTION_FEE_PERCENTAGE) / 100;
            double totalAmount = amount + fee;

            // Verify customer's transaction PIN
            System.out.println("Customer " + customer.getFullName() + " needs to authorize payment.");
            System.out.print("Enter customer's transaction PIN: ");
            String pin = scanner.nextLine();

            if (!userDAO.verifyPassword(pin, customer.getTransactionPinHash())) {
                System.out.println("Invalid transaction PIN.");
                return false;
            }

            // Create transaction
            String transactionId = transactionDAO.generateTransactionId();
            Transaction transaction = new Transaction(transactionId, customer.getId(),
                    businessUserId, amount, "PAYMENT");
            transaction.setDescription("Business payment");
            transaction.setTransactionFee(fee);
            transaction.setStatus("COMPLETED");

            // Update balances
            userDAO.updateWalletBalance(customer.getId(), -totalAmount);
            userDAO.updateWalletBalance(businessUserId, amount);

            // Save transaction
            transactionDAO.createTransaction(transaction);

            // Update business user balance in memory
            businessUser.setWalletBalance(businessUser.getWalletBalance() + amount);

            // Create notifications
            notificationDAO.createTransactionNotification(customer.getId(), transactionId, amount, "paid");
            notificationDAO.createTransactionNotification(businessUserId, transactionId, amount, "received as payment");

            System.out.println("Payment accepted successfully!");
            System.out.println("Amount: $" + amount);
            System.out.println("Transaction ID: " + transactionId);
            System.out.println("Fee: $" + fee);
            System.out.println("Customer: " + customer.getFullName());

            return true;

        } catch (SQLException e) {
            System.out.println("Error accepting payment: " + e.getMessage());
            return false;
        }
    }

    // Helper methods
    private boolean isValidCardNumber(String cardNumber) {
        // Remove non-digits
        cardNumber = cardNumber.replaceAll("[^0-9]", "");

        // Check length
        if (cardNumber.length() < 13 || cardNumber.length() > 19) {
            return false;
        }

        // Luhn algorithm
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

//    get Wallet Balance
    public double getWalletBalance(int id){
        return paymentMethodDAO.getWalletBalance(id);
    }

    public void close() {
        scanner.close();
    }
}