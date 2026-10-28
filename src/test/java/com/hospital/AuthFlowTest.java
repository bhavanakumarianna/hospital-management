package com.hospital;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

// Real sign-in flow: admin, doctor and patient
// each see only what their role allows.
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession login(String email, String password)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email
                        + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    private MockHttpSession register(String name, String email,
            String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"email\":\""
                        + email + "\",\"phone\":\"" + phone
                        + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    @Test
    void publicPagesOpenAndStaffApisNeedLogin() throws Exception {
        mockMvc.perform(get("/api/doctors")).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test.com\","
                        + "\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }

    @Test
    void adminDoctorAndPatientFlow() throws Exception {
        // Admin (created on startup) adds a doctor + login
        MockHttpSession admin = login("admin@test.com", "admin-test-pass");
        mockMvc.perform(get("/api/auth/me").session(admin))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        MvcResult created = mockMvc.perform(post("/api/doctors")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Dr. Auth","specialization":"Neurology",
                     "email":"dr.auth@hospital.com","phone":"9000000001"}
                    """))
                .andExpect(status().isCreated())
                .andReturn();
        int doctorId = JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/accounts/doctor/" + doctorId)
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"doctor123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Two patients sign up
        MockHttpSession alice = register("Alice", "alice@test.com", "9000000002");
        MockHttpSession bob = register("Bob", "bob@test.com", "9000000003");

        // Patients cannot use staff APIs
        mockMvc.perform(get("/api/patients").session(alice))
                .andExpect(status().isForbidden());

        // Alice books; the slot shows as taken publicly
        MvcResult booked = mockMvc.perform(post("/api/me/appointments")
                .session(alice)
                .param("doctorId", String.valueOf(doctorId))
                .param("date", "2026-12-20")
                .param("time", "09:30")
                .param("reason", "Headache"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patient.name").value("Alice"))
                .andReturn();
        int apptId = JsonPath.read(
                booked.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/appointments/booked-slots")
                .param("doctorId", String.valueOf(doctorId))
                .param("date", "2026-12-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("09:30:00"));

        // Bob sees none of Alice's appointments and can't cancel hers
        mockMvc.perform(get("/api/me/appointments").session(bob))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(put("/api/me/appointments/" + apptId + "/cancel")
                .session(bob))
                .andExpect(status().isForbidden());

        // The doctor sees it and completes it
        MockHttpSession doctor = login("DR.AUTH@hospital.com", "doctor123");
        mockMvc.perform(get("/api/doctor-portal/appointments").session(doctor))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].patient.name").value("Alice"));
        mockMvc.perform(put("/api/doctor-portal/appointments/" + apptId
                        + "/complete")
                .session(doctor)
                .param("doctorNotes", "Rest and fluids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Alice sees the doctor's notes
        mockMvc.perform(get("/api/me/appointments").session(alice))
                .andExpect(jsonPath("$[0].doctorNotes")
                        .value("Rest and fluids"));

        // Duplicate sign-up is refused
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alice\",\"email\":\"alice@test.com\","
                        + "\"phone\":\"9000000009\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());

        // Sign out ends the session
        mockMvc.perform(post("/api/auth/logout").session(alice))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me/appointments").session(alice))
                .andExpect(status().isUnauthorized());
    }
}
