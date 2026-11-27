package com.aarogya.controller;

import com.aarogya.model.User;
import com.aarogya.service.AccountService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/register")
public class RegistrationServlet extends HttpServlet {
    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof User user) {
            String role = user.getRole() == null ? "" : user.getRole().toLowerCase(java.util.Locale.ROOT);
            String target = "admin".equals(role) ? "/admin/dashboard"
                    : "patient".equals(role) ? "/patient/dashboard" : "/" + role + "/dashboard.jsp";
            response.sendRedirect(request.getContextPath() + target);
            return;
        }
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User user = accountService.registerPatient(
                    request.getParameter("fullName"),
                    request.getParameter("email"),
                    request.getParameter("mobile"),
                    request.getParameter("password"),
                    request.getParameter("confirmPassword"));

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("role", user.getRole());
            session.setAttribute("success", "Your patient account has been created.");
            response.sendRedirect(request.getContextPath() + "/patient/dashboard");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Patient account registration failed.", e);
            request.setAttribute("error", "We could not create your account right now. Please try again.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }
}
