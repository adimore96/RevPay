package com.revpay.dao;

import com.revpay.models.Invoice;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {
    private Connection connection;

    public InvoiceDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public Invoice createInvoice(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (invoice_number, business_user_id, " +
                "customer_email, customer_name, amount, tax_amount, total_amount, " +
                "currency, items, description, due_date, status, payment_terms, " +
                "created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, invoice.getInvoiceNumber());
            stmt.setInt(2, invoice.getBusinessUserId());
            stmt.setString(3, invoice.getCustomerEmail());
            stmt.setString(4, invoice.getCustomerName());
            stmt.setDouble(5, invoice.getAmount());
            stmt.setDouble(6, invoice.getTaxAmount());
            stmt.setDouble(7, invoice.getTotalAmount());
            stmt.setString(8, invoice.getCurrency());
            stmt.setString(9, invoice.getItems());
            stmt.setString(10, invoice.getDescription());

            if (invoice.getDueDate() != null) {
                stmt.setDate(11, Date.valueOf(invoice.getDueDate()));
            } else {
                stmt.setNull(11, Types.DATE);
            }

            stmt.setString(12, invoice.getStatus());
            stmt.setString(13, invoice.getPaymentTerms());
            stmt.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(15, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating invoice failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    invoice.setId(generatedKeys.getInt(1));
                }
            }
        }
        return invoice;
    }

    public List<Invoice> getInvoicesByBusinessUserId(int businessUserId) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE business_user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, businessUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }

    public List<Invoice> getInvoicesByCustomerEmail(String customerEmail) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE customer_email = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, customerEmail);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }

    public Invoice getInvoiceByNumber(String invoiceNumber) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE invoice_number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, invoiceNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInvoice(rs);
                }
            }
        }
        return null;
    }

    public boolean updateInvoiceStatus(String invoiceNumber, String status) throws SQLException {
        String sql = "UPDATE invoices SET status = ?, updated_at = ? WHERE invoice_number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, invoiceNumber);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean markInvoiceAsPaid(String invoiceNumber) throws SQLException {
        String sql = "UPDATE invoices SET status = 'PAID', updated_at = ? WHERE invoice_number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, invoiceNumber);

            return stmt.executeUpdate() > 0;
        }
    }

    public List<Invoice> getOverdueInvoices() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE status IN ('SENT', 'VIEWED') " +
                "AND due_date < ? ORDER BY due_date ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(LocalDate.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }

    public double getTotalOutstandingAmount(int businessUserId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM invoices " +
                "WHERE business_user_id = ? AND status IN ('SENT', 'VIEWED')";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, businessUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double getTotalPaidAmount(int businessUserId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM invoices " +
                "WHERE business_user_id = ? AND status = 'PAID' " +
                "AND updated_at BETWEEN ? AND ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, businessUserId);
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            stmt.setTimestamp(3, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public int getInvoiceCountByStatus(int businessUserId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM invoices WHERE business_user_id = ? AND status = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, businessUserId);
            stmt.setString(2, status);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id"));
        invoice.setInvoiceNumber(rs.getString("invoice_number"));
        invoice.setBusinessUserId(rs.getInt("business_user_id"));
        invoice.setCustomerEmail(rs.getString("customer_email"));
        invoice.setCustomerName(rs.getString("customer_name"));
        invoice.setAmount(rs.getDouble("amount"));
        invoice.setTaxAmount(rs.getDouble("tax_amount"));
        invoice.setTotalAmount(rs.getDouble("total_amount"));
        invoice.setCurrency(rs.getString("currency"));
        invoice.setItems(rs.getString("items"));
        invoice.setDescription(rs.getString("description"));

        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) {
            invoice.setDueDate(dueDate.toLocalDate());
        }

        invoice.setStatus(rs.getString("status"));
        invoice.setPaymentTerms(rs.getString("payment_terms"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            invoice.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            invoice.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return invoice;
    }

    public String generateInvoiceNumber() {
        return "INV" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    public LocalDate getDefaultDueDate() {
        return LocalDate.now().plusDays(30);
    }
}