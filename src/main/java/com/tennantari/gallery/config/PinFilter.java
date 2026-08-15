package com.tennantari.gallery.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class PinFilter implements Filter {

    @Value("${gallery.access.pin}")
    private String accessPin;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        if (path.equals("/pin") || path.startsWith("/css/") || path.startsWith("/js/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean verified = session != null && Boolean.TRUE.equals(session.getAttribute("pinVerified"));

        if (verified) {
            chain.doFilter(request, response);
            return;
        }

        if (accessPin == null || accessPin.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        req.getSession(true).setAttribute("pinReturnUrl", path);
        res.sendRedirect("/pin");
    }
}
