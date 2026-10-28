package com.hospital.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.entity.UserAccount;
import com.hospital.entity.UserAccount.Role;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    // Emails are stored lower-case, so lookups match
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Optional<UserAccount> findByDoctorId(Long doctorId);

    Optional<UserAccount> findByPatientId(Long patientId);

    // Remove the login when its doctor/patient is deleted
    void deleteByDoctorId(Long doctorId);

    void deleteByPatientId(Long patientId);
}
