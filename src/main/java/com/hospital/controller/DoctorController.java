package com.hospital.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.entity.Doctor;
import com.hospital.service.DoctorService;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin("*")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(
            DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // API 1: Add new doctor
    // POST http://localhost:8080/api/doctors
    @PostMapping
    public ResponseEntity<Doctor> addDoctor(
            @Valid @RequestBody Doctor doctor) {
        Doctor savedDoctor =
                doctorService.addDoctor(doctor);
        return new ResponseEntity<>(
                savedDoctor, HttpStatus.CREATED);
    }

    // API 2: Get doctor by id
    // GET http://localhost:8080/api/doctors/1
    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorById(
            @PathVariable Long id) {
        Doctor doctor =
                doctorService.getDoctorById(id);
        return new ResponseEntity<>(
                doctor, HttpStatus.OK);
    }

    // API 3: Get all doctors
    // GET http://localhost:8080/api/doctors
    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        List<Doctor> doctors =
                doctorService.getAllDoctors();
        return new ResponseEntity<>(
                doctors, HttpStatus.OK);
    }

    // API 4: Update doctor
    // PUT http://localhost:8080/api/doctors/1
    @PutMapping("/{id}")
    public ResponseEntity<Doctor> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody Doctor doctor) {
        Doctor updatedDoctor =
                doctorService.updateDoctor(id, doctor);
        return new ResponseEntity<>(
                updatedDoctor, HttpStatus.OK);
    }

    // API 5: Delete doctor
    // DELETE http://localhost:8080/api/doctors/1
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDoctor(
            @PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return new ResponseEntity<>(
                "Doctor deleted successfully!",
                HttpStatus.OK);
    }

    // API 6: Get by specialization
    // GET /api/doctors/specialization/Cardiology
    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<Doctor>> getBySpecialization(
            @PathVariable String specialization) {
        List<Doctor> doctors =
                doctorService
                    .getDoctorsBySpecialization(
                            specialization);
        return new ResponseEntity<>(
                doctors, HttpStatus.OK);
    }

    // API 7: Get available doctors
    // GET http://localhost:8080/api/doctors/available
    @GetMapping("/available")
    public ResponseEntity<List<Doctor>> getAvailableDoctors() {
        List<Doctor> doctors =
                doctorService.getAvailableDoctors();
        return new ResponseEntity<>(
                doctors, HttpStatus.OK);
    }

    // API 8: Search doctors by name
    // GET /api/doctors/search?name=smith
    @GetMapping("/search")
    public ResponseEntity<List<Doctor>> searchDoctors(
            @RequestParam String name) {
        List<Doctor> doctors =
                doctorService.searchDoctorsByName(name);
        return new ResponseEntity<>(
                doctors, HttpStatus.OK);
    }

    // API 9: Toggle doctor availability
    // PUT /api/doctors/1/toggle-availability
    @PutMapping("/{id}/toggle-availability")
    public ResponseEntity<Doctor> toggleAvailability(
            @PathVariable Long id) {
        Doctor doctor =
                doctorService.toggleAvailability(id);
        return new ResponseEntity<>(
                doctor, HttpStatus.OK);
    }

    // API 10: Get total doctor count
    // GET http://localhost:8080/api/doctors/count
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCount() {
        long count =
                doctorService.getTotalDoctorCount();
        return new ResponseEntity<>(
                count, HttpStatus.OK);
    }
}