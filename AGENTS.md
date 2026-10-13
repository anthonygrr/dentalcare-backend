# AGENTS.md — DentalCare Backend

REST API for a dental clinic management system (patients, doctors, appointments, medical records, users/auth).
Pairs with the frontend in the sibling repo `dentalcare-front`.

## Stack

- Java 17, Spring Boot 3.5.13, Maven (wrapper: `mvnw` / `mvnw.cmd`)
- Spring Data JPA (Hibernate) + PostgreSQL 14
- Spring Security + JWT (jjwt 0.12.6), stateless sessions
- Lombok, `spring-boot-starter-validation`, `spring-boot-starter-test`

## Commands

```bash
./mvnw spring-boot:run          # run API (port 8080)
./mvnw test                     # run tests (needs Docker for Testcontainers)
./mvnw package                  # build jar
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # if a dev profile is added later
```

While running: Swagger UI on `http://localhost:8080/swagger-ui.html`, OpenAPI JSON on `/v3/api-docs`.

Database: start Postgres from the parent folder (`../docker-compose.yml`, service `db-spring-dentalcare-api`, port 5432).

## Required environment variables

Loaded by `me.paulschwarz:springboot3-dotenv` from `../.env` (parent folder, shared with `docker-compose.yml` — see the `springdotenv.*` keys in `application.properties`).

| Variable            | Purpose                                                         |
|---------------------|-----------------------------------------------------------------|
| `DB_URL`            | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/dentalcare_db_api` |
| `POSTGRES_USER`     | DB user — same key the compose file uses                        |
| `POSTGRES_PASSWORD` | DB password — same key the compose file uses                    |
| `JWT_SECRET`        | HMAC secret for JWT signing (use a long random value)           |

The datasource reuses the compose `POSTGRES_*` keys on purpose: one credential pair, one place to change. Do not hardcode these values in the repo. `application.properties` is the only config file; there is no `application-dev.properties`.

Note: spring-dotenv does not resolve nested `${...}` references inside `.env` values — define every key with a literal value.

## Architecture

Layered (hexagonal-inspired) packages under `com.dentalcare.tony.api`:

```
domain/          entities, enums, filter records (pure domain, no Spring Web)
application/     service interfaces + *ServiceImpl implementations (business rules)
infrastructure/  persistence (JPA repositories, Specifications), security (JWT),
                 config, seed (DataInitializer)
presentation/    controllers, DTOs (dto/request, dto/response), exception handling
```

Dependency direction: `presentation → application → domain ← infrastructure`.
Controllers never touch repositories; services never return entities to controllers (they map to DTOs).

### Request flow

`Controller (@RestController, @Valid DTO in)` → `Service interface` → `ServiceImpl` → `Jpa*Repository` (+ `*Specification` for criteria queries) → `*Response` DTO out.

### Cross-cutting

- `presentation/exception/GlobalExceptionHandler` — maps exceptions to `ErrorResponse`; business exceptions extend `BusinessException`.
- `infrastructure/security/JwtAuthenticationFilter` + `JwtService` — Bearer token auth on every request.
- `infrastructure/config/SecurityConfig` — `/api/auth/**` and Swagger paths are public; everything else authenticated; `@EnableMethodSecurity` is on.
- `infrastructure/config/CorsConfig` — global CORS for the frontend dev server.
- `infrastructure/ratelimit/` — per-client-IP token-bucket rate limiting (Bucket4j, in-memory). `RateLimitFilter` is registered **before** the Security chain (`RateLimitConfig`, order -101) so unauthenticated floods are throttled too. `/api/auth/*` gets the stricter `login` bucket; everything else the general `api` bucket. Over-limit → `429` + `Retry-After` + JSON body in `ErrorResponse` shape. Idle buckets are evicted every 10 minutes. Disable with `rate-limit.enabled=false`; tune `rate-limit.api.*` / `rate-limit.login.*` in `application.properties`.
- `infrastructure/seed/DataInitializer` — on startup, seeds the ADMIN role and a default admin user.

## Conventions

- DTO suffixes are mandatory: `*Request` (input), `*Response` (output). Keep them in the matching `dto/request` / `dto/response` package.
- Entity → DTO mapping lives in the service layer, not the controller.
- List endpoints accept a `*Filter` record built from `@RequestParam`s and return `Page<T>`; default sort is `name` ascending when the client sends none.
- Controllers return `ResponseEntity<T>`: `200` for reads/writes, `204` for deletes.
- Use Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Getter`) instead of handwritten boilerplate.
- Add new business rules in `*ServiceImpl`, never in controllers.
- Keep validation on DTOs with `jakarta.validation` annotations; the global handler renders field errors.

## Known quirks (read before editing)

- **Route prefixes are inconsistent**: `/api/auth` and `/api/users` use the `/api` prefix, while `/patients`, `/doctors`, `/appointments`, `/medical-records` do not. Do not "fix" this casually — the frontend calls the exact paths above. Any change must be coordinated with `dentalcare-front`.
- **`ddl-auto=update`**: Hibernate manages the schema; there are no migrations (no Flyway/Liquibase). Never run this profile against production data.
- **`@CrossOrigin(origins = "http://localhost:8080")` on controllers** duplicates/conflicts with global CORS in `CorsConfig`; prefer the global config for new controllers.
- **Seeded credentials**: `DataInitializer` creates an admin user with a hardcoded password and logs it at startup. Treat it as dev-only data.
- **Swagger UI is installed** (springdoc 2.9.1 — the 2.x line is the Spring Boot 3.x one; 3.x requires Boot 4): open at `http://localhost:8080/swagger-ui.html`, spec at `/v3/api-docs`. Both paths are permitted in `SecurityConfig`. Metadata + JWT `bearerAuth` scheme live in `infrastructure/config/OpenApiConfig`; controllers use `@Tag` for UI grouping. Use the UI's "Authorize" button with a token from `POST /api/auth/login`.
- `HELP.md` and empty `<name/>`, `<description/>`, `<licenses>` tags in `pom.xml` are leftovers from Spring Initializr.

## Testing

- Real suite, no scaffolding: `./mvnw test` runs everything green with **Docker running** — no local database or env vars needed. Testcontainers (`postgres:14-alpine`, same image as compose) provides the DB via `@ServiceConnection`.
- Integration (full app + JWT login + JPA): `PatientApiIntegrationTest`, `AppointmentApiIntegrationTest`, `ApiApplicationTests` share a JVM-wide container via `support/PostgresIntegrationTest`. Rate limiting and `jwt.secret` are overridden in tests.
- Controller slices (`@WebMvcTest` + `@MockitoBean`, no DB): `PatientControllerTest`, `AppointmentControllerTest`. Security filters are off in slices (`@AutoConfigureMockMvc(addFilters=false)`) — real security is covered by the integration tests.
- Rate-limit unit tests (no DB): `RateLimitFilterTest`, `ClientRateLimiterTest`. Run only these with `./mvnw test "-Dtest=RateLimit*Test,ClientRateLimiterTest"`.
- Known current behavior documented as characterization tests: missing patient/appointment and doctor double-booking surface as `500` (bare `RuntimeException`/`IllegalArgumentException` instead of `BusinessException`); `AppointmentController.create/update` lack `@Valid`.
