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
import java.util.Set;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DOCTOR", "RECEPTIONIST", "PATIENT");
    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        try {
            User user = accountService.authenticate(email, password);
            if (user != null && user.getRole() != null
                    && ALLOWED_ROLES.contains(user.getRole().toUpperCase())) {
                user.setPasswordHash(null);
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                HttpSession session = request.getSession(true);
                session.setAttribute("user", user);
                session.setAttribute("role", user.getRole());
                redirectToDashboard(request, response, user.getRole());
                return;
            }
            request.setAttribute("error", "The email or password is incorrect. Please try again.");
        } catch (SQLException e) {
            getServletContext().log("Sign-in failed because the account service is unavailable.", e);
            request.setAttribute("error", "We could not sign you in right now. Please try again.");
        }
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    private void redirectToDashboard(HttpServletRequest request, HttpServletResponse response, String role)
            throws IOException {
        String rolePath = role.toLowerCase();
        if ("admin".equals(rolePath)) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        } else if ("patient".equals(rolePath)) {
            response.sendRedirect(request.getContextPath() + "/patient/dashboard");
        } else {
            response.sendRedirect(request.getContextPath() + "/" + rolePath + "/dashboard.jsp");
        }
    }
}
