package com.hospital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.entity.Doctor;

// Extends JpaRepository<Doctor, Long>
// Doctor = which entity/table
// Long = type of primary key (id)
// Gets ALL free methods automatically!
public interface DoctorRepository
        extends JpaRepository<Doctor, Long> {

    // Find doctor by email
    // SQL: SELECT * FROM doctors WHERE email = ?
    Optional<Doctor> findByEmail(String email);

    // Check if email exists
    // SQL: SELECT COUNT(*) FROM doctors WHERE email = ?
    boolean existsByEmail(String email);

    // Check if phone exists
    boolean existsByPhone(String phone);

    // Find all doctors by specialization
    // Example: find all "Cardiologist" doctors
    // SQL: SELECT * FROM doctors WHERE specialization = ?
    List<Doctor> findBySpecialization(
            String specialization);

    // Find all available doctors
    // SQL: SELECT * FROM doctors WHERE available = true
    List<Doctor> findByAvailable(boolean available);

    // Find doctors by department
    // SQL: SELECT * FROM doctors WHERE department = ?
    List<Doctor> findByDepartment(String department);

    // Search doctors by name
    // SQL: SELECT * FROM doctors WHERE name LIKE '%?%'
    List<Doctor> findByNameContainingIgnoreCase(
            String name);

    // Find available doctors by specialization
    // SQL: SELECT * FROM doctors
    //      WHERE specialization = ?
    //      AND available = true
    List<Doctor> findBySpecializationAndAvailable(
            String specialization, boolean available);
}