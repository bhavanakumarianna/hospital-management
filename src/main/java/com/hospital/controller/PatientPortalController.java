package com.hospital.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.entity.Appointment;
import com.hospital.entity.Patient;
import com.hospital.service.AccountService;
import com.hospital.service.AppointmentService;
import com.hospital.service.PatientService;

// For signed-in PATIENTS only.
// Everything here works on the patient's OWN data.
@RestController
@RequestMapping("/api/me")
public class PatientPortalController {

    private final AccountService accountService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientPortalController(
            AccountService accountService,
            PatientService patientService,
            AppointmentService appointmentService) {
        this.accountService = accountService;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    // GET /api/me/profile
    @GetMapping("/profile")
    public ResponseEntity<Patient> profile(Authentication auth) {
        return ResponseEntity.ok(
                patientService.getPatientById(patientId(auth)));
    }

    // GET /api/me/appointments
    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> appointments(
            Authentication auth) {
        return ResponseEntity.ok(appointmentService
                .getPatientAppointments(patientId(auth)));
    }

    // POST /api/me/appointments?doctorId=1&date=...&time=...&reason=...
    @PostMapping("/appointments")
    public ResponseEntity<Appointment> book(
            Authentication auth,
            @RequestParam Long doctorId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime time,
            @RequestParam String reason) {
        Appointment appointment = appointmentService.bookAppointment(
                patientId(auth), doctorId, date, time, reason);
        return new ResponseEntity<>(appointment, HttpStatus.CREATED);
    }

    // PUT /api/me/appointments/1/cancel
    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<Appointment> cancel(
            Authentication auth, @PathVariable Long id) {
        Appointment appointment =
                appointmentService.getAppointmentById(id);
        if (!appointment.getPatient().getId()
                .equals(patientId(auth))) {
            throw new AccessDeniedException(
                    "This is not your appointment");
        }
        return ResponseEntity.ok(
                appointmentService.cancelAppointment(id));
    }

    private Long patientId(Authentication auth) {
        return accountService.currentAccount(auth).getPatientId();
    }
}
