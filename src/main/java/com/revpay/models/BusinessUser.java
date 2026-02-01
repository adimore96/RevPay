package com.revpay.models;

public class BusinessUser extends User {

    public BusinessUser() {
        super();
        setAccountType("BUSINESS");
    }

    public BusinessUser(String username, String email, String phoneNumber,
                        String passwordHash, String fullName,
                        String businessName, String businessType) {
        super(username, email, phoneNumber, passwordHash, "BUSINESS", fullName);
        setBusinessName(businessName);
        setBusinessType(businessType);
    }
}