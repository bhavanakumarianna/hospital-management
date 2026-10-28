// ══════════════════════════════════════════
// PACKAGE DECLARATION
// ══════════════════════════════════════════

// What is this?
// This file lives in the repository package.
// repository = the folder we just created.
// Full address of this file:
// src/main/java/com/hospital/repository//
package com.hospital.repository;


// ══════════════════════════════════════════
// IMPORT STATEMENTS
// ══════════════════════════════════════════

// Import our Patient class we created in Step 7
// We need to tell Java WHICH Patient class we mean
// because there could be many classes named Patient
// in different packages
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.entity.Patient;


// ══════════════════════════════════════════
// @Repository ANNOTATION
// ══════════════════════════════════════════

// What is @Repository?
// This annotation tells Spring Boot:
// "Hey Spring! This interface talks to database!"
// "Create an implementation for me automatically!"
// "Manage it as a Spring Bean!"
//
// What is a Spring Bean?
// A Spring Bean = an object that Spring Boot
// creates and manages for you.
// You never write: new PatientRepository()
// Spring Boot creates it and gives it to you
// wherever you need it.
// This is called DEPENDENCY INJECTION.



// ══════════════════════════════════════════
// INTERFACE DECLARATION
// ══════════════════════════════════════════

// What is an interface?
// An interface is like a JOB DESCRIPTION.
// It says WHAT needs to be done
// but not HOW to do it.
//
// Example:
// Job description says: "must be able to save data"
// The actual employee figures out HOW to save it.
//
// Here:
// PatientRepository interface says WHAT we need
// Spring Data JPA figures out HOW to do it
// and writes all the actual code behind the scenes!
//
// interface vs class:
// class = has actual code (HOW to do things)
// interface = just defines methods (WHAT to do)
// Spring Boot writes the actual HOW for us!

// What is extends JpaRepository<Patient, Long>?
// extends = PatientRepository INHERITS everything
//           from JpaRepository
// Like a child inheriting traits from parents!
//
// JpaRepository needs TWO things in < >:
// 1. Patient = WHICH entity/table we work with
//    (tells JPA: "this repository works with Patient objects")
// 2. Long = the DATA TYPE of the primary key (id)
//    (our Patient's id field is of type Long)
//
// By extending JpaRepository<Patient, Long>
// we get ALL these methods for FREE:
// ──────────────────────────────────────────
// save(patient)          → INSERT or UPDATE
// findById(id)           → SELECT WHERE id=?
// findAll()              → SELECT all patients
// deleteById(id)         → DELETE WHERE id=?
// existsById(id)         → check if exists
// count()                → COUNT all patients
// saveAll(list)          → INSERT multiple
// deleteAll()            → DELETE all patients
// ──────────────────────────────────────────
// ALL FREE! You write ZERO code for these!
public interface PatientRepository
        extends JpaRepository<Patient, Long> {


    // ════════════════════════════════════════
    // CUSTOM METHOD 1: Find by Email
    // ════════════════════════════════════════

    // What is this?
    // This finds a patient using their email address.
    // We need this when:
    // 1. Patient logs in with email
    // 2. Checking if email already exists
    //
    // How does Spring know what SQL to write?
    // MAGIC! Spring reads the METHOD NAME!
    // findBy = SELECT WHERE
    // Email = email column
    // Spring generates this SQL automatically:
    // SELECT * FROM patients WHERE email = ?
    //
    // What is Optional<Patient>?
    // Optional means: "maybe a patient, maybe not"
    // If patient with this email exists → returns the patient
    // If no patient with this email → returns empty Optional
    // This prevents NullPointerException errors!
    // (NullPointerException = trying to use something that is null/empty)
    Optional<Patient> findByEmail(String email);


    // ════════════════════════════════════════
    // CUSTOM METHOD 2: Check if Email Exists
    // ════════════════════════════════════════

    // What is this?
    // Checks if a patient with this email
    // already exists in the database.
    // We use this when registering a new patient:
    // "Does this email already belong to someone?"
    //
    // Spring reads method name and generates:
    // SELECT COUNT(*) FROM patients WHERE email = ?
    // If count > 0 → returns true (email exists)
    // If count = 0 → returns false (email free)
    //
    // boolean = can only be true or false
    // true = email exists in database
    // false = email does not exist
    boolean existsByEmail(String email);


    // ════════════════════════════════════════
    // CUSTOM METHOD 3: Find by Phone
    // ════════════════════════════════════════

    // What is this?
    // Finds a patient using their phone number.
    // Useful when patient calls the hospital
    // and receptionist searches by phone.
    //
    // Spring generates:
    // SELECT * FROM patients WHERE phone = ?
    Optional<Patient> findByPhone(String phone);


    // ════════════════════════════════════════
    // CUSTOM METHOD 4: Find All by City
    // ════════════════════════════════════════

    // What is this?
    // Gets ALL patients from a specific city.
    // Useful for admin reports:
    // "How many patients are from Atlanta?"
    //
    // Spring generates:
    // SELECT * FROM patients WHERE city = ?
    //
    // Returns List<Patient> because
    // many patients can be from same city!
    // List = ordered collection of items
    List<Patient> findByCity(String city);


    // ════════════════════════════════════════
    // CUSTOM METHOD 5: Find by Name Containing
    // ════════════════════════════════════════

    // What is this?
    // Searches patients whose name CONTAINS
    // the search text.
    // Like a search bar — type "Sara" and
    // it finds "Sarah", "Saraswati" etc.
    //
    // Spring generates:
    // SELECT * FROM patients
    // WHERE name LIKE '%searchText%'
    // % means "anything before or after"
    // So %Sara% finds Sarah, Saraswati, MySara
    //
    // IgnoreCase means:
    // "sara" finds "Sara", "SARA", "sArA"
    // Case does not matter!
    List<Patient> findByNameContainingIgnoreCase(String name);


    // ════════════════════════════════════════
    // CUSTOM METHOD 6: Find by Blood Group
    // ════════════════════════════════════════

    // What is this?
    // Gets ALL patients with a specific blood group.
    // Very useful in emergencies!
    // "We need O+ blood donors urgently"
    // → find all O+ patients immediately
    //
    // Spring generates:
    // SELECT * FROM patients WHERE blood_group = ?
    List<Patient> findByBloodGroup(String bloodGroup);


    // ════════════════════════════════════════
    // CUSTOM METHOD 7: Find by Gender
    // ════════════════════════════════════════

    // What is this?
    // Gets all patients of a specific gender.
    // Useful for medical reports and statistics.
    //
    // Spring generates:
    // SELECT * FROM patients WHERE gender = ?
    List<Patient> findByGender(String gender);


    // ════════════════════════════════════════
    // CUSTOM METHOD 8: Check if Phone Exists
    // ════════════════════════════════════════

    // What is this?
    // Checks if a phone number already belongs
    // to an existing patient.
    // Prevents duplicate registrations!
    //
    // Spring generates:
    // SELECT COUNT(*) FROM patients WHERE phone = ?
    boolean existsByPhone(String phone);
}