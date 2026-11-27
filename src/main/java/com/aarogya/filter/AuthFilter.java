package com.aarogya.filter;

import com.aarogya.model.User;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@WebFilter(urlPatterns = {"/admin/*", "/doctor/*", "/receptionist/*", "/patient/*"})
public class AuthFilter implements Filter {
    private static final Set<String> ALLOWED_ROLES =
            Set.of("admin", "doctor", "receptionist", "patient");

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User user)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        String role = user.getRole() == null ? "" : user.getRole().toLowerCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            session.invalidate();
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        String protectedPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String requestedRole = protectedPath.substring(1, protectedPath.indexOf('/', 1));
        if (!role.equals(requestedRole)) {
            String redirect = "admin".equals(role) ? "/admin/dashboard"
                    : "patient".equals(role) ? "/patient/dashboard"
                    : "/" + role + "/dashboard.jsp";
            httpResponse.sendRedirect(httpRequest.getContextPath() + redirect);
            return;
        }

        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        chain.doFilter(request, response);
    }
}
