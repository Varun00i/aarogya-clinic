package com.aarogya.service;

import com.aarogya.dao.UserDAO;
import com.aarogya.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

public class AccountService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INDIAN_MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");

    private final UserDAO userDAO;

    public AccountService() {
        this(new UserDAO());
    }

    AccountService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User authenticate(String email, String password) throws SQLException {
        if (email == null || password == null || email.isBlank() || password.isEmpty()) {
            return null;
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (user == null || user.getPasswordHash() == null
                || !"Active".equalsIgnoreCase(user.getStatus())) {
            return null;
        }
        try {
            return BCrypt.checkpw(password, user.getPasswordHash()) ? user : null;
        } catch (IllegalArgumentException invalidHash) {
            throw new SQLException("The stored password hash is invalid for this account.", invalidHash);
        }
    }

    public User registerPatient(String name, String email, String mobile, String password, String confirmation)
            throws SQLException {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedMobile = mobile == null ? "" : mobile.trim();

        if (normalizedName.length() < 2 || normalizedName.length() > 100) {
            throw new IllegalArgumentException("Enter your full name (2 to 100 characters).");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches() || normalizedEmail.length() > 150) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!INDIAN_MOBILE_PATTERN.matcher(normalizedMobile).matches()) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number.");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("Choose a password between 8 and 72 characters.");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("The passwords do not match.");
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        return userDAO.createPatientAccount(normalizedName, normalizedEmail, normalizedMobile, passwordHash);
    }
}
