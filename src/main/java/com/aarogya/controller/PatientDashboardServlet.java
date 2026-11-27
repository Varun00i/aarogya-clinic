package com.aarogya.controller;

import com.aarogya.model.User;
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
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/patient/dashboard")
public class PatientDashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession(false).getAttribute("user");
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement patientQuery = connection.prepareStatement(
                     "SELECT id, patient_id, full_name FROM patients WHERE user_id = ?")) {
            patientQuery.setInt(1, user.getId());
            try (ResultSet patient = patientQuery.executeQuery()) {
                if (!patient.next()) {
                    request.setAttribute("dbError",
                            "We could not find a patient profile for this account. Please contact the reception desk.");
                    request.getRequestDispatcher("/patient/dashboard.jsp").forward(request, response);
                    return;
                }

                int patientRowId = patient.getInt("id");
                request.setAttribute("patientName", patient.getString("full_name"));
                request.setAttribute("patientId", patient.getString("patient_id"));
                loadNextAppointment(connection, request, patientRowId);
            }
            request.setAttribute("notificationCount", count(connection,
                    "SELECT COUNT(*) FROM notifications WHERE user_id = ?", user.getId()));
        } catch (SQLException e) {
            getServletContext().log("Patient dashboard data could not be loaded.", e);
            request.setAttribute("dbError", "Patient dashboard data is temporarily unavailable.");
        }
        request.getRequestDispatcher("/patient/dashboard.jsp").forward(request, response);
    }

    private void loadNextAppointment(Connection connection, HttpServletRequest request, int patientId)
            throws SQLException {
        String sql = "SELECT a.id, a.appointment_id, a.appointment_date, a.appointment_time, a.status, "
                + "d.name AS doctor_name, dep.name AS department_name, t.token_number, t.status AS token_status, "
                + "t.department_id, t.created_at AS token_created "
                + "FROM appointments a JOIN doctors d ON d.id = a.doctor_id "
                + "JOIN departments dep ON dep.id = a.department_id "
                + "LEFT JOIN tokens t ON t.appointment_id = a.id "
                + "WHERE a.patient_id = ? AND a.appointment_date >= CURRENT_DATE "
                + "AND a.status NOT IN ('Cancelled', 'Completed') "
                + "ORDER BY a.appointment_date, a.appointment_time LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet appointment = statement.executeQuery()) {
                if (appointment.next()) {
                    Map<String, Object> nextAppointment = new LinkedHashMap<>();
                    nextAppointment.put("appointmentId", appointment.getString("appointment_id"));
                    nextAppointment.put("appointmentDate", appointment.getDate("appointment_date"));
                    nextAppointment.put("appointmentTime", appointment.getString("appointment_time"));
                    nextAppointment.put("status", appointment.getString("status"));
                    nextAppointment.put("doctorName", appointment.getString("doctor_name"));
                    nextAppointment.put("departmentName", appointment.getString("department_name"));
                    nextAppointment.put("tokenNumber", appointment.getString("token_number"));
                    nextAppointment.put("tokenStatus", appointment.getString("token_status"));
                    request.setAttribute("nextAppointment", nextAppointment);
                    String tokenNumber = appointment.getString("token_number");
                    if (tokenNumber != null
                            && appointment.getDate("appointment_date").toLocalDate()
                            .equals(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")))) {
                        int ahead = countPatientsAhead(connection,
                                appointment.getInt("department_id"),
                                appointment.getTimestamp("token_created"),
                                appointment.getInt("id"));
                        request.setAttribute("patientsAhead", ahead);
                        request.setAttribute("estimatedWaitMinutes", ahead * 7);
                    }
                }
            }
        }
    }

    private int countPatientsAhead(Connection connection, int departmentId,
                                   java.sql.Timestamp tokenCreated, int appointmentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tokens t JOIN appointments a ON a.id = t.appointment_id "
                + "WHERE t.department_id = ? AND t.status IN ('Waiting', 'Called') "
                + "AND a.appointment_date = CURRENT_DATE AND "
                + "(t.created_at < ? OR (t.created_at = ? AND a.id < ?))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, departmentId);
            statement.setTimestamp(2, tokenCreated);
            statement.setTimestamp(3, tokenCreated);
            statement.setInt(4, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private int count(Connection connection, String sql, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
