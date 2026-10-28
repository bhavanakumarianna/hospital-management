package com.hospital.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// A login for the website.
// Each account has ONE role:
// ADMIN   = hospital staff (staff portal)
// DOCTOR  = linked to a Doctor record
// PATIENT = linked to a Patient record
@Entity
@Table(name = "user_accounts")
@Data
@NoArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Users sign in with their email
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Never store plain passwords — only a BCrypt hash
    @JsonIgnore
    @ToString.Exclude
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Set for PATIENT accounts
    private Long patientId;

    // Set for DOCTOR accounts
    private Long doctorId;

    public enum Role {
        ADMIN,
        DOCTOR,
        PATIENT
    }
}
