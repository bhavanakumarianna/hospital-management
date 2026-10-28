package com.hospital.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.hospital.repository.UserAccountRepository;
import com.hospital.service.AccountService;

// Who can call which URLs.
//
// Public:   website pages, doctor list, free time slots
// PATIENT:  /api/me/**            (own appointments)
// DOCTOR:   /api/doctor-portal/** (own schedule)
// ADMIN:    every other /api/**   (staff portal)
@Configuration
public class SecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository contextRepository)
            throws Exception {
        http
            // The session cookie is SameSite=Lax (see
            // application.properties), so other sites
            // cannot send forged requests with it.
            .csrf(csrf -> csrf.disable())
            .securityContext(sc -> sc
                    .securityContextRepository(contextRepository))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/doctors", "/api/doctors/**",
                        "/api/patients/count",
                        "/api/appointments/booked-slots")
                    .permitAll()
                .requestMatchers("/api/me/**").hasRole("PATIENT")
                .requestMatchers("/api/doctor-portal/**")
                    .hasRole("DOCTOR")
                .requestMatchers("/api/**", "/actuator/**")
                    .hasRole("ADMIN")
                // Website pages, CSS, JS
                .anyRequest().permitAll())
            // Not signed in → 401 (no login popup/redirect)
            .exceptionHandling(e -> e.authenticationEntryPoint(
                    new HttpStatusEntryPoint(
                            HttpStatus.UNAUTHORIZED)))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler()));
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // How Spring finds a user when they sign in
    @Bean
    public UserDetailsService userDetailsService(
            UserAccountRepository accountRepository) {
        return email -> accountRepository
                .findByEmail(AccountService.normalizeEmail(email))
                .map(account -> User
                        .withUsername(account.getEmail())
                        .password(account.getPasswordHash())
                        .roles(account.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account for " + email));
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    // On startup: create the admin login from
    // ADMIN_EMAIL / ADMIN_PASSWORD if there is none yet
    @Bean
    public ApplicationRunner createAdminAccount(
            AccountService accountService,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password}") String password) {
        return args -> {
            if (accountService.ensureAdmin(email, password)) {
                log.info("Created admin account {}", email);
            } else if (password.isBlank()) {
                log.info("ADMIN_PASSWORD not set — "
                        + "no admin account created");
            }
        };
    }
}
