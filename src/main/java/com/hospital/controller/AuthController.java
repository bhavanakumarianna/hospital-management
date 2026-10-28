package com.hospital.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.entity.Patient;
import com.hospital.entity.UserAccount;
import com.hospital.service.AccountService;
import com.hospital.service.DoctorService;
import com.hospital.service.PatientService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Sign in, sign up and "who am I".
// Sign out is POST /api/auth/logout (SecurityConfig).
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid") String email,
            @NotBlank(message = "Phone is required")
            @Pattern(regexp = "\\d{10,15}",
                    message = "Phone must be 10-15 digits") String phone,
            @NotBlank
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,
            @Min(0) @Max(150) Integer age,
            String gender,
            String city,
            String bloodGroup) {
    }

    // What the website needs to know about the signed-in user
    public record MeResponse(
            String email,
            String role,
            String name,
            Long patientId,
            Long doctorId) {
    }

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final AccountService accountService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            AccountService accountService,
            PatientService patientService,
            DoctorService doctorService) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.accountService = accountService;
        this.patientService = patientService;
        this.doctorService = doctorService;
    }

    // POST /api/auth/login {email, password}
    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication auth = signIn(body.email(),
                body.password(), request, response);
        return ResponseEntity.ok(
                describe(accountService.currentAccount(auth)));
    }

    // POST /api/auth/register — patients create an account
    @PostMapping("/register")
    public ResponseEntity<MeResponse> register(
            @Valid @RequestBody RegisterRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        Patient patient = new Patient();
        patient.setName(body.name().trim());
        patient.setEmail(body.email());
        patient.setPhone(body.phone());
        patient.setAge(body.age() == null ? 0 : body.age());
        patient.setGender(body.gender());
        patient.setCity(body.city());
        patient.setBloodGroup(body.bloodGroup());

        accountService.registerPatient(patient, body.password());

        // Sign them straight in
        Authentication auth = signIn(body.email(),
                body.password(), request, response);
        return new ResponseEntity<>(
                describe(accountService.currentAccount(auth)),
                HttpStatus.CREATED);
    }

    // GET /api/auth/me → 401 when not signed in
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(
            Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(describe(
                accountService.currentAccount(authentication)));
    }

    private Authentication signIn(
            String email, String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        AccountService.normalizeEmail(email),
                        password));

        // New session id on sign-in (prevents session fixation)
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return auth;
    }

    private MeResponse describe(UserAccount account) {
        String name = switch (account.getRole()) {
            case PATIENT -> patientService
                    .getPatientById(account.getPatientId()).getName();
            case DOCTOR -> doctorService
                    .getDoctorById(account.getDoctorId()).getName();
            case ADMIN -> "Hospital Admin";
        };
        return new MeResponse(account.getEmail(),
                account.getRole().name(), name,
                account.getPatientId(), account.getDoctorId());
    }
}
