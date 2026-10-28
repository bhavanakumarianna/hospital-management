// ════════════════════════════════════════
// Hospital Management — frontend
// Talks to the Spring Boot REST API
// served from the same address.
// ════════════════════════════════════════

const API = "/api";

const state = {
    patients: [],
    doctors: [],
    appointments: [],
    statusFilter: "ALL",
};

// ─── Helpers ───

const $ = (sel) => document.querySelector(sel);

// Escape text before putting it into HTML
function esc(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

// Call the API; throws with the server's error message
async function api(path, options = {}) {
    const res = await fetch(API + path, {
        headers: options.body ? { "Content-Type": "application/json" } : {},
        ...options,
    });
    const text = await res.text();
    let data = text;
    try { data = text ? JSON.parse(text) : null; } catch { /* plain text */ }
    if (!res.ok) {
        throw new Error(data?.message || `Request failed (${res.status})`);
    }
    return data;
}

let toastTimer;
function toast(message, isError = false) {
    const el = $("#toast");
    el.textContent = message;
    el.className = "toast show" + (isError ? " error" : "");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.className = "toast", 3500);
}

// Run an action and show its error, if any
async function attempt(fn, successMessage) {
    try {
        await fn();
        if (successMessage) toast(successMessage);
        return true;
    } catch (err) {
        toast(err.message, true);
        return false;
    }
}

function emptyRow(cols, text) {
    return `<tr><td class="empty" colspan="${cols}">${text}</td></tr>`;
}

function formatTime(time) {
    return time ? time.slice(0, 5) : "";
}

// ─── Loading data ───

async function loadAll() {
    await attempt(async () => {
        const [patients, doctors, appointments] = await Promise.all([
            api("/patients"), api("/doctors"), api("/appointments"),
        ]);
        state.patients = patients;
        state.doctors = doctors;
        state.appointments = appointments;
        renderAll();
    });
}

function renderAll() {
    renderDashboard();
    renderPatients();
    renderDoctors();
    renderAppointments();
}

// ─── Dashboard ───

function renderDashboard() {
    $("#stat-patients").textContent = state.patients.length;
    $("#stat-doctors").textContent = state.doctors.length;
    $("#stat-available").textContent = state.doctors.filter(d => d.available).length;
    $("#stat-appointments").textContent = state.appointments.length;

    const today = new Date().toISOString().slice(0, 10);
    const upcoming = state.appointments
        .filter(a => a.status === "SCHEDULED" && a.appointmentDate >= today)
        .sort((a, b) => (a.appointmentDate + a.appointmentTime)
            .localeCompare(b.appointmentDate + b.appointmentTime))
        .slice(0, 8);

    $("#upcoming-body").innerHTML = upcoming.length
        ? upcoming.map(a => `
            <tr>
                <td>${esc(a.appointmentDate)}</td>
                <td>${esc(formatTime(a.appointmentTime))}</td>
                <td>${esc(a.patient?.name)}</td>
                <td>${esc(a.doctor?.name)}</td>
                <td class="wrap">${esc(a.reason)}</td>
            </tr>`).join("")
        : emptyRow(5, "No upcoming appointments");
}

// ─── Patients ───

function renderPatients() {
    const query = $("#patient-search").value.trim().toLowerCase();
    const list = state.patients.filter(p => p.name.toLowerCase().includes(query));

    $("#patients-body").innerHTML = list.length
        ? list.map(p => `
            <tr>
                <td>${p.id}</td>
                <td><strong>${esc(p.name)}</strong></td>
                <td>${esc(p.email)}</td>
                <td>${esc(p.phone)}</td>
                <td>${esc(p.age)}</td>
                <td>${esc(p.gender)}</td>
                <td>${esc(p.city)}</td>
                <td>${esc(p.bloodGroup)}</td>
                <td class="row-actions">
                    <button class="btn small" data-edit-patient="${p.id}">Edit</button>
                    <button class="btn small danger" data-delete-patient="${p.id}">Delete</button>
                </td>
            </tr>`).join("")
        : emptyRow(9, query ? "No patients match your search" : "No patients yet");
}

