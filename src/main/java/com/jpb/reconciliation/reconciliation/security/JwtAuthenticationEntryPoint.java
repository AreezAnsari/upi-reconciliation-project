package com.jpb.reconciliation.reconciliation.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        // Do NOT return 401 for KalInfotech public endpoints.
        // These are permitAll() but JwtAuthenticationFilter may still
        // reach this entry point if no authentication is set in context.
        String path = request.getServletPath();
        if (path.startsWith("/api/kalinfotech/")) {
            // Let the request pass through to the controller
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PrintWriter writer = response.getWriter();
        writer.println("unathorized !!");
    }
}
