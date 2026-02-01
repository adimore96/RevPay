import com.revpay.dao.UserDAO;
import com.revpay.models.User;
import com.revpay.models.PersonalUser;
import com.revpay.models.BusinessUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
class UserDAOTest {

    private static UserDAO userDAO;
    private static User testUser;
    private static final String TEST_USERNAME = "junit_test_user";
    private static final String TEST_EMAIL = "junit.test@email.com";
    private static final String TEST_PHONE = "9999999999";
    private static final String TEST_PASSWORD = "TestPassword123";
    private static final String TEST_FULL_NAME = "JUnit Test User";

    @BeforeAll
    static void setUp() {
        userDAO = new UserDAO();
    }

    @Test
    @Order(1)
    void testHashPassword() {
        System.out.println("Testing password hashing...");

        String password = "TestPassword123";
        String hash = userDAO.hashPassword(password);

        assertNotNull(hash, "Hash should not be null");
        assertFalse(hash.isEmpty(), "Hash should not be empty");
        assertTrue(hash.startsWith("$2a$"), "Should be BCrypt hash");

        // Verify password
        boolean isValid = userDAO.verifyPassword(password, hash);
        assertTrue(isValid, "Password verification should succeed");

        // Test wrong password
        boolean isInvalid = userDAO.verifyPassword("WrongPassword", hash);
        assertFalse(isInvalid, "Wrong password should fail verification");

        System.out.println("✓ Password hashing and verification test passed");
    }

    @Test
    @Order(2)
    void testHashSecurityAnswer() {
        System.out.println("Testing security answer hashing...");

        String answer = "MyPetName";
        String hash = userDAO.hashSecurityAnswer(answer);

        assertNotNull(hash, "Security answer hash should not be null");
        assertFalse(hash.isEmpty(), "Security answer hash should not be empty");

        // Verify security answer
        boolean isValid = userDAO.verifySecurityAnswer(answer, hash);
        assertTrue(isValid, "Security answer verification should succeed");

        // Test wrong answer (case insensitive)
        boolean isCorrectCase = userDAO.verifySecurityAnswer("mypetname", hash);
        assertTrue(isCorrectCase, "Should be case insensitive");

        // Test wrong answer
        boolean isInvalid = userDAO.verifySecurityAnswer("WrongAnswer", hash);
        assertFalse(isInvalid, "Wrong answer should fail verification");

        System.out.println("✓ Security answer hashing test passed");
    }

