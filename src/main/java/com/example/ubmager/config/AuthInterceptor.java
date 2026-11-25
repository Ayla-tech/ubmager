package com.example.ubmager.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

// Halaman user wajib login sebagai user, halaman /admin wajib login sebagai admin
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        boolean halamanAdmin = request.getRequestURI().startsWith("/admin");
        String kunci = halamanAdmin ? "idAdmin" : "idUser";
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(kunci) != null) {
            return true;
        }
        response.sendRedirect(halamanAdmin ? "/admin/login" : "/login");
        return false;
    }
}
