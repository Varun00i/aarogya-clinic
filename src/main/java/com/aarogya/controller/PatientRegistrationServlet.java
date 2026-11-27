package com.aarogya.controller;

import com.aarogya.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@WebServlet("/receptionist/register")
public class PatientRegistrationServlet extends HttpServlet {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final Set<String> GENDERS = Set.of("Male", "Female", "Other");
    private static final Set<String> BLOOD_GROUPS = Set.of("", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/receptionist/register-patient.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = trim(request.getParameter("fullName"));
        String mobile = trim(request.getParameter("mobile"));
        String email = trim(request.getParameter("email"));
        String dobValue = trim(request.getParameter("dob"));
        String pin = trim(request.getParameter("pin"));

        if (name.length() < 2 || name.length() > 100) {
            reject(request, response, "Enter the patient's full name.");
            return;
        }
        if (!MOBILE_PATTERN.matcher(mobile).matches()) {
            reject(request, response, "Enter a valid 10-digit Indian mobile number.");
            return;
        }
        if (!email.isEmpty() && (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            reject(request, response, "Enter a valid email address.");
            return;
        }
        if (!PIN_PATTERN.matcher(pin).matches()) {
            reject(request, response, "Enter a valid six-digit PIN code.");
            return;
        }
        String gender = trim(request.getParameter("gender"));
        String address = trim(request.getParameter("address"));
        String city = trim(request.getParameter("city"));
        String state = trim(request.getParameter("state"));
        String bloodGroup = trim(request.getParameter("bloodGroup"));
        if (!GENDERS.contains(gender) || address.isEmpty() || address.length() > 255
                || city.isEmpty() || city.length() > 100 || state.isEmpty() || state.length() > 100
                || !BLOOD_GROUPS.contains(bloodGroup)) {
            reject(request, response, "Check the patient's address, gender and blood group details.");
            return;
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(dobValue);
            if (dateOfBirth.isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
                reject(request, response, "Date of birth cannot be in the future.");
                return;
            }
        } catch (DateTimeParseException e) {
            reject(request, response, "Enter a valid date of birth.");
            return;
        }

        String sql = "INSERT INTO patients (patient_id, full_name, guardian_name, dob, gender, mobile, email, "
                + "address, city, state, pin_code, blood_group) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int patientRowId;
                String temporaryId = "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, temporaryId);
                    statement.setString(2, name);
                    statement.setString(3, trim(request.getParameter("guardianName")));
                    statement.setDate(4, java.sql.Date.valueOf(dateOfBirth));
                    statement.setString(5, gender);
                    statement.setString(6, mobile);
                    statement.setString(7, email.isEmpty() ? null : email);
                    statement.setString(8, address);
                    statement.setString(9, city);
                    statement.setString(10, state);
                    statement.setString(11, pin);
                    statement.setString(12, bloodGroup.isEmpty() ? null : bloodGroup);
                    statement.executeUpdate();
                    patientRowId = generatedId(statement);
                }

                String patientId = "PAT-" + LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear()
                        + "-" + String.format("%04d", patientRowId);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE patients SET patient_id = ? WHERE id = ?")) {
                    statement.setString(1, patientId);
                    statement.setInt(2, patientRowId);
                    statement.executeUpdate();
                }
                connection.commit();
                request.getSession().setAttribute("success",
                        "Patient " + name + " registered successfully. Patient ID: " + patientId);
                response.sendRedirect(request.getContextPath() + "/receptionist/register");
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        } catch (SQLException e) {
            getServletContext().log("Receptionist patient registration failed.", e);
            reject(request, response, "Patient registration could not be completed. Please check the details and retry.");
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        doGet(request, response);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private int generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("The database did not return the new patient ID.");
            }
            return keys.getInt(1);
        }
    }
}