function openPatientForm(patient) {
    const form = $("#patient-form");
    form.reset();
    $("#patient-title").textContent = patient ? "Edit patient" : "Add patient";
    if (patient) {
        for (const [key, value] of Object.entries(patient)) {
            if (form.elements[key]) form.elements[key].value = value ?? "";
        }
    }
    // Email cannot be changed after registration
    form.elements.email.readOnly = !!patient;
    $("#patient-dialog").showModal();
}

$("#patient-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    const data = Object.fromEntries(new FormData(form));
    const id = data.id;
    delete data.id;
    data.age = data.age ? Number(data.age) : 0;

    const ok = await attempt(
        () => api(id ? `/patients/${id}` : "/patients", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(data),
        }),
        id ? "Patient updated" : "Patient registered");
    if (ok) {
        $("#patient-dialog").close();
        loadAll();
    }
});

// ─── Doctors ───

function renderDoctors() {
    const query = $("#doctor-search").value.trim().toLowerCase();
    const list = state.doctors.filter(d => d.name.toLowerCase().includes(query));

    $("#doctors-body").innerHTML = list.length
        ? list.map(d => `
            <tr>
                <td>${d.id}</td>
                <td><strong>${esc(d.name)}</strong><br><small>${esc(d.qualification)}</small></td>
                <td>${esc(d.specialization)}</td>
                <td>${esc(d.department)}</td>
                <td>${esc(d.experienceYears)} yrs</td>
                <td>${esc(d.consultationFee)}</td>
                <td>${esc(d.phone)}</td>
                <td><span class="badge ${d.available ? "on" : "off"}">${d.available ? "Available" : "Unavailable"}</span></td>
                <td class="row-actions">
                    <button class="btn small" data-toggle-doctor="${d.id}">${d.available ? "Set unavailable" : "Set available"}</button>
                    <button class="btn small" data-edit-doctor="${d.id}">Edit</button>
                    <button class="btn small danger" data-delete-doctor="${d.id}">Delete</button>
                </td>
            </tr>`).join("")
        : emptyRow(9, query ? "No doctors match your search" : "No doctors yet");
}

function openDoctorForm(doctor) {
    const form = $("#doctor-form");
    form.reset();
    $("#doctor-title").textContent = doctor ? "Edit doctor" : "Add doctor";
    if (doctor) {
        for (const [key, value] of Object.entries(doctor)) {
            if (form.elements[key]) form.elements[key].value = value ?? "";
        }
    }
    form.elements.email.readOnly = !!doctor;
    $("#doctor-dialog").showModal();
}

$("#doctor-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target));
    const id = data.id;
    delete data.id;
    data.experienceYears = data.experienceYears ? Number(data.experienceYears) : 0;
    data.consultationFee = data.consultationFee ? Number(data.consultationFee) : 0;

    const ok = await attempt(
        () => api(id ? `/doctors/${id}` : "/doctors", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(data),
        }),
        id ? "Doctor updated" : "Doctor added");
    if (ok) {
        $("#doctor-dialog").close();
        loadAll();
    }
});

// ─── Appointments ───

function renderAppointments() {
    const list = state.appointments
        .filter(a => state.statusFilter === "ALL" || a.status === state.statusFilter)
        .sort((a, b) => (b.appointmentDate + b.appointmentTime)
            .localeCompare(a.appointmentDate + a.appointmentTime));

    $("#appointments-body").innerHTML = list.length
        ? list.map(a => `
            <tr>
                <td>${a.id}</td>
                <td>${esc(a.appointmentDate)}</td>
                <td>${esc(formatTime(a.appointmentTime))}</td>
                <td>${esc(a.patient?.name)}</td>
                <td>${esc(a.doctor?.name)}</td>
                <td class="wrap">${esc(a.reason)}</td>
                <td><span class="badge ${esc(a.status)}">${esc(a.status)}</span></td>
                <td class="wrap">${esc(a.doctorNotes)}</td>
                <td class="row-actions">
                    ${a.status === "SCHEDULED" ? `
                        <button class="btn small" data-complete="${a.id}">Complete</button>
                        <button class="btn small danger" data-cancel="${a.id}">Cancel</button>` : ""}
                </td>
            </tr>`).join("")
        : emptyRow(9, "No appointments");
}

