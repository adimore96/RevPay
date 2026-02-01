package com.revpay.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanApplication {
    private int id;
    private String applicationId;
    private int businessUserId;
    private double loanAmount;
    private String purpose;
    private int termMonths;
    private Double interestRate;
    private String status;
    private String financialDocuments;
    private Double monthlyRevenue;
    private Integer creditScore;
    private Double disbursedAmount;
    private LocalDate disbursedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public LoanApplication() {}

    public LoanApplication(String applicationId, int businessUserId,
                           double loanAmount, String purpose, int termMonths) {
        this.applicationId = applicationId;
        this.businessUserId = businessUserId;
        this.loanAmount = loanAmount;
        this.purpose = purpose;
        this.termMonths = termMonths;
        this.status = "PENDING";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public int getBusinessUserId() { return businessUserId; }
    public void setBusinessUserId(int businessUserId) { this.businessUserId = businessUserId; }

    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public Double getInterestRate() { return interestRate; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFinancialDocuments() { return financialDocuments; }
    public void setFinancialDocuments(String financialDocuments) {
        this.financialDocuments = financialDocuments;
    }

    public Double getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(Double monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }

    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }

    public Double getDisbursedAmount() { return disbursedAmount; }
    public void setDisbursedAmount(Double disbursedAmount) { this.disbursedAmount = disbursedAmount; }

    public LocalDate getDisbursedDate() { return disbursedDate; }
    public void setDisbursedDate(LocalDate disbursedDate) { this.disbursedDate = disbursedDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "LoanApplication{" +
                "applicationId='" + applicationId + '\'' +
                ", loanAmount=" + loanAmount +
                ", purpose='" + purpose + '\'' +
                ", status='" + status + '\'' +
                ", termMonths=" + termMonths +
                '}';
    }
}