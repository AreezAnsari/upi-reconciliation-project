package com.jpb.reconciliation.reconciliation.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * Skip JWT filter for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
            throws ServletException {

        String path = request.getServletPath();
        return path.startsWith("/api/kalinfotech/")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/admin-login")
                || path.startsWith("/auth/google")
                || path.startsWith("/auth/refresh-token")
                || path.startsWith("/auth/forgot-password")
                || path.startsWith("/auth/reset-password")
                || path.startsWith("/authentication/app")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/h2-console")
                || path.startsWith("/api/v1/user/create-user")

                // Institution APIs
                || path.startsWith("/test/api/v1/institution/verify-credentials")
                || path.startsWith("/test/api/v1/institution/set-password")
                || path.startsWith("/test/api/v1/institution/login")
                || path.startsWith("/test/api/v1/institution/verify-email")
                || path.startsWith("/test/api/v1/institution/check-user-status")
                || path.startsWith("/test/api/v1/institution/forgot-password")
                || path.startsWith("/test/api/v1/institution/reset-password")
                || path.startsWith("/test/api/v1/institution/check-email")
                || path.startsWith("/test/api/v1/institution/check-name")

                // SubInstitution APIs
                || path.startsWith("/test/api/v1/subinstitution/verify-credentials")
                || path.startsWith("/test/api/v1/subinstitution/set-password")
                || path.startsWith("/test/api/v1/subinstitution/login")

                // OTP APIs
                || path.startsWith("/api/otp/verify");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        logger.debug("FILTER RUNNING FOR: {}", requestURI);

        String requestHeader = request.getHeader("Authorization");

        String username = null;
        String token = null;
        String jti = null;

        /**
         * Extract JWT token
         */
        if (requestHeader != null && requestHeader.startsWith("Bearer ")) {

            token = requestHeader.substring(7);

            try {

                username = jwtHelper.getUsernameFromToken(token);
                jti = jwtHelper.getJtiFromToken(token);

            } catch (ExpiredJwtException e) {

                logger.warn(
                        "JWT expired for request [{}]: {}",
                        requestURI,
                        e.getMessage()
                );

            } catch (MalformedJwtException e) {

                logger.warn(
                        "Malformed JWT for request [{}]: {}",
                        requestURI,
                        e.getMessage()
                );

            } catch (SignatureException e) {

                logger.warn(
                        "Invalid JWT signature for request [{}]: {}",
                        requestURI,
                        e.getMessage()
                );

            } catch (UnsupportedJwtException e) {

                logger.warn(
                        "Unsupported JWT for request [{}]: {}",
                        requestURI,
                        e.getMessage()
                );

            } catch (IllegalArgumentException e) {

                logger.warn(
                        "JWT claims empty for request [{}]: {}",
                        requestURI,
                        e.getMessage()
                );

            } catch (Exception e) {

                logger.warn(
                        "JWT parse error for request [{}]: {}",
                        requestURI,
                        e.getMessage()
                );
            }

        } else {

            logger.debug(
                    "No Bearer token found in request [{}]",
                    requestURI
            );
        }

        /**
         * Authenticate only if username exists
         */
        if (username != null
                && jti != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                boolean isTokenBlacklisted =
                        tokenBlacklistService.isTokenBlacklisted(jti);

                boolean validateToken =
                        jwtHelper.validateToken(
                                token,
                                username,
                                isTokenBlacklisted
                        );

                if (validateToken) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);

                    logger.debug(
                            "JWT authenticated user [{}] for request [{}]",
                            username,
                            requestURI
                    );

                } else {

                    logger.warn(
                            "JWT validation failed for user [{}]",
                            username
                    );
                }

            } catch (Exception e) {

                logger.warn(
                        "Could not authenticate user [{}] for request [{}]: {}",
                        username,
                        requestURI,
                        e.getMessage()
                );
            }
        }

        /**
         * Always continue filter chain
         */
        filterChain.doFilter(request, response);
    }
}