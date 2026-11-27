package com.aarogya.service;

import com.aarogya.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountServiceTest {
    private static AccountService accountService;

    @BeforeClass
    public static void configureDatabase() {
        System.setProperty("db.url", "jdbc:h2:mem:account-service-test;DB_CLOSE_DELAY=-1;MODE=MySQL");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
        accountService = new AccountService();
    }

    @Test
    public void patientCanRegisterAndSignInWithHashedPassword() throws Exception {
        User registered = accountService.registerPatient(
                "Test Patient", "registration@example.in", "9876543210", "carefulPass1", "carefulPass1");

        assertEquals("PATIENT", registered.getRole());
        assertNull(registered.getPasswordHash());
        assertNotNull(accountService.authenticate("REGISTRATION@example.in", "carefulPass1"));
        assertNull(accountService.authenticate("registration@example.in", "incorrectPass"));
    }

    @Test
    public void duplicateEmailIsRejectedWithoutCreatingAnotherPatient() throws Exception {
        accountService.registerPatient(
                "First Patient", "duplicate@example.in", "9123456780", "carefulPass1", "carefulPass1");
        try {
            accountService.registerPatient(
                    "Second Patient", "duplicate@example.in", "9123456781", "carefulPass1", "carefulPass1");
            fail("Expected the duplicate email to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("already exists"));
        }
    }

    @Test
    public void invalidRegistrationDetailsAreRejected() throws Exception {
        try {
            accountService.registerPatient(
                    "New Patient", "new.patient@example.in", "123", "short", "different");
            fail("Expected invalid registration details to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("mobile"));
        }
    }

    @Test
    public void seededDemoAccountUsesTheDisplayedPassword() throws Exception {
        assertNotNull(accountService.authenticate("patient@aarogya.in", "password"));
        assertNull(accountService.authenticate("patient@aarogya.in", "not-the-password"));
    }
}
