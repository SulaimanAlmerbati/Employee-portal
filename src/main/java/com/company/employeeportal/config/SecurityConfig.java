package com.company.employeeportal.config;

import com.company.employeeportal.security.CustomUserDetailsService;
import com.company.employeeportal.security.JwtAuthenticationFilter;
import com.company.employeeportal.security.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService, 
                         JwtAuthenticationFilter jwtAuthenticationFilter,
                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Disabled for stateless JWT API
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Enforce HTTPS in production environments
        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
        
        http.authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/logout", "/api/auth/validate").authenticated()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/login", "/", "/dashboard").permitAll()
                
                // Employee endpoints - accessible by all authenticated users
                .requestMatchers("/api/users/profile").hasAnyRole("EMPLOYEE", "HR", "IT_ADMIN", "FINANCE")
                .requestMatchers("/api/leaves/my-requests").hasAnyRole("EMPLOYEE", "HR", "IT_ADMIN", "FINANCE")
                .requestMatchers("/api/leaves/request").hasAnyRole("EMPLOYEE", "HR", "IT_ADMIN", "FINANCE")
                .requestMatchers("/api/payroll/my-payslips").hasAnyRole("EMPLOYEE", "HR", "IT_ADMIN", "FINANCE")
                .requestMatchers("/api/payroll/payslip/**").hasAnyRole("EMPLOYEE", "HR", "IT_ADMIN", "FINANCE")
                .requestMatchers("/api/announcements").authenticated()
                
                // HR endpoints - accessible by HR only
                .requestMatchers("/api/leaves/pending").hasRole("HR")
                .requestMatchers("/api/leaves/*/approve").hasRole("HR")
                .requestMatchers("/api/leaves/*/reject").hasRole("HR")
                .requestMatchers("/api/leaves/all").hasRole("HR")
                .requestMatchers(HttpMethod.POST, "/api/announcements").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/announcements/*").hasRole("HR")
                .requestMatchers(HttpMethod.DELETE, "/api/announcements/*").hasRole("HR")
                .requestMatchers("/api/announcements/*/reactivate").hasRole("HR")
                .requestMatchers("/api/announcements/admin/**").hasRole("HR")
                
                // IT Admin endpoints - accessible by IT Admin and HR
                .requestMatchers("/api/users").hasAnyRole("IT_ADMIN", "HR")
                .requestMatchers("/api/users/search").hasAnyRole("IT_ADMIN", "HR")
                .requestMatchers("/api/users/by-role/*").hasAnyRole("IT_ADMIN", "HR")
                .requestMatchers("/api/users/test").hasAnyRole("IT_ADMIN", "HR")
                .requestMatchers(HttpMethod.GET, "/api/users/*").hasAnyRole("IT_ADMIN", "HR")
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("IT_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/*").hasRole("IT_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("IT_ADMIN")
                .requestMatchers("/api/users/*/reactivate").hasRole("IT_ADMIN")
                .requestMatchers("/api/users/*/reset-password").hasRole("IT_ADMIN")
                
                // Finance endpoints - accessible by Finance only
                .requestMatchers("/api/payroll/users/*/payslips").hasRole("FINANCE")
                .requestMatchers("/api/payroll/create").hasRole("FINANCE")
                .requestMatchers("/api/payroll/*/update").hasRole("FINANCE")
                
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions().deny() // Prevent clickjacking attacks
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubDomains(true)
                    .preload(true)
                )
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow specific origins in production, wildcard for development
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin", 
            "Access-Control-Request-Method", 
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight response for 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}