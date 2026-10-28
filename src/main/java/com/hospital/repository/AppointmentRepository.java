package com.hospital.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.entity.Appointment;
import com.hospital.entity.Appointment.AppointmentStatus;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    // Find all appointments for a patient
    // SQL: SELECT * FROM appointments
    //      WHERE patient_id = ?
    List<Appointment> findByPatientId(Long patientId);

    // Find all appointments for a doctor
    // SQL: SELECT * FROM appointments
    //      WHERE doctor_id = ?
    List<Appointment> findByDoctorId(Long doctorId);

    // Find appointments by status
    // SQL: SELECT * FROM appointments
    //      WHERE status = ?
    List<Appointment> findByStatus(
            AppointmentStatus status);

    // Find appointments by date
    // SQL: SELECT * FROM appointments
    //      WHERE appointment_date = ?
    List<Appointment> findByAppointmentDate(
            LocalDate date);

    // Find doctor's appointments on specific date
    // SQL: SELECT * FROM appointments
    //      WHERE doctor_id = ?
    //      AND appointment_date = ?
    List<Appointment> findByDoctorIdAndAppointmentDate(
            Long doctorId, LocalDate date);

    // Find patient's appointments by status
    List<Appointment> findByPatientIdAndStatus(
            Long patientId, AppointmentStatus status);

    // Check if doctor has appointment at date+time
    // Used to prevent double booking!
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(
            Long doctorId,
            LocalDate date,
            java.time.LocalTime time);
}