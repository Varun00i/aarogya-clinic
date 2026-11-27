package com.aarogya.controller;

import com.aarogya.service.AppointmentService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;

@WebServlet("/patient/appointment-availability")
public class AppointmentAvailabilityServlet extends HttpServlet {
    private final AppointmentService appointmentService = new AppointmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        try {
            int doctorId = Integer.parseInt(request.getParameter("doctorId"));
            LocalDate appointmentDate = LocalDate.parse(request.getParameter("appointmentDate"));
            Set<String> unavailableTimes = appointmentService.getUnavailableTimes(doctorId, appointmentDate);
            response.getWriter().write(toJson(unavailableTimes));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Choose a doctor and valid appointment date.\"}");
        } catch (SQLException e) {
            getServletContext().log("Appointment availability could not be checked.", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"error\":\"Availability is temporarily unavailable.\"}");
        }
    }

    private String toJson(Set<String> unavailableTimes) {
        StringBuilder json = new StringBuilder("{\"unavailable\":[");
        boolean first = true;
        for (String time : unavailableTimes) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(time.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            first = false;
        }
        return json.append("]}").toString();
    }
}
