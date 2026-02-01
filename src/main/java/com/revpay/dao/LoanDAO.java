package com.revpay.dao;

import com.revpay.models.LoanApplication;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {
    private Connection connection;

    public LoanDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public LoanApplication createLoanApplication(LoanApplication loan) throws SQLException {
        String sql = "INSERT INTO loan_applications (application_id, business_user_id, " +
                "loan_amount, purpose, term_months, interest_rate, status, " +
                "financial_documents, monthly_revenue, credit_score, " +
                "disbursed_amount, disbursed_date, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, loan.getApplicationId());
            stmt.setInt(2, loan.getBusinessUserId());
            stmt.setDouble(3, loan.getLoanAmount());
            stmt.setString(4, loan.getPurpose());
            stmt.setInt(5, loan.getTermMonths());

            if (loan.getInterestRate() != null) {
                stmt.setDouble(6, loan.getInterestRate());
            } else {
                stmt.setNull(6, Types.DOUBLE);
            }

            stmt.setString(7, loan.getStatus());
            stmt.setString(8, loan.getFinancialDocuments());

            if (loan.getMonthlyRevenue() != null) {
                stmt.setDouble(9, loan.getMonthlyRevenue());
            } else {
                stmt.setNull(9, Types.DOUBLE);
            }

            if (loan.getCreditScore() != null) {
                stmt.setInt(10, loan.getCreditScore());
            } else {
                stmt.setNull(10, Types.INTEGER);
            }

            if (loan.getDisbursedAmount() != null) {
                stmt.setDouble(11, loan.getDisbursedAmount());
            } else {
                stmt.setNull(11, Types.DOUBLE);
            }

            if (loan.getDisbursedDate() != null) {
                stmt.setDate(12, Date.valueOf(loan.getDisbursedDate()));
            } else {
                stmt.setNull(12, Types.DATE);
            }

            stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating loan application failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    loan.setId(generatedKeys.getInt(1));
                }
            }
        }
        return loan;
    }

    public List<LoanApplication> getLoanApplicationsByUserId(int userId) throws SQLException {
        List<LoanApplication> loans = new ArrayList<>();
        String sql = "SELECT * FROM loan_applications WHERE business_user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapResultSetToLoanApplication(rs));
                }
            }
        }
        return loans;
    }

    public LoanApplication getLoanApplicationById(String applicationId) throws SQLException {
        String sql = "SELECT * FROM loan_applications WHERE application_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, applicationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLoanApplication(rs);
                }
            }
        }
        return null;
    }

    public boolean updateLoanApplicationStatus(String applicationId, String status) throws SQLException {
        String sql = "UPDATE loan_applications SET status = ?, updated_at = ? WHERE application_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, applicationId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateLoanApplication(LoanApplication loan) throws SQLException {
        String sql = "UPDATE loan_applications SET loan_amount = ?, purpose = ?, " +
                "term_months = ?, interest_rate = ?, status = ?, " +
                "financial_documents = ?, monthly_revenue = ?, credit_score = ?, " +
                "disbursed_amount = ?, disbursed_date = ?, updated_at = ? " +
                "WHERE application_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, loan.getLoanAmount());
            stmt.setString(2, loan.getPurpose());
            stmt.setInt(3, loan.getTermMonths());

            if (loan.getInterestRate() != null) {
                stmt.setDouble(4, loan.getInterestRate());
            } else {
                stmt.setNull(4, Types.DOUBLE);
            }

            stmt.setString(5, loan.getStatus());
            stmt.setString(6, loan.getFinancialDocuments());

            if (loan.getMonthlyRevenue() != null) {
                stmt.setDouble(7, loan.getMonthlyRevenue());
            } else {
                stmt.setNull(7, Types.DOUBLE);
            }

            if (loan.getCreditScore() != null) {
                stmt.setInt(8, loan.getCreditScore());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            if (loan.getDisbursedAmount() != null) {
                stmt.setDouble(9, loan.getDisbursedAmount());
            } else {
                stmt.setNull(9, Types.DOUBLE);
            }

            if (loan.getDisbursedDate() != null) {
                stmt.setDate(10, Date.valueOf(loan.getDisbursedDate()));
            } else {
                stmt.setNull(10, Types.DATE);
            }

            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(12, loan.getApplicationId());

            return stmt.executeUpdate() > 0;
        }
    }

    public List<LoanApplication> getPendingLoanApplications() throws SQLException {
        List<LoanApplication> loans = new ArrayList<>();
        String sql = "SELECT * FROM loan_applications WHERE status = 'PENDING' ORDER BY created_at ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapResultSetToLoanApplication(rs));
                }
            }
        }
        return loans;
    }

    public int getLoanApplicationCountByStatus(int userId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loan_applications WHERE business_user_id = ? AND status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, status);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public double getTotalApprovedLoanAmount(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(loan_amount), 0) FROM loan_applications " +
                "WHERE business_user_id = ? AND status IN ('APPROVED', 'DISBURSED')";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    private LoanApplication mapResultSetToLoanApplication(ResultSet rs) throws SQLException {
        LoanApplication loan = new LoanApplication();
        loan.setId(rs.getInt("id"));
        loan.setApplicationId(rs.getString("application_id"));
        loan.setBusinessUserId(rs.getInt("business_user_id"));
        loan.setLoanAmount(rs.getDouble("loan_amount"));
        loan.setPurpose(rs.getString("purpose"));
        loan.setTermMonths(rs.getInt("term_months"));

        double interestRate = rs.getDouble("interest_rate");
        if (!rs.wasNull()) {
            loan.setInterestRate(interestRate);
        }

        loan.setStatus(rs.getString("status"));
        loan.setFinancialDocuments(rs.getString("financial_documents"));

        double monthlyRevenue = rs.getDouble("monthly_revenue");
        if (!rs.wasNull()) {
            loan.setMonthlyRevenue(monthlyRevenue);
        }

        int creditScore = rs.getInt("credit_score");
        if (!rs.wasNull()) {
            loan.setCreditScore(creditScore);
        }

        double disbursedAmount = rs.getDouble("disbursed_amount");
        if (!rs.wasNull()) {
            loan.setDisbursedAmount(disbursedAmount);
        }

        Date disbursedDate = rs.getDate("disbursed_date");
        if (disbursedDate != null) {
            loan.setDisbursedDate(disbursedDate.toLocalDate());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            loan.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            loan.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return loan;
    }

    public String generateApplicationId() {
        return "LOAN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    public boolean createLoanRepayment(int loanApplicationId, int installmentNumber,
                                       LocalDate dueDate, double amountDue) throws SQLException {
        String sql = "INSERT INTO loan_repayments (loan_application_id, installment_number, " +
                "due_date, amount_due, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, loanApplicationId);
            stmt.setInt(2, installmentNumber);
            stmt.setDate(3, Date.valueOf(dueDate));
            stmt.setDouble(4, amountDue);
            stmt.setString(5, "PENDING");
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateRepaymentStatus(int repaymentId, String status, double amountPaid) throws SQLException {
        String sql = "UPDATE loan_repayments SET status = ?, amount_paid = ?, " +
                "payment_date = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setDouble(2, amountPaid);

            if ("PAID".equals(status)) {
                stmt.setDate(3, Date.valueOf(LocalDate.now()));
            } else {
                stmt.setNull(3, Types.DATE);
            }

            stmt.setInt(4, repaymentId);

            return stmt.executeUpdate() > 0;
        }
    }
}