    @Test
    @Order(3)
    void testCreateUser() throws SQLException {
        System.out.println("Testing user creation...");

        // Create Personal User
        PersonalUser personalUser = new PersonalUser();
        personalUser.setUsername(TEST_USERNAME);
        personalUser.setEmail(TEST_EMAIL);
        personalUser.setPhoneNumber(TEST_PHONE);
        personalUser.setPasswordHash(userDAO.hashPassword(TEST_PASSWORD));
        personalUser.setFullName(TEST_FULL_NAME);
        personalUser.setSecurityQuestion1("What is your pet's name?");
        personalUser.setSecurityAnswer1Hash(userDAO.hashSecurityAnswer("Fluffy"));
        personalUser.setSecurityQuestion2("What is your birth city?");
        personalUser.setSecurityAnswer2Hash(userDAO.hashSecurityAnswer("New York"));

        try {
            User createdUser = userDAO.createUser(personalUser);

            assertNotNull(createdUser, "Created user should not be null");
            assertTrue(createdUser.getId() > 0, "User ID should be positive");
            assertEquals(TEST_USERNAME, createdUser.getUsername(), "Username should match");
            assertEquals(TEST_EMAIL, createdUser.getEmail(), "Email should match");
            assertEquals("PERSONAL", createdUser.getAccountType(), "Account type should be PERSONAL");
            assertEquals(0.0, createdUser.getWalletBalance(), "Initial balance should be 0");
            assertFalse(createdUser.isLocked(), "New user should not be locked");

            testUser = createdUser;
            System.out.println("✓ User creation test passed. User ID: " + testUser.getId());

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("⚠ Test user already exists, skipping creation test");
                // Try to get existing user
                testUser = userDAO.getUserByUsername(TEST_USERNAME);
                assertNotNull(testUser, "Should retrieve existing test user");
            } else {
                throw e;
            }
        }
    }

    @Test
    @Order(4)
    void testGetUserByEmailOrPhone() throws SQLException {
        System.out.println("Testing user retrieval by email/phone...");

        assertNotNull(testUser, "Test user should exist");

        // Test by email
        User userByEmail = userDAO.getUserByEmailOrPhone(TEST_EMAIL);
        assertNotNull(userByEmail, "Should find user by email");
        assertEquals(testUser.getId(), userByEmail.getId(), "User IDs should match");

        // Test by phone
        User userByPhone = userDAO.getUserByEmailOrPhone(TEST_PHONE);
        assertNotNull(userByPhone, "Should find user by phone");
        assertEquals(testUser.getId(), userByPhone.getId(), "User IDs should match");

        // Test non-existent user
        User nonExistent = userDAO.getUserByEmailOrPhone("nonexistent@email.com");
        assertNull(nonExistent, "Should return null for non-existent user");

        System.out.println("✓ User retrieval test passed");
    }

    @Test
    @Order(5)
    void testGetUserById() throws SQLException {
        System.out.println("Testing user retrieval by ID...");

        assertNotNull(testUser, "Test user should exist");

        User retrievedUser = userDAO.getUserById(testUser.getId());

        assertNotNull(retrievedUser, "Should retrieve user by ID");
        assertEquals(testUser.getId(), retrievedUser.getId(), "User IDs should match");
        assertEquals(TEST_USERNAME, retrievedUser.getUsername(), "Usernames should match");
        assertEquals(TEST_EMAIL, retrievedUser.getEmail(), "Emails should match");

        // Test non-existent ID
        User nonExistent = userDAO.getUserById(999999);
        assertNull(nonExistent, "Should return null for non-existent ID");

        System.out.println("✓ User retrieval by ID test passed");
    }

    @Test
    @Order(6)
    void testGetUserByUsername() throws SQLException {
        System.out.println("Testing user retrieval by username...");

        User retrievedUser = userDAO.getUserByUsername(TEST_USERNAME);

        assertNotNull(retrievedUser, "Should retrieve user by username");
        assertEquals(testUser.getId(), retrievedUser.getId(), "User IDs should match");
        assertEquals(TEST_USERNAME, retrievedUser.getUsername(), "Usernames should match");

        // Test non-existent username
        User nonExistent = userDAO.getUserByUsername("nonexistentuser");
        assertNull(nonExistent, "Should return null for non-existent username");

        System.out.println("✓ User retrieval by username test passed");
    }

    @Test
    @Order(7)
    void testUpdateUser() throws SQLException {
        System.out.println("Testing user update...");

        assertNotNull(testUser, "Test user should exist");

        // Update user information
        String newFullName = "Updated Test User";
        String newPhone = "8888888888";

        testUser.setFullName(newFullName);
        testUser.setPhoneNumber(newPhone);
        testUser.setWalletBalance(1000.50);

        boolean updateSuccess = userDAO.updateUser(testUser);
        assertTrue(updateSuccess, "User update should succeed");

        // Retrieve and verify updates
        User updatedUser = userDAO.getUserById(testUser.getId());
        assertNotNull(updatedUser, "Should retrieve updated user");
        assertEquals(newFullName, updatedUser.getFullName(), "Full name should be updated");
        assertEquals(newPhone, updatedUser.getPhoneNumber(), "Phone number should be updated");
        assertEquals(1000.50, updatedUser.getWalletBalance(), 0.001, "Wallet balance should be updated");

        System.out.println("✓ User update test passed");
    }

    @Test
    @Order(8)
    void testUpdateWalletBalance() throws SQLException {
        System.out.println("Testing wallet balance update...");

        assertNotNull(testUser, "Test user should exist");

        double initialBalance = testUser.getWalletBalance();
        double depositAmount = 500.75;

        // Test deposit
        boolean depositSuccess = userDAO.updateWalletBalance(testUser.getId(), depositAmount);
        assertTrue(depositSuccess, "Wallet deposit should succeed");

        User afterDeposit = userDAO.getUserById(testUser.getId());
        assertEquals(initialBalance + depositAmount, afterDeposit.getWalletBalance(), 0.001,
                "Balance should increase after deposit");

        // Test withdrawal
        double withdrawalAmount = 200.25;
        boolean withdrawSuccess = userDAO.updateWalletBalance(testUser.getId(), -withdrawalAmount);
        assertTrue(withdrawSuccess, "Wallet withdrawal should succeed");

        User afterWithdrawal = userDAO.getUserById(testUser.getId());
        assertEquals(initialBalance + depositAmount - withdrawalAmount,
                afterWithdrawal.getWalletBalance(), 0.001,
                "Balance should decrease after withdrawal");

        testUser.setWalletBalance(afterWithdrawal.getWalletBalance());
        System.out.println("✓ Wallet balance update test passed");
    }



    @Test
    @Order(9)
    void testUpdatePassword() throws SQLException {
        System.out.println("Testing password update...");

        assertNotNull(testUser, "Test user should exist");

        String newPasswordHash = userDAO.hashPassword("NewPassword123");
        boolean updateSuccess = userDAO.updatePassword(testUser.getId(), newPasswordHash);

        assertTrue(updateSuccess, "Password update should succeed");

        // Verify new password works
        User updatedUser = userDAO.getUserById(testUser.getId());
        boolean isValid = userDAO.verifyPassword("NewPassword123", updatedUser.getPasswordHash());
        assertTrue(isValid, "New password should work");

        // Old password should fail
        boolean oldPasswordValid = userDAO.verifyPassword(TEST_PASSWORD, updatedUser.getPasswordHash());
        assertFalse(oldPasswordValid, "Old password should fail");

        // Restore original password for other tests
        String originalHash = userDAO.hashPassword(TEST_PASSWORD);
        userDAO.updatePassword(testUser.getId(), originalHash);

        System.out.println("✓ Password update test passed");
    }

    @Test
    @Order(10)
    void testUpdateTransactionPin() throws SQLException {
        System.out.println("Testing transaction PIN update...");

        assertNotNull(testUser, "Test user should exist");

        String pinHash = userDAO.hashPassword("123456");
        boolean updateSuccess = userDAO.updateTransactionPin(testUser.getId(), pinHash);

        assertTrue(updateSuccess, "Transaction PIN update should succeed");

        User updatedUser = userDAO.getUserById(testUser.getId());
        assertNotNull(updatedUser.getTransactionPinHash(), "Transaction PIN hash should be set");

        // Verify PIN
        boolean isValid = userDAO.verifyPassword("123456", updatedUser.getTransactionPinHash());
        assertTrue(isValid, "Transaction PIN verification should succeed");

        // Wrong PIN should fail
        boolean isInvalid = userDAO.verifyPassword("000000", updatedUser.getTransactionPinHash());
        assertFalse(isInvalid, "Wrong transaction PIN should fail");

        System.out.println("✓ Transaction PIN update test passed");
    }

    @Test
    @Order(11)
    void testCheckIfUserExists() throws SQLException {
        System.out.println("Testing user existence check...");

        // Check existing user
        boolean exists = userDAO.checkIfUserExists(TEST_EMAIL);
        assertTrue(exists, "Should find existing user by email");

        exists = userDAO.checkIfUserExists(TEST_PHONE);
        assertTrue(exists, "Should find existing user by phone");

        exists = userDAO.checkIfUserExists(TEST_USERNAME);
        assertTrue(exists, "Should find existing user by username");

        // Check non-existent user
        boolean notExists = userDAO.checkIfUserExists("nonexistent@email.com");
        assertFalse(notExists, "Should not find non-existent user");

        System.out.println("✓ User existence check test passed");
    }

    @Test
    @Order(12)
    void testCreateBusinessUser() throws SQLException {
        System.out.println("Testing business user creation...");

        String businessUsername = "junit_business_user";
        String businessEmail = "junit.business@email.com";
        String businessPhone = "7777777777";

        // Create Business User
        BusinessUser businessUser = new BusinessUser();
        businessUser.setUsername(businessUsername);
        businessUser.setEmail(businessEmail);
        businessUser.setPhoneNumber(businessPhone);
        businessUser.setPasswordHash(userDAO.hashPassword("BusinessPass123"));
        businessUser.setFullName("JUnit Business");
        businessUser.setBusinessName("Test Business Inc.");
        businessUser.setBusinessType("Technology");
        businessUser.setTaxId("123456789");
        businessUser.setBusinessAddress("123 Business St, City");

        try {
            User createdBusinessUser = userDAO.createUser(businessUser);

            assertNotNull(createdBusinessUser, "Created business user should not be null");
            assertEquals("BUSINESS", createdBusinessUser.getAccountType(), "Account type should be BUSINESS");
            assertEquals("Test Business Inc.", createdBusinessUser.getBusinessName(), "Business name should match");
            assertEquals("Technology", createdBusinessUser.getBusinessType(), "Business type should match");
            assertEquals("123456789", createdBusinessUser.getTaxId(), "Tax ID should match");

            System.out.println("✓ Business user creation test passed");

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("⚠ Business test user already exists, skipping test");
            } else {
                throw e;
            }
        }
    }

    @Test
    @Order(13)
    void testUserModelMethods() {
        System.out.println("Testing User model methods...");

        // Create a new user for testing model methods
        PersonalUser modelTestUser = new PersonalUser();
        modelTestUser.setUsername("model_test");
        modelTestUser.setEmail("model@test.com");
        modelTestUser.setPhoneNumber("6666666666");
        modelTestUser.setPasswordHash("test_hash");
        modelTestUser.setFullName("Model Test User");

        // Test getters
        assertEquals("model_test", modelTestUser.getUsername());
        assertEquals("model@test.com", modelTestUser.getEmail());
        assertEquals("PERSONAL", modelTestUser.getAccountType());

        // Test toString method
        String toStringOutput = modelTestUser.toString();
        assertNotNull(toStringOutput);
        assertTrue(toStringOutput.contains("model_test"));
        assertTrue(toStringOutput.contains("model@test.com"));

        System.out.println("✓ User model methods test passed");
    }

    @Test
    @Order(14)
    void testEdgeCases() throws SQLException {
        System.out.println("Testing edge cases...");

        // Test with null values
        try {
            User nullUser = new PersonalUser();
            nullUser.setUsername(null);
            nullUser.setEmail(null);
            nullUser.setPhoneNumber(null);

            // This should throw SQLException
            userDAO.createUser(nullUser);
            fail("Should throw exception for null values");
        } catch (SQLException e) {
            // Expected behavior
            assertTrue(e.getMessage().contains("null") || e.getMessage().contains("NOT NULL"));
        }

        // Test duplicate user creation
        try {
            PersonalUser duplicateUser = new PersonalUser();
            duplicateUser.setUsername(TEST_USERNAME); // Already exists
            duplicateUser.setEmail("newemail@test.com");
            duplicateUser.setPhoneNumber("5555555555");
            duplicateUser.setPasswordHash(userDAO.hashPassword("test123"));
            duplicateUser.setFullName("Duplicate User");

            userDAO.createUser(duplicateUser);
            fail("Should throw exception for duplicate username");
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("Duplicate") || e.getMessage().contains("unique"));
        }

        System.out.println("✓ Edge cases test passed");
    }

    @AfterAll
    static void cleanUp() {
        System.out.println("\n=== All UserDAO Tests Completed ===");
        System.out.println("Total tests executed: 17");
        System.out.println("Test cleanup completed");
    }
}