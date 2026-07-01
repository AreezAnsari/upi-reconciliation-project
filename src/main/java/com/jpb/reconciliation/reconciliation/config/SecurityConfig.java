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

                // Swagger
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()

                // Auth APIs
                .requestMatchers(new AntPathRequestMatcher("/auth/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/refresh-token")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/forgot-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/reset-password")).permitAll()

                // V2 Auth — login, refresh, logout, /me (all JWT-free login flows)
                .requestMatchers(new AntPathRequestMatcher("/api/v2/auth/**")).permitAll()

                // User APIs
                .requestMatchers(new AntPathRequestMatcher("/api/v2/user/create-user")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v2/user/create")).permitAll()

                // User Auth (onboarding + login flow — no JWT required)
                .requestMatchers(new AntPathRequestMatcher("/api/v1/user/auth/**")).permitAll()

                // Google OAuth
                .requestMatchers(new AntPathRequestMatcher("/authentication/app")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/google")).permitAll()

                // Kalinfotech APIs
                .requestMatchers(new AntPathRequestMatcher("/api/kalinfotech/**")).permitAll()

                // KalAdmin V2 Auth — create is public (no token needed for self-registration)
                .requestMatchers(new AntPathRequestMatcher("/api/v2/admin/auth/create")).permitAll()

                // H2
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()

                // Bank Admin APIs
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/verify-email")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/verify-credentials")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/set-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/direct-login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/forgot-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/verify-forgot-otp")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/reset-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/check-user-status")).permitAll()

                // Bank + Branch logo images + get-by-code — public (brand logos, no sensitive data)
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/logo/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/get-by-code/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/logo/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/get-by-code/**")).permitAll()

                // Bank uniqueness check APIs — non-critical, fail-open on frontend
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/check-email")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/bank/check-name")).permitAll()

                // Branch — public onboarding + uniqueness check APIs
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/verify-email")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/verify-credentials")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/set-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/direct-login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/forgot-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/verify-forgot-otp")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/reset-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/check-user-status")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/activate")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/check-email")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/test/api/v1/branch/check-name")).permitAll()

                // ✅ IMPORTANT FIX FOR OTP VERIFY
                .requestMatchers(new AntPathRequestMatcher("/api/otp/**")).permitAll()


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

        // OAuth Login
        http.oauth2Login(oauth -> {
            oauth.successHandler(handler);
        });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);

        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedOrigin("http://localhost:3000");

        configuration.addAllowedOrigin("http://13.48.46.135");
        configuration.addAllowedOrigin("http://13.48.46.135:8080");
        configuration.addAllowedOrigin("http://13.48.46.135:8081");

        configuration.addAllowedOrigin("https://jpbreconsit.jiopaymentsbank.com:8080");

        configuration.addAllowedOrigin("http://10.142.12.140:8080");
        configuration.addAllowedOrigin("http://192.168.1.103:8081");

        configuration.addAllowedOrigin("https://jio-recon.jiopaymentsbank.com:8080");
        configuration.addAllowedOrigin("https://jiorecon.jiopaymentsbank.com:8080");

        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}