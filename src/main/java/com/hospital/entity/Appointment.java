package com.hospital.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ════════════════════════════════════════
    // RELATIONSHIP: Many Appointments → One Patient
    // ════════════════════════════════════════
    // What is @ManyToOne?
    // Many appointments can belong to ONE patient.
    // Sarah can have appointment on Monday,
    // Wednesday, Friday — all linked to Sarah.
    //
    // fetch = FetchType.EAGER means:
    // Load patient data together with the appointment.
    // We need this because open-in-view=false closes
    // the database session before the JSON response
    // is written — LAZY data would fail to load then!
    @ManyToOne(fetch = FetchType.EAGER)

    // What is @JoinColumn?
    // This creates a foreign key column
    // in the appointments table.
    // name = "patient_id" means:
    // the column is called "patient_id"
    // It stores the id of the patient
    // who booked this appointment.
    //
    // nullable = false means:
    // every appointment MUST have a patient!
    @JoinColumn(name = "patient_id",
                nullable = false)
    private Patient patient;

    // ════════════════════════════════════════
    // RELATIONSHIP: Many Appointments → One Doctor
    // ════════════════════════════════════════
    // Same concept as patient above.
    // Many appointments can be with ONE doctor.
    // Dr. Smith can have many patients
    // in their appointment schedule.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id",
                nullable = false)
    private Doctor doctor;

    // Appointment date
    // Example: 2026-09-15
    @Column(nullable = false)
    private LocalDate appointmentDate;

    // Appointment time
    // Example: 10:30
    @Column(nullable = false)
    private LocalTime appointmentTime;

    // Why patient is visiting
    // Example: "Chest pain", "Regular checkup"
    @Column(length = 500)
    private String reason;

    // What is @Enumerated?
    // An Enum = a fixed set of allowed values.
    // AppointmentStatus can ONLY be one of:
    // SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    // Cannot be "maybe" or "unknown" etc.
    //
    // EnumType.STRING means:
    // Store the text "SCHEDULED" in database
    // Not the number 0,1,2,3
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    // Doctor's notes after the appointment
    // Only filled after appointment is done
    @Column(length = 1000)
    private String doctorNotes;

    // When this appointment record was created
    private LocalDate createdAt;

    // ════════════════════════════════════════
    // ENUM: AppointmentStatus
    // ════════════════════════════════════════
    // What is an Enum?
    // A special type that can only have
    // specific predefined values.
    // Like a dropdown with fixed options.
    //
    // SCHEDULED  = appointment is booked
    // COMPLETED  = patient was seen by doctor
    // CANCELLED  = appointment was cancelled
    // NO_SHOW    = patient didn't show up
    public enum AppointmentStatus {
        SCHEDULED,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }
}