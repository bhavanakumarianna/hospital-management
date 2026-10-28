package com.hospital.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.entity.Appointment;
import com.hospital.service.AppointmentService;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin("*")
public class AppointmentController {

    private final AppointmentService
            appointmentService;

    public AppointmentController(
            AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // API 1: Book appointment
    // POST /api/appointments/book
    // ?patientId=1&doctorId=1
    // &date=2026-09-15&time=10:30&reason=checkup
    @PostMapping("/book")
    public ResponseEntity<Appointment> bookAppointment(
            @RequestParam Long patientId,
            @RequestParam Long doctorId,
            @RequestParam
            @DateTimeFormat(iso =
                DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam
            @DateTimeFormat(iso =
                DateTimeFormat.ISO.TIME)
            LocalTime time,
            @RequestParam String reason) {

        Appointment appointment =
                appointmentService.bookAppointment(
                        patientId, doctorId,
                        date, time, reason);

        return new ResponseEntity<>(
                appointment, HttpStatus.CREATED);
    }

    // API 2: Get appointment by id
    // GET /api/appointments/1
    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getById(
            @PathVariable Long id) {
        Appointment appointment =
                appointmentService
                        .getAppointmentById(id);
        return new ResponseEntity<>(
                appointment, HttpStatus.OK);
    }

    // API 3: Get all appointments
    // GET /api/appointments
    @GetMapping
    public ResponseEntity<List<Appointment>>
            getAllAppointments() {
        List<Appointment> appointments =
                appointmentService.getAllAppointments();
        return new ResponseEntity<>(
                appointments, HttpStatus.OK);
    }

    // API 4: Get patient appointments
    // GET /api/appointments/patient/1
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>>
            getPatientAppointments(
            @PathVariable Long patientId) {
        List<Appointment> appointments =
                appointmentService
                        .getPatientAppointments(
                                patientId);
        return new ResponseEntity<>(
                appointments, HttpStatus.OK);
    }

    // API 5: Get doctor appointments
    // GET /api/appointments/doctor/1
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>>
            getDoctorAppointments(
            @PathVariable Long doctorId) {
        List<Appointment> appointments =
                appointmentService
                        .getDoctorAppointments(
                                doctorId);
        return new ResponseEntity<>(
                appointments, HttpStatus.OK);
    }

    // API 6: Get appointments by date
    // GET /api/appointments/date/2026-09-15
    @GetMapping("/date/{date}")
    public ResponseEntity<List<Appointment>>
            getByDate(
            @PathVariable
            @DateTimeFormat(iso =
                DateTimeFormat.ISO.DATE)
            LocalDate date) {
        List<Appointment> appointments =
                appointmentService
                        .getAppointmentsByDate(date);
        return new ResponseEntity<>(
                appointments, HttpStatus.OK);
    }

    // API 7: Complete appointment
    // PUT /api/appointments/1/complete
    // ?doctorNotes=Patient is healthy
    @PutMapping("/{id}/complete")
    public ResponseEntity<Appointment>
            completeAppointment(
            @PathVariable Long id,
            @RequestParam String doctorNotes) {
        Appointment appointment =
                appointmentService
                        .completeAppointment(
                                id, doctorNotes);
        return new ResponseEntity<>(
                appointment, HttpStatus.OK);
    }

    // API 8: Cancel appointment
    // PUT /api/appointments/1/cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Appointment>
            cancelAppointment(
            @PathVariable Long id) {
        Appointment appointment =
                appointmentService
                        .cancelAppointment(id);
        return new ResponseEntity<>(
                appointment, HttpStatus.OK);
    }

    // API 9: Get total count
    // GET /api/appointments/count
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCount() {
        long count = appointmentService
                .getTotalAppointmentCount();
        return new ResponseEntity<>(
                count, HttpStatus.OK);
    }
}