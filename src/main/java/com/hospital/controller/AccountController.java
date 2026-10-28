package com.hospital.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.entity.UserAccount;
import com.hospital.service.AccountService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

// ADMIN only: give doctors and patients a login,
// or reset a forgotten password.
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    public record PasswordRequest(@NotBlank String password) {
    }

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // GET /api/accounts (password hashes are never sent)
    @GetMapping
    public ResponseEntity<List<UserAccount>> getAll() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    // PUT /api/accounts/doctor/1 {password}
    @PutMapping("/doctor/{doctorId}")
    public ResponseEntity<UserAccount> setDoctorLogin(
            @PathVariable Long doctorId,
            @Valid @RequestBody PasswordRequest body) {
        return ResponseEntity.ok(accountService
                .setDoctorLogin(doctorId, body.password()));
    }

    // PUT /api/accounts/patient/1 {password}
    @PutMapping("/patient/{patientId}")
    public ResponseEntity<UserAccount> setPatientLogin(
            @PathVariable Long patientId,
            @Valid @RequestBody PasswordRequest body) {
        return ResponseEntity.ok(accountService
                .setPatientLogin(patientId, body.password()));
    }
}
