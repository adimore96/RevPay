package com.revpay.services;

import java.util.regex.Pattern;

public class ValidationService {

    // Validation patterns
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]{3,20}$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z\\s]{2,50}$");
    private static final Pattern CARD_NUMBER_PATTERN =
            Pattern.compile("^\\d{13,19}$");
    private static final Pattern CVV_PATTERN =
            Pattern.compile("^\\d{3,4}$");
    private static final Pattern TAX_ID_PATTERN =
            Pattern.compile("^\\d{9}$");

    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        // Remove any non-digit characters except leading +
        phone = phone.replaceAll("[^+\\d]", "");
        return PHONE_PATTERN.matcher(phone).matches();
    }

    public boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return NAME_PATTERN.matcher(name).matches();
    }

    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return false;
        }
        // Remove any non-digit characters
        cardNumber = cardNumber.replaceAll("[^\\d]", "");
        if (!CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
            return false;
        }
        return isValidLuhn(cardNumber);
    }

    public boolean isValidCVV(String cvv) {
        if (cvv == null || cvv.isEmpty()) {
            return false;
        }
        return CVV_PATTERN.matcher(cvv).matches();
    }

    public boolean isValidExpiryDate(int month, int year) {
        if (month < 1 || month > 12) {
            return false;
        }

        int currentYear = java.time.Year.now().getValue();
        int currentMonth = java.time.LocalDate.now().getMonthValue();

        if (year < currentYear) {
            return false;
        }
        if (year == currentYear && month < currentMonth) {
            return false;
        }
        return true;
    }

    public boolean isValidAmount(double amount, double min, double max) {
        return amount >= min && amount <= max;
    }

    public boolean isValidTransactionPIN(String pin) {
        if (pin == null || pin.length() != 6) {
            return false;
        }
        return pin.matches("\\d{6}");
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        // Check for at least one digit, one lowercase, one uppercase
        boolean hasDigit = false;
        boolean hasLower = false;
        boolean hasUpper = false;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isUpperCase(c)) hasUpper = true;
        }

        return hasDigit && hasLower && hasUpper;
    }

    public boolean isValidTaxId(String taxId) {
        if (taxId == null || taxId.isEmpty()) {
            return false;
        }
        return TAX_ID_PATTERN.matcher(taxId).matches();
    }

    public boolean isValidBusinessType(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return false;
        }
        return businessType.length() >= 2 && businessType.length() <= 50;
    }

    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
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

    public String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&;]", "");
    }

    public boolean isStrongPassword(String password) {
        if (!isValidPassword(password)) {
            return false;
        }

        // Additional strength checks
        boolean hasSpecial = false;
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        for (char c : password.toCharArray()) {
            if (specialChars.indexOf(c) >= 0) {
                hasSpecial = true;
                break;
            }
        }

        return hasSpecial && password.length() >= 10;
    }

    public String formatPhoneNumber(String phone) {
        if (phone == null) {
            return "";
        }

        // Remove all non-digit characters
        String digits = phone.replaceAll("[^\\d]", "");

        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6));
        } else if (digits.length() == 11 && digits.startsWith("1")) {
            return String.format("+1 (%s) %s-%s",
                    digits.substring(1, 4),
                    digits.substring(4, 7),
                    digits.substring(7));
        }

        return phone;
    }
}