package com.hospital;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hospital.entity.Patient;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.UserAccountRepository;
import com.hospital.service.PatientService;

// What is @ExtendWith(MockitoExtension.class)?
// Tells JUnit to use Mockito for this test.
// Mockito = a tool that creates FAKE objects.
// We don't need real database for testing!
// Mockito fakes the database responses.
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    // What is @Mock?
    // Creates a FAKE PatientRepository.
    // It does not talk to real MySQL.
    // We control what it returns in each test!
    // Like a fake storage room guy
    // who returns whatever we tell him to.
    @Mock
    private PatientRepository patientRepository;

    // Fake login storage (used when deleting)
    @Mock
    private UserAccountRepository accountRepository;

    // What is @InjectMocks?
    // Creates a REAL PatientService
    // but injects the FAKE repository into it.
    // So we test real service logic
    // without needing real database!
    @InjectMocks
    private PatientService patientService;

    // Test patient object we reuse
    private Patient testPatient;

    // What is @BeforeEach?
    // This method runs BEFORE every test.
    // Sets up fresh test data each time.
    @BeforeEach
    void setUp() {
        testPatient = new Patient();
        testPatient.setId(1L);
        testPatient.setName("Sarah Johnson");
        testPatient.setEmail("sarah@email.com");
        testPatient.setPhone("9876543210");
        testPatient.setAge(28);
        testPatient.setCity("Atlanta");
        testPatient.setBloodGroup("O+");
    }

    // ════════════════════════════════════════
    // TEST 1: Register patient successfully
    // ════════════════════════════════════════
    @Test
    void registerPatient_Success() {
        // ARRANGE — set up the fake responses
        // When repository checks email → return false
        // (email does not exist yet)
        when(patientRepository.existsByEmail(
                "sarah@email.com"))
                .thenReturn(false);

        // When repository checks phone → return false
        when(patientRepository.existsByPhone(
                "9876543210"))
                .thenReturn(false);

        // When repository saves → return our patient
        when(patientRepository.save(any(Patient.class)))
                .thenReturn(testPatient);

        // ACT — run the actual method
        Patient result =
                patientService.registerPatient(
                        testPatient);

        // ASSERT — check results are correct
        assertNotNull(result);
        assertEquals("Sarah Johnson", result.getName());
        assertEquals("sarah@email.com",
                result.getEmail());

        // Verify save was called exactly once
        verify(patientRepository, times(1))
                .save(any(Patient.class));
    }

    // ════════════════════════════════════════
    // TEST 2: Register fails — duplicate email
    // ════════════════════════════════════════
    @Test
    void registerPatient_DuplicateEmail_ThrowsException() {
        // ARRANGE
        // Email already exists in database
        when(patientRepository.existsByEmail(
                "sarah@email.com"))
                .thenReturn(true);

        // ACT + ASSERT
        // Expect RuntimeException to be thrown
        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> patientService
                                .registerPatient(
                                        testPatient));

        // Check error message is correct
        assertTrue(exception.getMessage()
                .contains("already exists"));

        // Verify save was NEVER called
        // (should not save duplicate!)
        verify(patientRepository, never())
                .save(any(Patient.class));
    }

    // ════════════════════════════════════════
    // TEST 3: Get patient by id — found
    // ════════════════════════════════════════
    @Test
    void getPatientById_Found() {
        // ARRANGE
        when(patientRepository.findById(1L))
                .thenReturn(
                        Optional.of(testPatient));

        // ACT
        Patient result =
                patientService.getPatientById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Sarah Johnson",
                result.getName());
    }

    // ════════════════════════════════════════
    // TEST 4: Get patient by id — not found
    // ════════════════════════════════════════
    @Test
    void getPatientById_NotFound_ThrowsException() {
        // ARRANGE
        when(patientRepository.findById(999L))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> patientService
                                .getPatientById(999L));

        assertTrue(exception.getMessage()
                .contains("999"));
    }

    // ════════════════════════════════════════
    // TEST 5: Get all patients
    // ════════════════════════════════════════
    @Test
    void getAllPatients_ReturnsList() {
        // ARRANGE
        Patient patient2 = new Patient();
        patient2.setId(2L);
        patient2.setName("John Doe");

        when(patientRepository.findAll())
                .thenReturn(Arrays.asList(
                        testPatient, patient2));

        // ACT
        List<Patient> result =
                patientService.getAllPatients();

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Sarah Johnson",
                result.get(0).getName());
        assertEquals("John Doe",
                result.get(1).getName());
    }

    // ════════════════════════════════════════
    // TEST 6: Delete patient — success
    // ════════════════════════════════════════
    @Test
    void deletePatient_Success() {
        // ARRANGE
        when(patientRepository.findById(1L))
                .thenReturn(
                        Optional.of(testPatient));

        // doNothing = when deleteById called
        // do nothing (fake delete)
        doNothing().when(patientRepository)
                .deleteById(1L);

        // ACT
        patientService.deletePatient(1L);

        // ASSERT
        // Verify deleteById was called once
        verify(patientRepository, times(1))
                .deleteById(1L);
    }

    // ════════════════════════════════════════
    // TEST 7: Count patients
    // ════════════════════════════════════════
    @Test
    void getTotalPatientCount_ReturnsCount() {
        // ARRANGE
        when(patientRepository.count())
                .thenReturn(5L);

        // ACT
        long count =
                patientService.getTotalPatientCount();

        // ASSERT
        assertEquals(5L, count);
    }
}