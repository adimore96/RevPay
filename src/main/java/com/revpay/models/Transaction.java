package com.revpay.models;

import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private String transactionId;
    private int senderId;
    private int receiverId;
    private double amount;
    private String transactionType;
    private String status;
    private String description;
    private Integer paymentMethodId;
    private double transactionFee;
    private boolean isRecurring;
    private String recurringFrequency;
    private Integer invoiceId;
    private LocalDateTime createdAt;

    // Constructors
    public Transaction() {}

    public Transaction(String transactionId, int senderId, int receiverId,
                       double amount, String transactionType) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.status = "PENDING";
        this.transactionFee = 0.0;
        this.isRecurring = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public double getTransactionFee() { return transactionFee; }
    public void setTransactionFee(double transactionFee) {
        this.transactionFee = transactionFee;
    }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }

    public String getRecurringFrequency() { return recurringFrequency; }
    public void setRecurringFrequency(String recurringFrequency) {
        this.recurringFrequency = recurringFrequency;
    }

    public Integer getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Integer invoiceId) { this.invoiceId = invoiceId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", type='" + transactionType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}