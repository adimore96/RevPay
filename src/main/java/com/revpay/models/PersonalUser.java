package com.revpay.models;

public class PersonalUser extends User {

    public PersonalUser() {
        super();
        setAccountType("PERSONAL");
    }

    public PersonalUser(String username, String email, String phoneNumber,
                        String passwordHash, String fullName) {
        super(username, email, phoneNumber, passwordHash, "PERSONAL", fullName);
    }
}