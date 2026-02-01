package com.revpay.models;

import java.time.LocalDateTime;

public class PaymentMethod {
    private int id;
    private int userId;
    private String cardType;
    private String cardNumberEncrypted;
    private String cardHolderName;
    private int expiryMonth;
    private int expiryYear;
    private String cvvEncrypted;
    private boolean isDefault;
    private boolean isActive;
    private String bankName;
    private String accountNumberEncrypted;
    private String routingNumber;
    private LocalDateTime createdAt;

    // Constructors
    public PaymentMethod() {}

    public PaymentMethod(int userId, String cardType, String cardNumberEncrypted,
                         String cardHolderName, int expiryMonth, int expiryYear,
                         String cvvEncrypted) {
        this.userId = userId;
        this.cardType = cardType;
        this.cardNumberEncrypted = cardNumberEncrypted;
        this.cardHolderName = cardHolderName;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvvEncrypted = cvvEncrypted;
        this.isDefault = false;
        this.isActive = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getCardNumberEncrypted() { return cardNumberEncrypted; }
    public void setCardNumberEncrypted(String cardNumberEncrypted) {
        this.cardNumberEncrypted = cardNumberEncrypted;
    }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public int getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(int expiryMonth) { this.expiryMonth = expiryMonth; }

    public int getExpiryYear() { return expiryYear; }
    public void setExpiryYear(int expiryYear) { this.expiryYear = expiryYear; }

    public String getCvvEncrypted() { return cvvEncrypted; }
    public void setCvvEncrypted(String cvvEncrypted) { this.cvvEncrypted = cvvEncrypted; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumberEncrypted() { return accountNumberEncrypted; }
    public void setAccountNumberEncrypted(String accountNumberEncrypted) {
        this.accountNumberEncrypted = accountNumberEncrypted;
    }

    public String getRoutingNumber() { return routingNumber; }
    public void setRoutingNumber(String routingNumber) { this.routingNumber = routingNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PaymentMethod{" +
                "id=" + id +
                ", cardType='" + cardType + '\'' +
                ", cardHolderName='" + cardHolderName + '\'' +
                ", expiry=" + expiryMonth + "/" + expiryYear +
                ", isDefault=" + isDefault +
                '}';
    }
}