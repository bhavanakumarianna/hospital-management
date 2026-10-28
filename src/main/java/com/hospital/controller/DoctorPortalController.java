package com.hospital.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.entity.Appointment;
import com.hospital.entity.Doctor;
import com.hospital.service.AccountService;
import com.hospital.service.AppointmentService;
import com.hospital.service.DoctorService;

// For signed-in DOCTORS only.
// A doctor sees and updates only their OWN schedule.
@RestController
@RequestMapping("/api/doctor-portal")
public class DoctorPortalController {

    private final AccountService accountService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    public DoctorPortalController(
            AccountService accountService,
            DoctorService doctorService,
            AppointmentService appointmentService) {
        this.accountService = accountService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
    }

    // GET /api/doctor-portal/profile
    @GetMapping("/profile")
    public ResponseEntity<Doctor> profile(Authentication auth) {
        return ResponseEntity.ok(
                doctorService.getDoctorById(doctorId(auth)));
    }

    // GET /api/doctor-portal/appointments
    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> appointments(
            Authentication auth) {
        return ResponseEntity.ok(appointmentService
                .getDoctorAppointments(doctorId(auth)));
    }

    // PUT /api/doctor-portal/appointments/1/complete?doctorNotes=...
    @PutMapping("/appointments/{id}/complete")
    public ResponseEntity<Appointment> complete(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam String doctorNotes) {
        checkOwnAppointment(auth, id);
        return ResponseEntity.ok(appointmentService
                .completeAppointment(id, doctorNotes));
    }

    // PUT /api/doctor-portal/appointments/1/cancel
    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<Appointment> cancel(
            Authentication auth, @PathVariable Long id) {
        checkOwnAppointment(auth, id);
        return ResponseEntity.ok(
                appointmentService.cancelAppointment(id));
    }

    // PUT /api/doctor-portal/availability → on/off
    @PutMapping("/availability")
    public ResponseEntity<Doctor> toggleAvailability(
            Authentication auth) {
        return ResponseEntity.ok(
                doctorService.toggleAvailability(doctorId(auth)));
    }

    private void checkOwnAppointment(
            Authentication auth, Long appointmentId) {
        Appointment appointment = appointmentService
                .getAppointmentById(appointmentId);
        if (!appointment.getDoctor().getId()
                .equals(doctorId(auth))) {
            throw new AccessDeniedException(
                    "This appointment is with another doctor");
        }
    }

    private Long doctorId(Authentication auth) {
        return accountService.currentAccount(auth).getDoctorId();
    }
}
