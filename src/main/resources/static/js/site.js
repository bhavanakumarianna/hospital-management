// ════════════════════════════════════════
// CarePoint Hospital — public website
// Loads live doctors from the REST API and
// handles online appointment booking.
// ════════════════════════════════════════

const API = "/api";

const DEPARTMENTS = [
    { name: "Cardiology", icon: "heart", desc: "Heart and vascular care, ECG, echo and cardiac rehabilitation." },
    { name: "Neurology", icon: "brain", desc: "Brain, spine and nerve disorders, stroke and epilepsy care." },
    { name: "Orthopedics", icon: "bone", desc: "Joint replacement, sports injuries, fractures and spine care." },
    { name: "Pediatrics", icon: "baby", desc: "Complete care for newborns, children and teenagers." },
    { name: "Gynecology", icon: "venus", desc: "Women's health, pregnancy, maternity and fertility care." },
    { name: "Dermatology", icon: "sparkle", desc: "Skin, hair and nail conditions, allergies and cosmetic care." },
    { name: "Oncology", icon: "ribbon", desc: "Cancer screening, chemotherapy and personalised treatment." },
    { name: "ENT", icon: "ear", desc: "Ear, nose and throat care, hearing and sinus treatment." },
    { name: "Ophthalmology", icon: "eye", desc: "Eye exams, cataract and retina care, vision correction." },
    { name: "General Medicine", icon: "stetho", desc: "Diagnosis and treatment of everyday and chronic illness." },
    { name: "Emergency", icon: "siren", desc: "Round-the-clock trauma, accident and critical care." },
    { name: "Radiology", icon: "scan", desc: "MRI, CT, X-ray and ultrasound imaging with expert reports." },
];

// Appointment slots offered each day (30 minutes apart)
const SLOTS = [];
for (let h = 9; h < 17; h++) {
    if (h === 13) continue; // lunch break
    SLOTS.push(`${String(h).padStart(2, "0")}:00`, `${String(h).padStart(2, "0")}:30`);
}

const state = { doctors: [], specFilter: "All", user: null };
const PENDING_KEY = "carepoint.pendingBooking";

// ─── Helpers ───

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => [...document.querySelectorAll(sel)];

function esc(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;").replace(/</g, "&lt;")
        .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function icon(name) {
    return `<svg class="i"><use href="#i-${name}"/></svg>`;
}

async function api(path, options = {}) {
    const res = await fetch(API + path, {
        headers: options.body ? { "Content-Type": "application/json" } : {},
        ...options,
    });
    const text = await res.text();
    let data = text;
    try { data = text ? JSON.parse(text) : null; } catch { /* plain text */ }
    if (!res.ok) {
        const err = new Error(data?.message || `Request failed (${res.status})`);
        err.status = res.status;
        throw err;
    }
    return data;
}

let toastTimer;
function toast(message, isError = false) {
    const el = $("#toast");
    el.textContent = message;
    el.className = "toast show" + (isError ? " error" : "");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.className = "toast", 4000);
}

function initials(name) {
    return name.replace(/^dr\.?\s*/i, "").split(/\s+/)
        .map(w => w[0]).join("").slice(0, 2).toUpperCase();
}

function sameSpec(a, b) {
    return a.trim().toLowerCase() === b.trim().toLowerCase();
}

function today() {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 10);
}

function prettyDate(iso) {
    return new Date(iso + "T00:00").toLocaleDateString(undefined,
        { weekday: "short", day: "numeric", month: "short", year: "numeric" });
}

function prettyTime(time) {
    const [h, m] = time.split(":").map(Number);
    const suffix = h >= 12 ? "PM" : "AM";
    return `${((h + 11) % 12) + 1}:${String(m).padStart(2, "0")} ${suffix}`;
}

// ─── Navigation ───

window.addEventListener("scroll", () =>
    $("#nav").classList.toggle("scrolled", window.scrollY > 10), { passive: true });