function openBookForm() {
    const form = $("#book-form");
    form.reset();
    const available = state.doctors.filter(d => d.available);

    form.elements.patientId.innerHTML = state.patients.length
        ? `<option value="">Select a patient</option>` + state.patients
            .map(p => `<option value="${p.id}">${esc(p.name)} (#${p.id})</option>`).join("")
        : `<option value="">Add a patient first</option>`;
    form.elements.doctorId.innerHTML = available.length
        ? `<option value="">Select a doctor</option>` + available
            .map(d => `<option value="${d.id}">${esc(d.name)} — ${esc(d.specialization)}</option>`).join("")
        : `<option value="">No available doctors</option>`;
    form.elements.date.min = new Date().toISOString().slice(0, 10);

    $("#book-dialog").showModal();
}

$("#book-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const params = new URLSearchParams(new FormData(e.target));
    const ok = await attempt(
        () => api(`/appointments/book?${params}`, { method: "POST" }),
        "Appointment booked");
    if (ok) {
        $("#book-dialog").close();
        loadAll();
    }
});

$("#complete-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const { id, doctorNotes } = Object.fromEntries(new FormData(e.target));
    const params = new URLSearchParams({ doctorNotes });
    const ok = await attempt(
        () => api(`/appointments/${id}/complete?${params}`, { method: "PUT" }),
        "Appointment completed");
    if (ok) {
        $("#complete-dialog").close();
        loadAll();
    }
});

// ─── Event wiring ───

// Tabs
$("#tabs").addEventListener("click", (e) => {
    const tab = e.target.closest(".tab");
    if (!tab) return;
    document.querySelectorAll(".tab").forEach(t => t.classList.toggle("active", t === tab));
    document.querySelectorAll(".view").forEach(v =>
        v.classList.toggle("active", v.id === `view-${tab.dataset.view}`));
    location.hash = tab.dataset.view;
});

// Status filter chips
$("#status-filters").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.statusFilter = chip.dataset.status;
    document.querySelectorAll(".chip").forEach(c => c.classList.toggle("active", c === chip));
    renderAppointments();
});

$("#patient-search").addEventListener("input", renderPatients);
$("#doctor-search").addEventListener("input", renderDoctors);
$("#add-patient").addEventListener("click", () => openPatientForm(null));
$("#add-doctor").addEventListener("click", () => openDoctorForm(null));

// Close buttons inside dialogs
document.querySelectorAll("[data-close]").forEach(btn =>
    btn.addEventListener("click", () => btn.closest("dialog").close()));

// Row buttons (one listener for the whole page)
document.addEventListener("click", async (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    const d = btn.dataset;

    if (d.action === "book") {
        openBookForm();
    } else if (d.editPatient) {
        openPatientForm(state.patients.find(p => p.id == d.editPatient));
    } else if (d.deletePatient) {
        const p = state.patients.find(p => p.id == d.deletePatient);
        if (confirm(`Delete patient "${p.name}"? Their appointments will also be removed.`)) {
            await attempt(() => api(`/patients/${p.id}`, { method: "DELETE" }), "Patient deleted");
            loadAll();
        }
    } else if (d.editDoctor) {
        openDoctorForm(state.doctors.find(doc => doc.id == d.editDoctor));
    } else if (d.deleteDoctor) {
        const doc = state.doctors.find(x => x.id == d.deleteDoctor);
        if (confirm(`Delete "${doc.name}"? Their appointments will also be removed.`)) {
            await attempt(() => api(`/doctors/${doc.id}`, { method: "DELETE" }), "Doctor deleted");
            loadAll();
        }
    } else if (d.toggleDoctor) {
        await attempt(() => api(`/doctors/${d.toggleDoctor}/toggle-availability`, { method: "PUT" }),
            "Availability updated");
        loadAll();
    } else if (d.complete) {
        const form = $("#complete-form");
        form.reset();
        form.elements.id.value = d.complete;
        $("#complete-dialog").showModal();
    } else if (d.cancel) {
        if (confirm("Cancel this appointment?")) {
            await attempt(() => api(`/appointments/${d.cancel}/cancel`, { method: "PUT" }),
                "Appointment cancelled");
            loadAll();
        }
    }
});

// Open the tab from the URL (e.g. /#doctors)
const startView = location.hash.slice(1);
if (startView) document.querySelector(`.tab[data-view="${startView}"]`)?.click();

loadAll();
