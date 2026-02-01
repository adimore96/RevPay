package com.revpay.models;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String transactionPinHash;
    private String accountType;
    private String fullName;
    private String businessName;
    private String businessType;
    private String taxId;
    private String businessAddress;
    private String verificationDocuments;
    private double walletBalance;
    private String securityQuestion1;
    private String securityAnswer1Hash;
    private String securityQuestion2;
    private String securityAnswer2Hash;
    private boolean isVerified;
    private boolean isLocked;
    private int failedLoginAttempts;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public User() {}

    public User(String username, String email, String phoneNumber, String passwordHash,
                String accountType, String fullName) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
        this.fullName = fullName;
        this.walletBalance = 0.0;
        this.isVerified = false;
        this.isLocked = false;
        this.failedLoginAttempts = 0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getTransactionPinHash() { return transactionPinHash; }
    public void setTransactionPinHash(String transactionPinHash) {
        this.transactionPinHash = transactionPinHash;
    }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }

    public String getVerificationDocuments() { return verificationDocuments; }
    public void setVerificationDocuments(String verificationDocuments) {
        this.verificationDocuments = verificationDocuments;
    }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public String getSecurityQuestion1() { return securityQuestion1; }
    public void setSecurityQuestion1(String securityQuestion1) {
        this.securityQuestion1 = securityQuestion1;
    }

    public String getSecurityAnswer1Hash() { return securityAnswer1Hash; }
    public void setSecurityAnswer1Hash(String securityAnswer1Hash) {
        this.securityAnswer1Hash = securityAnswer1Hash;
    }

    public String getSecurityQuestion2() { return securityQuestion2; }
    public void setSecurityQuestion2(String securityQuestion2) {
        this.securityQuestion2 = securityQuestion2;
    }

    public String getSecurityAnswer2Hash() { return securityAnswer2Hash; }
    public void setSecurityAnswer2Hash(String securityAnswer2Hash) {
        this.securityAnswer2Hash = securityAnswer2Hash;
    }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", accountType='" + accountType + '\'' +
                ", fullName='" + fullName + '\'' +
                ", walletBalance=" + walletBalance +
                '}';
    }
}