$("#nav-toggle").addEventListener("click", () => $("#nav-links").classList.toggle("open"));
$$("#nav-links a").forEach(a =>
    a.addEventListener("click", () => $("#nav-links").classList.remove("open")));

$("#year").textContent = new Date().getFullYear();

// ─── Scroll reveal & counters ───

const revealer = new IntersectionObserver((entries) => {
    entries.forEach(e => {
        if (!e.isIntersecting) return;
        e.target.classList.add("visible");
        revealer.unobserve(e.target);
    });
}, { threshold: 0.12 });
$$(".reveal").forEach(el => revealer.observe(el));

function animateCount(el) {
    const target = Number(el.dataset.count) || 0;
    const suffix = el.dataset.suffix || "";
    if (matchMedia("(prefers-reduced-motion: reduce)").matches) {
        el.textContent = target.toLocaleString() + suffix;
        return;
    }
    const start = performance.now();
    const duration = 1400;
    function tick(now) {
        const p = Math.min((now - start) / duration, 1);
        const eased = 1 - Math.pow(1 - p, 3);
        el.textContent = Math.round(target * eased).toLocaleString() + suffix;
        if (p < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
}

const counter = new IntersectionObserver((entries) => {
    entries.forEach(e => {
        if (!e.isIntersecting) return;
        animateCount(e.target);
        counter.unobserve(e.target);
    });
}, { threshold: 0.5 });

// ─── Departments ───

function renderDepartments() {
    $("#dept-grid").innerHTML = DEPARTMENTS.map(d => {
        const count = state.doctors.filter(doc => sameSpec(doc.specialization, d.name)).length;
        return `
            <button class="dept reveal visible" data-dept="${esc(d.name)}">
                <span class="dept-icon">${icon(d.icon)}</span>
                <h3>${esc(d.name)}</h3>
                <p>${esc(d.desc)}</p>
                <span class="dept-count">${count ? `${count} doctor${count > 1 ? "s" : ""}` : "View doctors"} ${icon("arrow")}</span>
            </button>`;
    }).join("");
}

$("#dept-grid").addEventListener("click", (e) => {
    const card = e.target.closest(".dept");
    if (!card) return;
    const match = specializations().find(s => sameSpec(s, card.dataset.dept));
    state.specFilter = match || "All";
    renderDoctors();
    if (!match) toast(`No ${card.dataset.dept} doctors listed yet — showing all doctors.`);
    $("#doctors").scrollIntoView();
});

// ─── Doctors ───

function specializations() {
    const seen = new Map();
    state.doctors.forEach(d => {
        const key = d.specialization.trim().toLowerCase();
        if (!seen.has(key)) seen.set(key, d.specialization.trim());
    });
    return [...seen.values()].sort();
}

function renderDoctors() {
    const specs = specializations();
    $("#doctor-chips").innerHTML = ["All", ...specs].map(s =>
        `<button class="chip ${s === state.specFilter ? "active" : ""}" data-spec="${esc(s)}">${esc(s)}</button>`
    ).join("");

    const query = $("#doctor-search").value.trim().toLowerCase();
    const list = state.doctors
        .filter(d => state.specFilter === "All" || sameSpec(d.specialization, state.specFilter))
        .filter(d => d.name.toLowerCase().includes(query))
        .sort((a, b) => Number(b.available) - Number(a.available));

    $("#doctor-grid").innerHTML = list.length ? list.map(d => `
        <article class="doctor">
            <div class="doctor-photo">
                <span class="doctor-initials">${esc(initials(d.name))}</span>
                <span class="doctor-status ${d.available ? "" : "off"}">${d.available ? "Available" : "Unavailable"}</span>
            </div>
            <div class="doctor-body">
                <span class="doctor-spec">${esc(d.specialization)}</span>
                <h3>${esc(d.name)}</h3>
                <span class="doctor-qual">${esc(d.qualification || d.department || "")}</span>
                <div class="doctor-meta">
                    <div><strong>${esc(d.experienceYears)}+ yrs</strong>Experience</div>
                    <div><strong>${d.consultationFee ? "$" + esc(d.consultationFee) : "—"}</strong>Consultation</div>
                </div>
                <button class="btn ${d.available ? "btn-primary" : "btn-ghost"} btn-block"
                        data-book-doctor="${d.id}" ${d.available ? "" : "disabled"}>
                    ${d.available ? `${icon("calendar")} Book appointment` : "Not taking bookings"}
                </button>
            </div>
        </article>`).join("")
        : `<div class="empty-state">No doctors match your search.</div>`;
}

$("#doctor-chips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.specFilter = chip.dataset.spec;
    renderDoctors();
});
$("#doctor-search").addEventListener("input", renderDoctors);

$("#doctor-grid").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-book-doctor]");
    if (!btn) return;
    const doctor = state.doctors.find(d => d.id == btn.dataset.bookDoctor);
    showTab("new");
    $("#book-department").value = doctor.specialization.trim();
    fillDoctorSelect();
    $("#book-doctor").value = doctor.id;
    loadSlots();
    $("#book").scrollIntoView();
});

