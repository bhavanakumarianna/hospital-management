package com.hospital;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Full flow test: real controllers + services
// + in-memory H2 database (no MySQL needed).
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bookCompleteAndCancelFlow() throws Exception {
        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Sarah Johnson",
                     "email":"sarah.api@email.com",
                     "phone":"9876543210","age":28}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(post("/api/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Dr. Smith",
                     "specialization":"Cardiology",
                     "email":"smith.api@hospital.com",
                     "phone":"9123456780"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(true));

        // Book → response includes patient + doctor
        mockMvc.perform(post("/api/appointments/book")
                .param("patientId", "1")
                .param("doctorId", "1")
                .param("date", "2026-12-15")
                .param("time", "10:30")
                .param("reason", "Checkup"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status")
                        .value("SCHEDULED"))
                .andExpect(jsonPath("$.patient.name")
                        .value("Sarah Johnson"))
                .andExpect(jsonPath("$.doctor.name")
                        .value("Dr. Smith"));

        // Same doctor, same slot → double booking
        mockMvc.perform(post("/api/appointments/book")
                .param("patientId", "1")
                .param("doctorId", "1")
                .param("date", "2026-12-15")
                .param("time", "10:30")
                .param("reason", "Again"))
                .andExpect(status().isBadRequest());

        // Website looks up returning patients by email
        mockMvc.perform(get("/api/patients/by-email")
                .param("email", "sarah.api@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(get("/api/patients/by-email")
                .param("email", "nobody@email.com"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/appointments/doctor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/appointments/1/complete")
                .param("doctorNotes", "Healthy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("COMPLETED"));

        // Completed appointments cannot be cancelled
        mockMvc.perform(put("/api/appointments/1/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void homePageServesWebsite() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/js/site.js"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/no-such-page"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingRecordsAndBadInput() throws Exception {
        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("NOT_FOUND"));

        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"","email":"not-an-email",
                     "phone":"12"}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("VALIDATION_FAILED"));
    }
}
