// ════════════════════════════════════════
// Shared sign-in helpers for the portal pages
// (login, patient, doctor, admin).
// Exposes a single global: Auth
// ════════════════════════════════════════

const Auth = (() => {
    const HOME = {
        ADMIN: "/admin.html",
        DOCTOR: "/doctor.html",
        PATIENT: "/patient.html",
    };

    async function api(path, options = {}) {
        const res = await fetch("/api" + path, {
            headers: options.body ? { "Content-Type": "application/json" } : {},
            ...options,
        });
        const text = await res.text();
        let data = text;
        try { data = text ? JSON.parse(text) : null; } catch { /* plain text */ }
        if (!res.ok) {
            const err = new Error(data?.message ||
                (res.status === 401 ? "Please sign in again." : `Request failed (${res.status})`));
            err.status = res.status;
            throw err;
        }
        return data;
    }

    // The signed-in user, or null
    async function me() {
        try { return await api("/auth/me"); } catch { return null; }
    }

    // Send the user to sign in unless they have this role
    async function requireRole(role) {
        const user = await me();
        if (!user || user.role !== role) {
            location.replace(`/login.html?next=${encodeURIComponent(location.pathname)}`);
            return null;
        }
        return user;
    }

    async function logout() {
        await fetch("/api/auth/logout", { method: "POST" });
        location.href = "/login.html";
    }

    function homeFor(role) {
        return HOME[role] || "/";
    }

    function esc(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }

    let toastTimer;
    function toast(message, isError = false) {
        const el = document.getElementById("toast");
        if (!el) return;
        el.textContent = message;
        el.className = "toast show" + (isError ? " error" : "");
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => el.className = "toast", 4000);
    }

    function prettyDate(iso) {
        return new Date(iso + "T00:00").toLocaleDateString(undefined,
            { weekday: "short", day: "numeric", month: "short", year: "numeric" });
    }

    function prettyTime(time) {
        const [h, m] = time.split(":").map(Number);
        return `${((h + 11) % 12) + 1}:${String(m).padStart(2, "0")} ${h >= 12 ? "PM" : "AM"}`;
    }

    function today() {
        const d = new Date();
        d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
        return d.toISOString().slice(0, 10);
    }

    return { api, me, requireRole, logout, homeFor, esc, toast, prettyDate, prettyTime, today };
})();
