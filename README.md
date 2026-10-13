# DentalCare API — Dental Clinic Backend

REST API for a dental clinic management system: patients, doctors, appointments, and medical records, secured with JWT and rate-limited per client. Built with Spring Boot 3.5 and PostgreSQL, with interactive documentation via Swagger UI. Designed to pair with the [dentalcare-front](../dentalcare-front) admin dashboard.

---

## Quick Start

### Prerequisites
- Java 17+
- Docker (for the database and tests)

### 1. Environment

Create a `.env` file in the **parent folder** (next to the repos, shared with `docker-compose.yml`):

```env
POSTGRES_DB=your_postgres_db_name
POSTGRES_USER=your_postgres_user
POSTGRES_PASSWORD=your_postgres_password
DB_URL=jdbc:postgresql://localhost:5432/your_db_name
JWT_SECRET=<long random string>
```

Values are loaded by `springboot3-dotenv` from `../.env` — the same keys feed both the API and `docker-compose.yml`, so credentials stay in one place. Never commit `.env`.

### 2. Database

```bash
docker compose up -d db-spring-dentalcare-api   # from the parent folder
```

### 3. Run the API

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

On startup the API seeds an admin user. Log in and verify:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"rosasdr","password":"Admin1234"}'
```

You get back an `accessToken` — paste it into Swagger UI's **Authorize** button to explore protected endpoints.

| | |
|---|---|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` |
| **Dev credentials** | `rosasdr` / `Admin1234` (seeded, dev-only) |

---

## Tech Stack

| Category | Technology | Version |
|---|---|---|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.5.13 |
| **Security** | Spring Security + JWT (jjwt) | 0.12.6 |
| **Persistence** | Spring Data JPA (Hibernate) + PostgreSQL | 14 |
| **Rate Limiting** | Bucket4j (in-memory token buckets) | 8.19 |
| **Docs** | springdoc-openapi (Swagger UI) | 2.9.1 |
| **Testing** | JUnit 5, Mockito, Testcontainers | — |
| **Build** | Maven (wrapper included) | — |
| **Config** | springboot3-dotenv (`../.env`) | 5.1.0 |

---

## Features

- **JWT auth** — stateless access + refresh tokens (`POST /api/auth/login`, `POST /api/auth/refresh`); every endpoint except login/refresh and the docs requires a Bearer token.
- **Full CRUD** — patients, doctors, appointments; medical records are read/update/delete (created automatically when an appointment is completed).
- **Anti double-booking** — a doctor cannot hold two active appointments at the same date/time.
- **Pagination + filters** — every list endpoint returns a Spring `Page<T>` with filters (name, status, date, procedure, diagnosis…).
- **Rate limiting** — per-client token buckets: 5/min on login endpoints, 60/min on everything else; over-limit requests get `429` + `Retry-After`. Disable with `rate-limit.enabled=false`.
- **Interactive docs** — Swagger UI grouped by resource with a JWT "Authorize" flow.
- **Error handling** — validation errors → `400` with field details, duplicate email/phone → `409`, invalid login → `401`.

---

## API Endpoints

Route prefixes are **intentionally inconsistent** (`/api/auth`, `/api/users` vs. unprefixed resources) — the frontend calls these exact paths.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/login` | Authenticate (returns access + refresh tokens) |
| POST | `/api/auth/refresh` | Refresh tokens |
| GET | `/api/users/me` | Current user profile |
| PATCH | `/api/users/password` | Change password |
| GET / POST | `/patients` | List (paginated, `?name=`) / create |
| GET / PUT / DELETE | `/patients/{id}` | Read / update / delete |
| GET / POST | `/doctors` | List (paginated, `?name=`) / create |
| GET / PUT / DELETE | `/doctors/{id}` | Read / update / delete |
| GET / POST | `/appointments` | List (paginated, `?status=&date=&procedure=&name=`) / create |
| GET / PUT / DELETE | `/appointments/{id}` | Read / update / delete |
| GET | `/medical-records` | List (paginated, `?name=&diagnosis=`) |

Completing an appointment (`PUT /appointments/{id}` with `"status":"COMPLETED"` + `diagnosis` + `treatment`) auto-creates its medical record.

---

## Architecture

Layered, hexagonal-inspired packages under `com.dentalcare.tony.api`:

```
domain/           entities, enums, filter records
application/      service interfaces + *ServiceImpl (business rules)
infrastructure/   persistence (JPA repos, Specifications), security (JWT),
                  ratelimit/, config/, seed/
presentation/     controllers, DTOs (dto/request, dto/response), exceptions
```

Dependency direction: `presentation → application → domain ← infrastructure`.
Controllers never touch repositories; services return DTOs, never entities.

### Project Structure

```
src/main/java/com/dentalcare/tony/api/
├── domain/
├── application/service/          # PatientService(Impl), AppointmentService(Impl)…
├── infrastructure/
│   ├── persistence/repository/   # JPA repositories + Specifications
│   ├── security/                 # JwtService, JwtAuthenticationFilter, entry points
│   ├── ratelimit/                # ClientRateLimiter, RateLimitFilter, config
│   ├── config/                   # SecurityConfig, CorsConfig, OpenApiConfig…
│   └── seed/                     # DataInitializer (admin user)
└── presentation/
    ├── controller/               # PatientController, AuthController…
    ├── dto/request|response/     # *Request / *Response records
    └── exception/                # GlobalExceptionHandler, ErrorResponse
```

---

## Testing

```bash
./mvnw test                                          # full suite (needs Docker)
./mvnw test "-Dtest=RateLimit*Test,ClientRateLimiterTest"   # rate-limit only
```

30 tests, no local database needed: Testcontainers boots a throwaway PostgreSQL (`postgres:14-alpine`) per run.

| Layer | Classes |
|---|---|
| Integration (full app + JWT + JPA) | `PatientApiIntegrationTest`, `AppointmentApiIntegrationTest`, `OpenApiDocsIntegrationTest`, `ApiApplicationTests` |
| Controller slices (`@WebMvcTest`, mocked services) | `PatientControllerTest`, `AppointmentControllerTest` |
| Unit (no DB) | `RateLimitFilterTest`, `ClientRateLimiterTest` |

Integration tests log in as the seeded admin and exercise the real security chain; slices use `@AutoConfigureMockMvc(addFilters=false)` — security is proven by the integration layer, not mocked around.

---

## Configuration Reference

| Property | Default | Notes |
|---|---|---|
| `spring.datasource.url` | `${DB_URL}` | JDBC URL from `../.env` |
| `spring.datasource.username` | `${POSTGRES_USER}` | Same keys as docker-compose |
| `spring.datasource.password` | `${POSTGRES_PASSWORD}` | |
| `jwt.secret` | `${JWT_SECRET}` | HMAC secret; use a long random value |
| `rate-limit.enabled` | `true` | Kill-switch for the rate limiter |
| `rate-limit.login.capacity` | `5` | Login burst size (per minute) |
| `rate-limit.api.capacity` | `60` | General API burst size (per minute) |

Schema is managed by Hibernate (`ddl-auto=update`) — **dev only**, there are no migrations. Never run this against production data.

---

## License

MIT License — see the [frontend repository](../dentalcare-front#licen) for the full text.
