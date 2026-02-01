package com.revpay.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Invoice {
    private int id;
    private String invoiceNumber;
    private int businessUserId;
    private String customerEmail;
    private String customerName;
    private double amount;
    private double taxAmount;
    private double totalAmount;
    private String currency;
    private String items; // JSON string
    private String description;
    private LocalDate dueDate;
    private String status;
    private String paymentTerms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Invoice() {}

    public Invoice(String invoiceNumber, int businessUserId,
                   String customerEmail, double amount) {
        this.invoiceNumber = invoiceNumber;
        this.businessUserId = businessUserId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.taxAmount = 0.0;
        this.totalAmount = amount;
        this.currency = "USD";
        this.status = "DRAFT";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public int getBusinessUserId() { return businessUserId; }
    public void setBusinessUserId(int businessUserId) { this.businessUserId = businessUserId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceNumber='" + invoiceNumber + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", dueDate=" + dueDate +
                '}';
    }
}