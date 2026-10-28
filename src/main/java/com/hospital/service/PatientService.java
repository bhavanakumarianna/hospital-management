package com.hospital.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.entity.Patient;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.UserAccountRepository;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserAccountRepository accountRepository;

    public PatientService(
            PatientRepository patientRepository,
            UserAccountRepository accountRepository) {
        this.patientRepository = patientRepository;
        this.accountRepository = accountRepository;
    }

    public Patient registerPatient(Patient patient) {
        if (patientRepository.existsByEmail(
                patient.getEmail())) {
            throw new RuntimeException(
                "Email already exists: "
                + patient.getEmail());
        }
        if (patientRepository.existsByPhone(
                patient.getPhone())) {
            throw new RuntimeException(
                "Phone already exists: "
                + patient.getPhone());
        }
        patient.setRegisteredOn(LocalDate.now());
        return patientRepository.save(patient);
    }

    @Transactional(readOnly = true)
    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Patient not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient updatePatient(
            Long id, Patient updatedPatient) {
        Patient existing = getPatientById(id);
        existing.setName(updatedPatient.getName());
        existing.setPhone(updatedPatient.getPhone());
        existing.setAge(updatedPatient.getAge());
        existing.setGender(updatedPatient.getGender());
        existing.setCity(updatedPatient.getCity());
        existing.setBloodGroup(
                updatedPatient.getBloodGroup());
        return patientRepository.save(existing);
    }

    public void deletePatient(Long id) {
        getPatientById(id);
        // Their login goes too
        accountRepository.deleteByPatientId(id);
        patientRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Patient> searchPatientsByName(
            String name) {
        return patientRepository
                .findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Patient> getPatientsByCity(
            String city) {
        return patientRepository.findByCity(city);
    }

    @Transactional(readOnly = true)
    public List<Patient> getPatientsByBloodGroup(
            String bloodGroup) {
        return patientRepository
                .findByBloodGroup(bloodGroup);
    }

    @Transactional(readOnly = true)
    public Patient getPatientByEmail(String email) {
        return patientRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Patient not found: " + email));
    }

    @Transactional(readOnly = true)
    public long getTotalPatientCount() {
        return patientRepository.count();
    }
}