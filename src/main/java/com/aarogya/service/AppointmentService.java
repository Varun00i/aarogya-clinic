package com.aarogya.service;

import com.aarogya.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AppointmentService {
    private static final Set<String> AVAILABLE_TIMES =
            Set.of("09:30 AM", "10:30 AM", "01:00 PM", "02:30 PM", "05:00 PM");
    private static final BigDecimal REGISTRATION_FEE = BigDecimal.valueOf(100);

    public BookingConfirmation book(int userId, int doctorId, int departmentId,
                                    LocalDate appointmentDate, String appointmentTime) throws SQLException {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        if (appointmentDate == null || appointmentDate.isBefore(today)) {
            throw new IllegalArgumentException("Choose today or a future appointment date.");
        }
        if (appointmentTime == null || !AVAILABLE_TIMES.contains(appointmentTime)) {
            throw new IllegalArgumentException("Choose one of the available appointment times.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int patientId = findPatientId(connection, userId);
                DoctorDetails doctor = findAvailableDoctor(connection, doctorId, departmentId);
                String appointmentId = insertAppointment(
                        connection, patientId, doctorId, departmentId, appointmentDate, appointmentTime);
                String tokenNumber = insertToken(connection, appointmentId, departmentId, doctor.code);
                createPayment(connection, appointmentId, doctor.fee);
                createNotification(connection, userId, appointmentId, tokenNumber);
                connection.commit();
                return new BookingConfirmation(appointmentId, tokenNumber);
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                if (failure instanceof SQLException sqlFailure
                        && ("23505".equals(sqlFailure.getSQLState())
                        || "23000".equals(sqlFailure.getSQLState()))) {
                    throw new IllegalArgumentException(
                            "That doctor’s time is no longer available. Please choose another time.", sqlFailure);
                }
                throw failure;
            }
        }
    }

    public Set<String> getUnavailableTimes(int doctorId, LocalDate appointmentDate) throws SQLException {
        if (doctorId < 1 || appointmentDate == null) {
            throw new IllegalArgumentException("Choose a doctor and appointment date.");
        }
        Set<String> unavailableTimes = new HashSet<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT appointment_time FROM appointments WHERE doctor_id = ? AND appointment_date = ?")) {
            statement.setInt(1, doctorId);
            statement.setDate(2, java.sql.Date.valueOf(appointmentDate));
            try (ResultSet appointments = statement.executeQuery()) {
                while (appointments.next()) {
                    unavailableTimes.add(appointments.getString("appointment_time"));
                }
            }
        }
        return Set.copyOf(unavailableTimes);
    }

    private int findPatientId(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM patients WHERE user_id = ? AND status = 'Active'")) {
            statement.setInt(1, userId);
            try (ResultSet patient = statement.executeQuery()) {
                if (!patient.next()) {
                    throw new IllegalArgumentException("Your patient profile could not be found. Contact reception.");
                }
                return patient.getInt(1);
            }
        }
    }

    private DoctorDetails findAvailableDoctor(Connection connection, int doctorId, int departmentId)
            throws SQLException {
        String sql = "SELECT d.consultation_fee, dep.code FROM doctors d "
                + "JOIN departments dep ON dep.id = d.department_id "
                + "WHERE d.id = ? AND d.department_id = ? AND d.status = 'Available' AND dep.status = 'Active'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            statement.setInt(2, departmentId);
            try (ResultSet doctor = statement.executeQuery()) {
                if (!doctor.next()) {
                    throw new IllegalArgumentException("Choose an available doctor in the selected department.");
                }
                return new DoctorDetails(doctor.getString("code"), doctor.getBigDecimal("consultation_fee"));
            }
        }
    }

    private String insertAppointment(Connection connection, int patientId, int doctorId, int departmentId,
                                     LocalDate date, String time) throws SQLException {
        String temporaryId = "TMP-A-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        int appointmentRowId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO appointments (appointment_id, patient_id, doctor_id, department_id, "
                        + "appointment_date, appointment_time, status) VALUES (?, ?, ?, ?, ?, ?, 'Scheduled')",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, temporaryId);
            statement.setInt(2, patientId);
            statement.setInt(3, doctorId);
            statement.setInt(4, departmentId);
            statement.setDate(5, java.sql.Date.valueOf(date));
            statement.setString(6, time);
            statement.executeUpdate();
            appointmentRowId = generatedId(statement);
        }

        String appointmentId = "APT-" + java.time.Year.now(ZoneId.of("Asia/Kolkata"))
                + "-" + String.format("%04d", appointmentRowId);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE appointments SET appointment_id = ? WHERE id = ?")) {
            statement.setString(1, appointmentId);
            statement.setInt(2, appointmentRowId);
            statement.executeUpdate();
        }
        return appointmentId;
    }

    private String insertToken(Connection connection, String appointmentId, int departmentId, String departmentCode)
            throws SQLException {
        int appointmentRowId;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM appointments WHERE appointment_id = ?")) {
            statement.setString(1, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The new appointment could not be found.");
                }
                appointmentRowId = result.getInt(1);
            }
        }

        String temporaryToken = "TMP-T-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        int tokenId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tokens (token_number, appointment_id, department_id, status) "
                        + "VALUES (?, ?, ?, 'Waiting')", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, temporaryToken);
            statement.setInt(2, appointmentRowId);
            statement.setInt(3, departmentId);
            statement.executeUpdate();
            tokenId = generatedId(statement);
        }

        String tokenNumber = departmentCode + "-" + String.format("%03d", tokenId);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tokens SET token_number = ? WHERE id = ?")) {
            statement.setString(1, tokenNumber);
            statement.setInt(2, tokenId);
            statement.executeUpdate();
        }
        return tokenNumber;
    }

    private void createPayment(Connection connection, String appointmentId, BigDecimal consultationFee)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM appointments WHERE appointment_id = ?")) {
            statement.setString(1, appointmentId);
            try (ResultSet appointment = statement.executeQuery()) {
                if (!appointment.next()) {
                    throw new SQLException("The new appointment could not be found for payment.");
                }
                try (PreparedStatement payment = connection.prepareStatement(
                        "INSERT INTO payments (appointment_id, amount, status, payment_method) "
                                + "VALUES (?, ?, 'Pending', 'Counter')")) {
                    payment.setInt(1, appointment.getInt(1));
                    payment.setBigDecimal(2, REGISTRATION_FEE.add(consultationFee));
                    payment.executeUpdate();
                }
            }
        }
    }

    private void createNotification(Connection connection, int userId, String appointmentId, String tokenNumber)
            throws SQLException {
        String message = "Appointment " + appointmentId + " is confirmed. Your token is " + tokenNumber + ".";
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO notifications (user_id, message) VALUES (?, ?)")) {
            statement.setInt(1, userId);
            statement.setString(2, message);
            statement.executeUpdate();
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

    private record DoctorDetails(String code, BigDecimal fee) {
    }

    public record BookingConfirmation(String appointmentId, String tokenNumber) {
    }
}