// ─── Booking form ───

function availableDoctors() {
    return state.doctors.filter(d => d.available);
}

function fillDepartmentSelect() {
    const specs = [...new Set(availableDoctors().map(d => d.specialization.trim()))].sort();
    $("#book-department").innerHTML = specs.length
        ? `<option value="">Select department</option>` +
            specs.map(s => `<option>${esc(s)}</option>`).join("")
        : `<option value="">No doctors available right now</option>`;
}

function fillDoctorSelect() {
    const spec = $("#book-department").value;
    const doctors = availableDoctors().filter(d => spec && sameSpec(d.specialization, spec));
    $("#book-doctor").innerHTML = doctors.length
        ? `<option value="">Select doctor</option>` + doctors.map(d =>
            `<option value="${d.id}">${esc(d.name)}${d.consultationFee ? ` · $${esc(d.consultationFee)}` : ""}</option>`).join("")
        : `<option value="">${spec ? "No available doctors" : "Choose a department first"}</option>`;
}

async function loadSlots() {
    const select = $("#book-time");
    const doctorId = $("#book-doctor").value;
    const date = $("#book-date").value;
    if (!doctorId || !date) {
        select.innerHTML = `<option value="">Choose doctor &amp; date first</option>`;
        return;
    }
    select.innerHTML = `<option value="">Checking availability…</option>`;
    try {
        const booked = await api(`/appointments/booked-slots?${new URLSearchParams({ doctorId, date })}`);
        const taken = new Set(booked.map(t => t.slice(0, 5)));
        const now = new Date();
        const nowTime = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
        const free = SLOTS.filter(s => !taken.has(s) && !(date === today() && s <= nowTime));
        select.innerHTML = free.length
            ? `<option value="">Select a time</option>` +
                free.map(s => `<option value="${s}">${prettyTime(s)}</option>`).join("")
            : `<option value="">No free slots — try another date</option>`;
    } catch (err) {
        select.innerHTML = `<option value="">Could not load slots</option>`;
        toast(err.message, true);
    }
}

$("#book-department").addEventListener("change", () => { fillDoctorSelect(); loadSlots(); });
$("#book-doctor").addEventListener("change", loadSlots);
$("#book-date").addEventListener("change", loadSlots);
$("#book-date").min = today();

function showTab(name) {
    $$(".book-tab").forEach(t => t.classList.toggle("active", t.dataset.tab === name));
    $$(".book-pane").forEach(p => p.classList.toggle("active", p.dataset.pane === name));
}

$$(".book-tab").forEach(tab => tab.addEventListener("click", () => showTab(tab.dataset.tab)));
$$("[data-open-status]").forEach(link => link.addEventListener("click", () => showTab("status")));

