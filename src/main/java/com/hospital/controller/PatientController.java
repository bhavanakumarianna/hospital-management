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

import com.hospital.entity.Patient;
import com.hospital.service.PatientService;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin("*")
public class PatientController {

    private final PatientService patientService;

    public PatientController(
            PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<Patient> registerPatient(
            @Valid @RequestBody Patient patient) {
        Patient savedPatient =
                patientService.registerPatient(patient);
        return new ResponseEntity<>(
                savedPatient, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(
            @PathVariable Long id) {
        Patient patient =
                patientService.getPatientById(id);
        return new ResponseEntity<>(
                patient, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        List<Patient> patients =
                patientService.getAllPatients();
        return new ResponseEntity<>(
                patients, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody Patient patient) {
        Patient updatedPatient =
                patientService.updatePatient(id, patient);
        return new ResponseEntity<>(
                updatedPatient, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePatient(
            @PathVariable Long id) {
        patientService.deletePatient(id);
        return new ResponseEntity<>(
                "Patient deleted successfully!",
                HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Patient>> searchPatients(
            @RequestParam String name) {
        List<Patient> patients =
                patientService.searchPatientsByName(name);
        return new ResponseEntity<>(
                patients, HttpStatus.OK);
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<Patient>> getPatientsByCity(
            @PathVariable String city) {
        List<Patient> patients =
                patientService.getPatientsByCity(city);
        return new ResponseEntity<>(
                patients, HttpStatus.OK);
    }

    @GetMapping("/blood-group/{bloodGroup}")
    public ResponseEntity<List<Patient>> getByBloodGroup(
            @PathVariable String bloodGroup) {
        List<Patient> patients =
                patientService
                        .getPatientsByBloodGroup(bloodGroup);
        return new ResponseEntity<>(
                patients, HttpStatus.OK);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCount() {
        long count =
                patientService.getTotalPatientCount();
        return new ResponseEntity<>(
                count, HttpStatus.OK);
    }
}