package com.revpay.services;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class EncryptionService {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private String secretKey;
    private String iv;

    public EncryptionService() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            this.secretKey = properties.getProperty("encryption.aes.key", "YourSecretKey12345");
            this.iv = properties.getProperty("encryption.aes.iv", "YourIV12345678901");

            // Ensure key and IV are correct length
            if (secretKey.length() < 16) {
                secretKey = String.format("%-16s", secretKey).substring(0, 16);
            } else if (secretKey.length() > 16) {
                secretKey = secretKey.substring(0, 16);
            }

            if (iv.length() < 16) {
                iv = String.format("%-16s", iv).substring(0, 16);
            } else if (iv.length() > 16) {
                iv = iv.substring(0, 16);
            }
        } catch (Exception e) {
            System.err.println("Error loading encryption properties: " + e.getMessage());
            // Default fallback values
            this.secretKey = "YourSecretKey12345".substring(0, 16);
            this.iv = "YourIV12345678901".substring(0, 16);
        }
    }

    public String encrypt(String data) {
        try {
            if (data == null || data.isEmpty()) {
                return data;
            }

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            System.err.println("Error encrypting data: " + e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            if (encryptedData == null || encryptedData.isEmpty()) {
                return encryptedData;
            }

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("Error decrypting data: " + e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    public String maskCVV(String cvv) {
        return "***";
    }

    public String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}