package com.aarogya.dao;

import com.aarogya.model.User;
import com.aarogya.util.DBUtil;

import java.sql.*;
import java.util.UUID;

public class UserDAO {
    public User findByEmail(String email) throws SQLException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, email, password_hash, role, status FROM users WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    User user = new User();
                    user.setId(result.getInt("id"));
                    user.setName(result.getString("name"));
                    user.setEmail(result.getString("email"));
                    user.setPasswordHash(result.getString("password_hash"));
                    user.setRole(result.getString("role"));
                    user.setStatus(result.getString("status"));
                    return user;
                }
            }
        }
        return null;
    }

    public User createPatientAccount(String name, String email, String mobile, String passwordHash)
            throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users (name, email, password_hash, role, status) VALUES (?, ?, ?, 'PATIENT', 'Active')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, name);
                    statement.setString(2, email);
                    statement.setString(3, passwordHash);
                    statement.executeUpdate();
                    userId = generatedId(statement);
                }

                int patientRowId;
                String temporaryPatientId = "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO patients (patient_id, user_id, full_name, mobile, email) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, temporaryPatientId);
                    statement.setInt(2, userId);
                    statement.setString(3, name);
                    statement.setString(4, mobile);
                    statement.setString(5, email);
                    statement.executeUpdate();
                    patientRowId = generatedId(statement);
                }

                String patientId = "PAT-" + java.time.Year.now(
                        java.time.ZoneId.of("Asia/Kolkata")) + "-" + String.format("%04d", patientRowId);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE patients SET patient_id = ? WHERE id = ?")) {
                    statement.setString(1, patientId);
                    statement.setInt(2, patientRowId);
                    statement.executeUpdate();
                }

                connection.commit();
                User user = new User();
                user.setId(userId);
                user.setName(name);
                user.setEmail(email);
                user.setRole("PATIENT");
                user.setStatus("Active");
                return user;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                if (failure instanceof SQLException sqlFailure
                        && ("23505".equals(sqlFailure.getSQLState())
                        || "23000".equals(sqlFailure.getSQLState()))) {
                    throw new IllegalArgumentException("An account already exists for that email address.", sqlFailure);
                }
                throw failure;
            }
        }
    }

    private int generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("The database did not return the new record ID.");
            }
            return keys.getInt(1);
        }
    }
}