$("#book-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    const errorEl = $("#book-error");
    errorEl.textContent = "";

    // Highlight invalid fields
    let firstInvalid = null;
    [...form.elements].forEach(el => {
        if (!el.willValidate) return;
        const bad = !el.checkValidity();
        el.classList.toggle("invalid", bad);
        if (bad && !firstInvalid) firstInvalid = el;
    });
    if (firstInvalid) {
        errorEl.textContent = "Please complete the highlighted fields.";
        firstInvalid.focus();
        return;
    }

    const data = Object.fromEntries(new FormData(form));

    // Booking needs a patient account: remember the choice,
    // sign in, then come back here to confirm.
    if (state.user?.role !== "PATIENT") {
        try { sessionStorage.setItem(PENDING_KEY, JSON.stringify(data)); } catch { /* optional */ }
        location.href = `login.html?next=${encodeURIComponent("/#book")}`;
        return;
    }

    const submit = $("#book-submit");
    submit.disabled = true;
    submit.textContent = "Booking…";
    try {
        const params = new URLSearchParams({
            doctorId: data.doctorId,
            date: data.date,
            time: data.time,
            reason: data.reason.trim(),
        });
        const appt = await api(`/me/appointments?${params}`, { method: "POST" });
        showConfirmation(appt);
        form.reset();
        fillDoctorSelect();
        loadSlots();
        renderStatus();
    } catch (err) {
        errorEl.textContent = err.message;
    } finally {
        submit.disabled = false;
        renderAccountBox();
    }
});

// Who is booking: the signed-in patient, or a prompt to sign in
function renderAccountBox() {
    const box = $("#book-account");
    const submit = $("#book-submit");
    const user = state.user;
    if (user?.role === "PATIENT") {
        box.innerHTML = `
            <div class="account-box">
                <span class="avatar">${esc(initials(user.name))}</span>
                <div><small>Booking as</small><strong>${esc(user.name)}</strong></div>
                <a href="patient.html">My dashboard →</a>
            </div>`;
        submit.innerHTML = `${icon("calendar")} Confirm appointment`;
        $("#book-note").textContent = "You'll see this booking in your patient dashboard.";
    } else if (user) {
        box.innerHTML = `
            <div class="account-box warn">
                <div><strong>Signed in as ${esc(user.role.toLowerCase())}</strong>
                <small>Online booking is for patient accounts. Staff can book from the staff portal.</small></div>
            </div>`;
        submit.innerHTML = `${icon("lock")} Sign in as a patient to book`;
        $("#book-note").textContent = "";
    } else {
        box.innerHTML = `
            <div class="account-box">
                <div><strong>Sign in to confirm</strong>
                <small>New here? <a href="login.html?tab=register&next=${encodeURIComponent("/#book")}">Create a free patient account</a> — it takes a minute.</small></div>
            </div>`;
        submit.innerHTML = `${icon("lock")} Sign in &amp; book`;
        $("#book-note").textContent = "Your choices are saved while you sign in.";
    }
}

function showConfirmation(appt) {
    const pane = $("#book-done");
    pane.innerHTML = `
        <div class="confirm-icon">${icon("check")}</div>
        <h3>You're booked in!</h3>
        <p>We look forward to seeing you, ${esc(appt.patient.name.split(" ")[0])}.</p>
        <div class="confirm-details">
            <div><span>Booking ID</span><strong>#${appt.id}</strong></div>
            <div><span>Doctor</span><strong>${esc(appt.doctor.name)}</strong></div>
            <div><span>Department</span><strong>${esc(appt.doctor.specialization)}</strong></div>
            <div><span>Date</span><strong>${esc(prettyDate(appt.appointmentDate))}</strong></div>
            <div><span>Time</span><strong>${esc(prettyTime(appt.appointmentTime))}</strong></div>
        </div>
        <p>Please arrive 15 minutes early with a photo ID.</p>
        <br>
        <button class="btn btn-ghost" id="book-another">Book another appointment</button>`;
    showTab("done");
    $("#book-another").addEventListener("click", () => showTab("new"));
    $("#book").scrollIntoView();
}

// ─── My appointments tab ───

