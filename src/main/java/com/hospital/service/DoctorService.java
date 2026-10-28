package com.hospital.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.entity.Doctor;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.UserAccountRepository;

@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserAccountRepository accountRepository;

    public DoctorService(
            DoctorRepository doctorRepository,
            UserAccountRepository accountRepository) {
        this.doctorRepository = doctorRepository;
        this.accountRepository = accountRepository;
    }

    public Doctor addDoctor(Doctor doctor) {
        if (doctorRepository.existsByEmail(
                doctor.getEmail())) {
            throw new RuntimeException(
                "Doctor with email "
                + doctor.getEmail()
                + " already exists!");
        }
        if (doctorRepository.existsByPhone(
                doctor.getPhone())) {
            throw new RuntimeException(
                "Doctor with phone "
                + doctor.getPhone()
                + " already exists!");
        }
        doctor.setAvailable(true);
        return doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Doctor not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Doctor updateDoctor(
            Long id, Doctor updatedDoctor) {
        Doctor existing = getDoctorById(id);
        existing.setName(updatedDoctor.getName());
        existing.setSpecialization(
                updatedDoctor.getSpecialization());
        existing.setPhone(updatedDoctor.getPhone());
        existing.setQualification(
                updatedDoctor.getQualification());
        existing.setExperienceYears(
                updatedDoctor.getExperienceYears());
        existing.setConsultationFee(
                updatedDoctor.getConsultationFee());
        existing.setDepartment(
                updatedDoctor.getDepartment());
        return doctorRepository.save(existing);
    }

    public void deleteDoctor(Long id) {
        getDoctorById(id);
        // Their login goes too
        accountRepository.deleteByDoctorId(id);
        doctorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctorsBySpecialization(
            String specialization) {
        return doctorRepository
                .findBySpecialization(specialization);
    }

    @Transactional(readOnly = true)
    public List<Doctor> getAvailableDoctors() {
        return doctorRepository.findByAvailable(true);
    }

    @Transactional(readOnly = true)
    public List<Doctor> getAvailableDoctorsBySpecialization(
            String specialization) {
        return doctorRepository
                .findBySpecializationAndAvailable(
                        specialization, true);
    }

    @Transactional(readOnly = true)
    public List<Doctor> searchDoctorsByName(
            String name) {
        return doctorRepository
                .findByNameContainingIgnoreCase(name);
    }

    public Doctor toggleAvailability(Long id) {
        Doctor doctor = getDoctorById(id);
        doctor.setAvailable(
                !Boolean.TRUE.equals(doctor.getAvailable()));
        return doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public long getTotalDoctorCount() {
        return doctorRepository.count();
    }
}