package com.hospital.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.entity.Appointment;
import com.hospital.entity.Appointment.AppointmentStatus;
import com.hospital.entity.Doctor;
import com.hospital.entity.Patient;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PatientRepository;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository
            appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // Book a new appointment
    public Appointment bookAppointment(
            Long patientId,
            Long doctorId,
            LocalDate date,
            java.time.LocalTime time,
            String reason) {

        // Rule 1: Find the patient
        // If not found → error thrown
        Patient patient = patientRepository
                .findById(patientId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Patient not found: "
                        + patientId));

        // Rule 2: Find the doctor
        Doctor doctor = doctorRepository
                .findById(doctorId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Doctor not found: "
                        + doctorId));

        // Rule 3: Check doctor is available
        if (!Boolean.TRUE.equals(doctor.getAvailable())) {
            throw new RuntimeException(
                "Doctor is not available!");
        }

        // Rule 4: Check no double booking
        // Doctor cannot have two appointments
        // at same date AND same time!
        boolean alreadyBooked =
                appointmentRepository
                .existsByDoctorIdAndAppointmentDateAndAppointmentTime(
                        doctorId, date, time);

        if (alreadyBooked) {
            throw new RuntimeException(
                "Doctor already has appointment "
                + "at this date and time!");
        }

        // Create and save appointment
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(time);
        appointment.setReason(reason);
        appointment.setStatus(
                AppointmentStatus.SCHEDULED);
        appointment.setCreatedAt(LocalDate.now());

        return appointmentRepository
                .save(appointment);
    }

    // Get appointment by id
    @Transactional(readOnly = true)
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Appointment not found: "
                        + id));
    }

    // Get all appointments
    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Get appointments for a patient
    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(
            Long patientId) {
        return appointmentRepository
                .findByPatientId(patientId);
    }

    // Get appointments for a doctor
    @Transactional(readOnly = true)
    public List<Appointment> getDoctorAppointments(
            Long doctorId) {
        return appointmentRepository
                .findByDoctorId(doctorId);
    }

    // Times already taken for a doctor on a date
    @Transactional(readOnly = true)
    public List<java.time.LocalTime> getBookedSlots(
            Long doctorId, LocalDate date) {
        return appointmentRepository
                .findByDoctorIdAndAppointmentDate(
                        doctorId, date)
                .stream()
                .filter(a -> a.getStatus()
                        == AppointmentStatus.SCHEDULED)
                .map(Appointment::getAppointmentTime)
                .sorted()
                .toList();
    }

    // Get appointments by date
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByDate(
            LocalDate date) {
        return appointmentRepository
                .findByAppointmentDate(date);
    }

    // Complete an appointment
    public Appointment completeAppointment(
            Long id, String doctorNotes) {
        Appointment appointment =
                getAppointmentById(id);

        // Can only complete SCHEDULED appointments
        if (appointment.getStatus() !=
                AppointmentStatus.SCHEDULED) {
            throw new RuntimeException(
                "Can only complete SCHEDULED "
                + "appointments!");
        }

        appointment.setStatus(
                AppointmentStatus.COMPLETED);
        appointment.setDoctorNotes(doctorNotes);

        return appointmentRepository
                .save(appointment);
    }

    // Cancel an appointment
    public Appointment cancelAppointment(Long id) {
        Appointment appointment =
                getAppointmentById(id);

        if (appointment.getStatus() ==
                AppointmentStatus.COMPLETED) {
            throw new RuntimeException(
                "Cannot cancel a completed "
                + "appointment!");
        }

        appointment.setStatus(
                AppointmentStatus.CANCELLED);

        return appointmentRepository
                .save(appointment);
    }

    // Count total appointments
    @Transactional(readOnly = true)
    public long getTotalAppointmentCount() {
        return appointmentRepository.count();
    }
}