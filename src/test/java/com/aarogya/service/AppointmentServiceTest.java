package com.aarogya.service;

import com.aarogya.dao.UserDAO;
import com.aarogya.model.User;
import com.aarogya.util.DBUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.*;

public class AppointmentServiceTest {
    @BeforeClass
    public static void configureDatabase() {
        System.setProperty("db.url", "jdbc:h2:mem:appointment-service-test;DB_CLOSE_DELAY=-1;MODE=MySQL");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
    }

    @Test
    public void bookingCreatesAppointmentTokenPaymentAndNotification() throws Exception {
        User patient = new UserDAO().findByEmail("patient@aarogya.in");
        AppointmentService service = new AppointmentService();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        AppointmentService.BookingConfirmation confirmation =
                service.book(patient.getId(), 1, 1, today, "02:30 PM");

        assertTrue(confirmation.appointmentId().startsWith("APT-"));
        assertTrue(confirmation.tokenNumber().startsWith("GM-"));
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT a.status, t.status AS token_status, p.status AS payment_status "
                             + "FROM appointments a JOIN tokens t ON t.appointment_id = a.id "
                             + "JOIN payments p ON p.appointment_id = a.id WHERE a.appointment_id = ?")) {
            statement.setString(1, confirmation.appointmentId());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("Scheduled", result.getString("status"));
                assertEquals("Waiting", result.getString("token_status"));
                assertEquals("Pending", result.getString("payment_status"));
            }
        }
    }

    @Test
    public void duplicateDoctorTimeSlotIsRejected() throws Exception {
        User patient = new UserDAO().findByEmail("patient@aarogya.in");
        AppointmentService service = new AppointmentService();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        service.book(patient.getId(), 1, 1, date, "05:00 PM");

        try {
            service.book(patient.getId(), 1, 1, date, "05:00 PM");
            fail("Expected the duplicate doctor time slot to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("no longer available"));
        }
    }

    @Test
    public void anotherPatientCannotBookAnOccupiedDoctorTime() throws Exception {
        User firstPatient = new UserDAO().findByEmail("patient@aarogya.in");
        User secondPatient = new AccountService().registerPatient(
                "Second Test Patient", "second.booking@example.in", "9123456780",
                "carefulPass1", "carefulPass1");
        AppointmentService service = new AppointmentService();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(2);
        service.book(firstPatient.getId(), 1, 1, date, "02:30 PM");

        assertTrue(service.getUnavailableTimes(1, date).contains("02:30 PM"));
        try {
            service.book(secondPatient.getId(), 1, 1, date, "02:30 PM");
            fail("Expected the time slot to be unavailable to a different patient.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("no longer available"));
        }
    }
}
