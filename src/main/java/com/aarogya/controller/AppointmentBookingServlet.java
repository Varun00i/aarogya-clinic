package com.aarogya.controller;

import com.aarogya.model.User;
import com.aarogya.service.AppointmentService;
import com.aarogya.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/patient/book-appointment")
public class AppointmentBookingServlet extends HttpServlet {
    private final AppointmentService appointmentService = new AppointmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        forwardBookingPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession(false).getAttribute("user");
        try {
            int departmentId = Integer.parseInt(request.getParameter("departmentId"));
            int doctorId = Integer.parseInt(request.getParameter("doctorId"));
            LocalDate appointmentDate = LocalDate.parse(request.getParameter("appointmentDate"));
            AppointmentService.BookingConfirmation confirmation = appointmentService.book(
                    user.getId(), doctorId, departmentId, appointmentDate, request.getParameter("appointmentTime"));

            HttpSession session = request.getSession();
            session.setAttribute("bookingAppointmentId", confirmation.appointmentId());
            session.setAttribute("bookingTokenNumber", confirmation.tokenNumber());
            response.sendRedirect(request.getContextPath() + "/patient/book-appointment");
        } catch (NumberFormatException | DateTimeParseException e) {
            request.setAttribute("error", "Choose a department, doctor and valid appointment date.");
            forwardBookingPage(request, response);
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            forwardBookingPage(request, response);
        } catch (SQLException e) {
            getServletContext().log("Appointment booking failed.", e);
            request.setAttribute("error", "We could not book this appointment. Please try another available time.");
            forwardBookingPage(request, response);
        }
    }

    private void forwardBookingPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            loadDoctorsAndDepartments(request);
            request.getRequestDispatcher("/patient/book-appointment.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Appointment options could not be loaded.", e);
            request.setAttribute("dbError", "Doctor and department information is temporarily unavailable.");
            request.getRequestDispatcher("/patient/book-appointment.jsp").forward(request, response);
        }
    }

    private void loadDoctorsAndDepartments(HttpServletRequest request) throws SQLException {
        request.setAttribute("today", java.time.LocalDate.now(
                java.time.ZoneId.of("Asia/Kolkata")).toString());
        List<Map<String, Object>> departments = new ArrayList<>();
        List<Map<String, Object>> doctors = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement departmentQuery = connection.prepareStatement(
                     "SELECT id, name, code FROM departments WHERE status = 'Active' ORDER BY name");
             ResultSet departmentResults = departmentQuery.executeQuery()) {
            while (departmentResults.next()) {
                Map<String, Object> department = new LinkedHashMap<>();
                department.put("id", departmentResults.getInt("id"));
                department.put("name", departmentResults.getString("name"));
                department.put("code", departmentResults.getString("code"));
                departments.add(department);
            }
        }
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement doctorQuery = connection.prepareStatement(
                     "SELECT d.id, d.name, d.department_id, d.consultation_fee "
                             + "FROM doctors d JOIN departments dep ON dep.id = d.department_id "
                             + "WHERE d.status = 'Available' AND dep.status = 'Active' ORDER BY d.name");
             ResultSet doctorResults = doctorQuery.executeQuery()) {
            while (doctorResults.next()) {
                Map<String, Object> doctor = new LinkedHashMap<>();
                doctor.put("id", doctorResults.getInt("id"));
                doctor.put("name", doctorResults.getString("name"));
                doctor.put("departmentId", doctorResults.getInt("department_id"));
                doctor.put("fee", doctorResults.getBigDecimal("consultation_fee"));
                doctors.add(doctor);
            }
        }
        request.setAttribute("departments", departments);
        request.setAttribute("doctors", doctors);
    }
}
