package com.jpb.reconciliation.reconciliation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.jpb.reconciliation.reconciliation.security.JwtAuthenticationEntryPoint;
import com.jpb.reconciliation.reconciliation.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint point;

    @Autowired
    private JwtAuthenticationFilter filter;

    @Autowired
    OAuthAuthenticationSuccessHandler handler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            .authorizeHttpRequests(auth -> auth

                // ─────────────────────────────────────────────────────────
                // SWAGGER
                // ─────────────────────────────────────────────────────────

                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()

                // ─────────────────────────────────────────────────────────
                // AUTH APIs
                // ─────────────────────────────────────────────────────────

                .requestMatchers(new AntPathRequestMatcher("/auth/admin-login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/refresh-token")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/forgot-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/reset-password")).permitAll()

                // ─────────────────────────────────────────────────────────
                // USER APIs
                // ─────────────────────────────────────────────────────────

                .requestMatchers(new AntPathRequestMatcher("/api/v1/user/create-user")).permitAll()

                // ─────────────────────────────────────────────────────────
                // GOOGLE OAUTH
                // ─────────────────────────────────────────────────────────

                .requestMatchers(new AntPathRequestMatcher("/authentication/app")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/google")).permitAll()

                // ─────────────────────────────────────────────────────────
                // KALINFOTECH APIs
                // ─────────────────────────────────────────────────────────

                .requestMatchers(new AntPathRequestMatcher("/api/kalinfotech/**")).permitAll()

                // ─────────────────────────────────────────────────────────
                // H2 CONSOLE
                // ─────────────────────────────────────────────────────────

                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()

                // ─────────────────────────────────────────────────────────
                // OTP APIs
                // ─────────────────────────────────────────────────────────

                .requestMatchers(new AntPathRequestMatcher("/api/otp/**")).permitAll()

                // ═════════════════════════════════════════════════════════
                // SUPER USER AUTH APIs
                // ═════════════════════════════════════════════════════════

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/verify-email")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/verify-credentials")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/set-password")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/login")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/forgot-password")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/reset-password")
                ).permitAll()

                // ═════════════════════════════════════════════════════════
                // INSTITUTION APIs
                // ═════════════════════════════════════════════════════════

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/create")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/get-all")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/get/**")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/get-by-status")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/update/**")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/update-status/**")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/change-status/**")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/delete/**")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/upload-logo/**")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/check-name")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/check-email")
                ).permitAll()

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/institution/export/**")
                ).permitAll()

                // ═════════════════════════════════════════════════════════
                // SUB INSTITUTION APIs
                // ═════════════════════════════════════════════════════════

                .requestMatchers(
                    new AntPathRequestMatcher("/test/api/v1/subinstitution/**")
                ).permitAll()
                .requestMatchers(
                	    new AntPathRequestMatcher("/test/api/v1/institution/check-user-status")
                	).permitAll()
                // ─────────────────────────────────────────────────────────
                // EVERYTHING ELSE SECURED
                // ─────────────────────────────────────────────────────────

                .anyRequest().authenticated()
            )

            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(point)
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .httpBasic(Customizer.withDefaults())

            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)

            .headers(headers ->
                headers.frameOptions(frameOptions ->
                    frameOptions.sameOrigin()
                )
            );

        // ─────────────────────────────────────────────────────────
        // GOOGLE OAUTH LOGIN
        // ─────────────────────────────────────────────────────────

        http.oauth2Login(oauth -> {
            oauth.successHandler(handler);
        });

        return http.build();
    }

    // ─────────────────────────────────────────────────────────────
    // CORS CONFIGURATION
    // ─────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);

        // LOCALHOST
        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedOrigin("http://localhost:3000");

        // SERVER IPs
        configuration.addAllowedOrigin("http://13.48.46.135");
        configuration.addAllowedOrigin("http://13.48.46.135:8080");
        configuration.addAllowedOrigin("http://13.48.46.135:8081");

        // INTERNAL IPs
        configuration.addAllowedOrigin("http://10.142.12.140:8080");
        configuration.addAllowedOrigin("http://192.168.1.103:8081");

        // JIO DOMAINS
        configuration.addAllowedOrigin("https://jpbreconsit.jiopaymentsbank.com:8080");
        configuration.addAllowedOrigin("https://jio-recon.jiopaymentsbank.com:8080");
        configuration.addAllowedOrigin("https://jiorecon.jiopaymentsbank.com:8080");

        // HEADERS
        configuration.addAllowedHeader("*");

        // METHODS
        configuration.addAllowedMethod("*");

        // CACHE
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}