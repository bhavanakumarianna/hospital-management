package com.hospital.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.entity.Doctor;
import com.hospital.entity.Patient;
import com.hospital.entity.UserAccount;
import com.hospital.entity.UserAccount.Role;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.UserAccountRepository;

@Service
@Transactional
public class AccountService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserAccountRepository accountRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            UserAccountRepository accountRepository,
            PatientRepository patientRepository,
            PatientService patientService,
            DoctorService doctorService,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.patientRepository = patientRepository;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.passwordEncoder = passwordEncoder;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

    // Patient signs up on the website:
    // creates the Patient record AND the login
    public UserAccount registerPatient(
            Patient patient, String password) {
        checkPassword(password);
        String email = normalizeEmail(patient.getEmail());

        if (accountRepository.existsByEmail(email)) {
            throw new RuntimeException(
                "An account with this email already exists. "
                + "Please sign in instead.");
        }
        // Record made by reception → staff set up the login,
        // so nobody can claim someone else's records.
        if (patientRepository.existsByEmail(email)) {
            throw new RuntimeException(
                "This email is already registered with the "
                + "hospital. Please ask reception to set up "
                + "your login.");
        }

        patient.setEmail(email);
        Patient saved = patientService.registerPatient(patient);

        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(Role.PATIENT);
        account.setPatientId(saved.getId());
        return accountRepository.save(account);
    }

    // Staff create or reset a doctor's login
    public UserAccount setDoctorLogin(
            Long doctorId, String password) {
        Doctor doctor = doctorService.getDoctorById(doctorId);
        UserAccount account = accountRepository
                .findByDoctorId(doctorId)
                .orElseGet(UserAccount::new);
        return saveLogin(account, doctor.getEmail(),
                password, Role.DOCTOR, null, doctorId);
    }

    // Staff create or reset a patient's login
    public UserAccount setPatientLogin(
            Long patientId, String password) {
        Patient patient = patientService.getPatientById(patientId);
        UserAccount account = accountRepository
                .findByPatientId(patientId)
                .orElseGet(UserAccount::new);
        return saveLogin(account, patient.getEmail(),
                password, Role.PATIENT, patientId, null);
    }

    // Creates the first admin from ADMIN_EMAIL/ADMIN_PASSWORD
    public boolean ensureAdmin(String email, String password) {
        if (password == null || password.isBlank()
                || accountRepository.existsByRole(Role.ADMIN)) {
            return false;
        }
        saveLogin(new UserAccount(), email, password,
                Role.ADMIN, null, null);
        return true;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    // The account of whoever is signed in
    @Transactional(readOnly = true)
    public UserAccount currentAccount(
            Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException(
                    "Not signed in");
        }
        return accountRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found"));
    }

    private UserAccount saveLogin(
            UserAccount account, String email,
            String password, Role role,
            Long patientId, Long doctorId) {
        checkPassword(password);
        String normalized = normalizeEmail(email);

        accountRepository.findByEmail(normalized)
                .filter(other -> !other.getId()
                        .equals(account.getId()))
                .ifPresent(other -> {
                    throw new RuntimeException(
                        "Another account already uses "
                        + normalized);
                });

        account.setEmail(normalized);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(role);
        account.setPatientId(patientId);
        account.setDoctorId(doctorId);
        return accountRepository.save(account);
    }

    private void checkPassword(String password) {
        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException(
                "Password must be at least "
                + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