async function renderStatus() {
    const pane = $("#status-pane");
    if (state.user?.role !== "PATIENT") {
        pane.innerHTML = `
            <div class="signed-in-prompt">
                <p class="pane-intro">Sign in to see, manage and cancel your appointments.</p>
                <a class="btn btn-primary btn-block" href="login.html">Sign in</a>
                <a class="btn btn-ghost btn-block" href="login.html?tab=register">Create a patient account</a>
            </div>`;
        return;
    }
    try {
        const appts = (await api("/me/appointments"))
            .sort((a, b) => (b.appointmentDate + b.appointmentTime)
                .localeCompare(a.appointmentDate + a.appointmentTime))
            .slice(0, 5);
        pane.innerHTML = (appts.length ? appts.map(a => {
            const d = new Date(a.appointmentDate + "T00:00");
            return `
                <div class="appt">
                    <div class="appt-date">
                        <strong>${d.getDate()}</strong>
                        <small>${d.toLocaleDateString(undefined, { month: "short" })}</small>
                    </div>
                    <div class="appt-info">
                        <strong>${esc(a.doctor.name)} · ${esc(a.doctor.specialization)}</strong>
                        <small>${esc(prettyTime(a.appointmentTime))} · ${esc(a.reason)}</small>
                    </div>
                    <span class="badge ${esc(a.status)}">${esc(a.status.charAt(0) + a.status.slice(1).toLowerCase())}</span>
                </div>`;
        }).join("") : `<p class="pane-intro">You have no appointments yet.</p>`)
            + `<a class="btn btn-ghost btn-block" href="patient.html" style="margin-top:12px">Open my dashboard</a>`;
    } catch (err) {
        pane.innerHTML = `<p class="pane-intro">${esc(err.message)}</p>`;
    }
}

function renderNavAccount() {
    const link = $("#nav-account");
    const user = state.user;
    if (!user) return;
    link.href = user.role === "ADMIN" ? "admin.html"
        : user.role === "DOCTOR" ? "doctor.html" : "patient.html";
    link.querySelector("span").textContent = user.role === "PATIENT" ? "My account"
        : user.role === "DOCTOR" ? "Doctor portal" : "Staff portal";
}

// Restore a booking started before signing in
function restorePendingBooking() {
    let pending = null;
    try {
        pending = JSON.parse(sessionStorage.getItem(PENDING_KEY));
        sessionStorage.removeItem(PENDING_KEY);
    } catch { /* optional */ }
    if (!pending || state.user?.role !== "PATIENT") return;
    $("#book-department").value = pending.department || "";
    fillDoctorSelect();
    $("#book-doctor").value = pending.doctorId || "";
    $("#book-date").value = pending.date || "";
    $("#book-form").elements.reason.value = pending.reason || "";
    loadSlots().then(() => { $("#book-time").value = pending.time || ""; });
    showTab("new");
    toast("Welcome back — check the details and confirm your appointment.");
}

// ─── Load live data ───

async function init() {
    renderDepartments();
    try {
        const [doctors, patientCount] = await Promise.all([
            api("/doctors"), api("/patients/count"),
        ]);
        state.doctors = doctors;
        $("#stat-doctors").dataset.count = doctors.length;
        $("#stat-patients").dataset.count = patientCount;
        const availableNow = doctors.filter(d => d.available).length;
        if (availableNow) {
            $("#hero-available").textContent = `${availableNow} specialist${availableNow === 1 ? "" : "s"}`;
            $("#hero-available-note").textContent = "available for booking";
        }
    } catch (err) {
        $("#doctor-grid").innerHTML =
            `<div class="empty-state">Doctor information is temporarily unavailable.</div>`;
    }
    $$(".stat-num").forEach(el => counter.observe(el));
    renderDepartments();
    if (state.doctors.length) renderDoctors();
    fillDepartmentSelect();
    fillDoctorSelect();

    state.user = await api("/auth/me").catch(() => null);
    renderNavAccount();
    renderAccountBox();
    renderStatus();
    restorePendingBooking();
}

init();
