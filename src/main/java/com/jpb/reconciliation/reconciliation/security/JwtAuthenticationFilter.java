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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private Logger logger = LoggerFactory.getLogger(OncePerRequestFilter.class);

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired 
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestHeader = request.getHeader("Authorization");
        String username = null;
        String token = null;
        String jti = null; 

        if (requestHeader != null && requestHeader.startsWith("Bearer")) {
            token = requestHeader.substring(7);
            try {
                username = this.jwtHelper.getUsernameFromToken(token);
                jti = this.jwtHelper.getJtiFromToken(token); 
            } catch (IllegalArgumentException e) {
                logger.info("Illegal Argument while fetching the username or JTI !!", e);
            } catch (ExpiredJwtException e) {
                logger.info("Given jwt token is expired !!", e);
            } catch (MalformedJwtException e) {
                logger.info("Some change has been done in token !! Invalid Token", e);
            } catch (Exception e) {
                logger.error("An unexpected error occurred during token processing", e);
            }

        } else {
            logger.info("Invalid Header Value !! Token either missing or not starting with Bearer.");
        }

        if (username != null && jti != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            boolean isTokenBlacklisted = tokenBlacklistService.isTokenBlacklisted(jti);

            Boolean validateToken = this.jwtHelper.validateToken(token, username, isTokenBlacklisted);

            if (validateToken) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                logger.info("Validation fails !! Token might be invalid, expired, or blacklisted.");
            }
        }

        filterChain.doFilter(request, response);
    }
}