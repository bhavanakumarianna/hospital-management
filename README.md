# Hospital Management System

A REST API for managing **patients**, **doctors** and **appointments**, built with Spring Boot, Spring Data JPA and MySQL.

## Tech stack

- Java 17
- Spring Boot 4 (Web MVC, Data JPA, Validation)
- MySQL (H2 in-memory for tests)
- Lombok
- JUnit 5 + Mockito + MockMvc

## Features

- Register, update, search and delete patients (by name, city, blood group)
- Manage doctors, filter by specialization, toggle availability
- Book appointments with rules:
  - the patient and doctor must exist
  - the doctor must be available
  - no double booking (same doctor, date and time)
- Complete appointments (with doctor notes) or cancel them
- Input validation and consistent JSON error responses (400 / 404 / 409 / 500)
- Built-in website to manage everything from the browser

## Getting started

### 1. Prerequisites

- JDK 17+
- MySQL running on `localhost:3306`

### 2. Configure the database

Credentials are read from environment variables, so no password is stored in the code:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
# optional, defaults to jdbc:mysql://localhost:3306/hospital_db
# export DB_URL=jdbc:mysql://localhost:3306/hospital_db
```

The `hospital_db` database and its tables are created automatically on first run.

### 3. Run

```bash
./mvnw spring-boot:run
```

Open **http://localhost:8080** in your browser to use the website (Dashboard, Patients, Doctors, Appointments).
The REST API is available under `http://localhost:8080/api/...`.

### 4. Test

```bash
./mvnw test
```

Tests use an in-memory H2 database, so they don't need MySQL.

## API endpoints

### Patients — `/api/patients`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/patients` | Register a patient |
| GET | `/api/patients` | List all patients |
| GET | `/api/patients/{id}` | Get a patient by id |
| PUT | `/api/patients/{id}` | Update a patient |
| DELETE | `/api/patients/{id}` | Delete a patient |
| GET | `/api/patients/search?name=sar` | Search by name |
| GET | `/api/patients/city/{city}` | Filter by city |
| GET | `/api/patients/blood-group/{group}` | Filter by blood group |
| GET | `/api/patients/count` | Total patients |

### Doctors — `/api/doctors`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/doctors` | Add a doctor |
| GET | `/api/doctors` | List all doctors |
| GET | `/api/doctors/{id}` | Get a doctor by id |
| PUT | `/api/doctors/{id}` | Update a doctor |
| DELETE | `/api/doctors/{id}` | Delete a doctor |
| GET | `/api/doctors/specialization/{name}` | Filter by specialization |
| GET | `/api/doctors/available` | Available doctors |
| GET | `/api/doctors/search?name=smith` | Search by name |
| PUT | `/api/doctors/{id}/toggle-availability` | Toggle availability |
| GET | `/api/doctors/count` | Total doctors |

### Appointments — `/api/appointments`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/appointments/book?patientId=1&doctorId=1&date=2026-12-15&time=10:30&reason=Checkup` | Book an appointment |
| GET | `/api/appointments` | List all appointments |
| GET | `/api/appointments/{id}` | Get an appointment by id |
| GET | `/api/appointments/patient/{patientId}` | A patient's appointments |
| GET | `/api/appointments/doctor/{doctorId}` | A doctor's appointments |
| GET | `/api/appointments/date/{yyyy-MM-dd}` | Appointments on a date |
| PUT | `/api/appointments/{id}/complete?doctorNotes=...` | Mark as completed |
| PUT | `/api/appointments/{id}/cancel` | Cancel |
| GET | `/api/appointments/count` | Total appointments |

### Example requests

```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"Sarah Johnson","email":"sarah@email.com","phone":"9876543210","age":28,"gender":"Female","city":"Atlanta","bloodGroup":"O+"}'

curl -X POST http://localhost:8080/api/doctors \
  -H "Content-Type: application/json" \
  -d '{"name":"Dr. Smith","specialization":"Cardiology","email":"smith@hospital.com","phone":"9123456780","experienceYears":10,"consultationFee":500}'

curl -X POST "http://localhost:8080/api/appointments/book?patientId=1&doctorId=1&date=2026-12-15&time=10:30&reason=Checkup"
```

### Error response format

```json
{
  "status": 404,
  "message": "Patient not found: 5",
  "timestamp": "2026-10-28T10:15:30",
  "error": "NOT_FOUND"
}
```

## Project structure

```
src/main/java/com/hospital
├── controller   # REST endpoints
├── service      # Business rules
├── repository   # Spring Data JPA repositories
├── entity       # JPA entities (Patient, Doctor, Appointment)
└── exception    # Custom exceptions + global error handler
```
