# Bulk-Importing Patients from CSV in Spring Boot

Clinics migrate from spreadsheets, not from other APIs. Until now, loading patients into
DentalCare meant one `POST /patients` per row — a non-starter when a receptionist has a few
thousand rows exported from their old system. This guide documents how we built
**`POST /patients/import`**: a multipart CSV endpoint that imports every valid row and reports
the rest back with physical line numbers, without letting one bad row destroy a good import.

```http
POST /patients/import            (multipart/form-data, field name: file)
Authorization: Bearer <JWT>
```

The whole feature is deliberately small: one dependency, one response record, one parser
component, one non-transactional service method, and one controller guard. What follows is what
the code actually does — every message and JSON key below is taken verbatim from the source.

## What you get — quick reference

| Item | Value |
|------|-------|
| Endpoint | `POST /patients/import` (no `/api` prefix — consistent with the sibling `/patients` routes) |
| Method / content type | `POST`, `multipart/form-data` |
| Request field | `file` (a `.csv` file; missing/empty file → `400`) |
| Auth | `Authorization: Bearer <JWT>` — unauthenticated → `401` |
| CSV columns (header required) | `name,email,phone,birthDate` — exact, case-sensitive; order does not matter; extra columns ignored |
| Date format | `yyyy-MM-dd` (ISO local date) |
| Encoding | UTF-8 |
| Success response (`200`) | `{ "total": int, "imported": int, "failed": int, "errors": [ { "row": int, "message": string } ] }` |
| Row error `row` value | Physical line number in the file (header counts as line 1, blank lines are counted too) |
| Structural problems → `400` | Missing/empty file, empty file, missing/wrong header column, malformed CSV (e.g. unclosed quote) — Spring `ProblemDetail`: `title: "Business Error"`, plus `detail` with the message |
| Row problems → `200` with `errors[]` | Bean-validation failures, `birthDate` parse failures, duplicate email/phone, other per-row persistence failures |
| Unauthenticated → `401` | JSON body with `"message": "Authentication required"` |
| Upload limits | `5MB` per file, `10MB` per request (Spring Boot's defaults are `1MB`/`10MB`) |
| Rate limit | Shares the general `api` bucket (burst 60, 60/min) — not the stricter `login` bucket |
| Semantics | **Create only, partial success**: valid rows are imported even when other rows fail; no upsert |

## Architecture / request flow

The import travels the project's normal layered path — controller stays thin, the service owns
the business rules, and parsing lives in its own infrastructure component:

```
Client (multipart/form-data: patients.csv)
  │
  ▼
PatientController.importPatients          presentation/
  │  guard: file == null || file.isEmpty() → BusinessException("CSV file is required")
  │  opens file.getInputStream()
  ▼
PatientServiceImpl.importPatients         application/   ← NO @Transactional (on purpose)
  │  1. PatientCsvParser.parse(csv)        infrastructure/csv/  → header check, rows + line numbers
  │     (structural failure → BusinessException → 400, nothing imported)
  │  2. for each row: importRow(row)
  │       ├─ LocalDate.parse(birthDate)                 → row error on failure
  │       ├─ validator.validate(PatientCreationRequest) → row error on violations
  │       └─ patientRepository.saveAndFlush(entity)     → own transaction per row
  │            └─ DataIntegrityViolationException       → row error (uk_patient_email / uk_patient_phone)
  ▼
PatientImportResponse(total, imported, failed, errors[])   → 200 JSON
```

Structural problems (the file itself is unusable) abort before any insert; row problems are
collected and reported while the good rows keep committing.

## Step-by-step build

### 1. Dependency and multipart limits

```xml
<!-- CSV patient import (Apache Commons CSV; version is explicit — Boot does not manage it) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.14.1</version>
</dependency>
```

Spring Boot does not manage `commons-csv`, so the version is pinned explicitly — do not leave
it to transitive luck.

Then raise the upload caps in `application.properties`:

```properties
# MULTIPART (CSV patient import)
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB
```

**Why the default was too small:** Spring Boot ships with `spring.servlet.multipart.max-file-size=1MB`.
A real spreadsheet export — a few thousand rows of names, emails, phones — crosses 1MB easily,
and the container would reject the upload before our controller ever ran. `5MB` still keeps the
endpoint safely bounded (a CSV of that size holds far more rows than anyone wants to review in
one response), while `max-request-size=10MB` leaves headroom for multipart overhead.

### 2. The response DTO

```java
public record PatientImportResponse(
        int total,
        int imported,
        int failed,
        List<RowError> errors
) {
    public record RowError(int row, String message) {
    }
}
```

Three counts plus a list of row-level errors. `RowError.row` is the **physical line number in
the uploaded file** — header included, blank lines counted — so a user can jump straight to line
47 of their spreadsheet. `total` is the number of data rows (blank lines are skipped), and
`failed` always equals `errors.size()`.

### 3. Parsing with Apache Commons CSV

**Why not `String.split`?** The moment a name is `"Doe, John"` or a field is quoted, naive
splitting corrupts the row — silently, which is worse than failing. Commons CSV implements
RFC 4180 quoting for us, and it also gives blank-line skipping and physical line numbers for free.

```java
private static final List<String> REQUIRED_COLUMNS = List.of("name", "email", "phone", "birthDate");

private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
        .setHeader() // no-arg: read the header from the first record
        .get();
```

Behavior that matters (each one has a unit test):

| Behavior | Implementation detail |
|----------|----------------------|
| Header validation | Every name in `REQUIRED_COLUMNS` must exist in the header map — **exact, case-sensitive match; order-insensitive**; extra columns are tolerated |
| Empty file | `parser.getHeaderMap()` is empty → `BusinessException("CSV file is empty or has no header row")` |
| Wrong header | `BusinessException("CSV header must contain the columns: name, email, phone, birthDate")` |
| Blank/duplicate header names | Rejected by Commons CSV itself (`IllegalArgumentException`) → `BusinessException("Invalid CSV header: " + message)` |
| Malformed CSV (unclosed quote) | `CSVException` → `BusinessException("Malformed CSV file: " + message)` — **a client error (`400`), not a `500`** |
| Blank lines | Skipped by the library; they still count toward physical line numbers |
| Line numbers | `parser.getCurrentLineNumber()` captured per record → the `row` field in errors |
| Short rows | `record.isSet(column) ? value.trim() : null` — a row with only a name yields `null` fields instead of throwing, so validation reports them as normal row errors |
| Encoding | Explicit `UTF-8` via `InputStreamReader` — never the platform default |

Because every structural failure throws `BusinessException`, and `GlobalExceptionHandler` maps
`BusinessException` to a `400` `ProblemDetail` (`title: "Business Error"`, message in `detail`),
an unusable file never reaches persistence and never surfaces as a `500`.

### 4. Service orchestration — the key design point

```java
// Deliberately NOT @Transactional: each saveAndFlush runs in its own
// transaction, so a failing row never rolls back rows already imported.
public PatientImportResponse importPatients(InputStream csv) { ... }
```

**No `@Transactional` on the import method.** This is the single most important decision in the
feature, and it is a double isolation guarantee:

1. **No lost work.** `SimpleJpaRepository.saveAndFlush` is itself `@Transactional`, so with no
   outer transaction every row commits in its own transaction. Row 5 failing cannot roll back
   rows 1–4.
2. **No rollback poisoning.** If one big transaction wrapped the import, the first constraint
   violation would mark the transaction *rollback-only*; every later `saveAndFlush` would then
   blow up with a `TransactionSystemException` regardless of its own validity — one bad row would
   genuinely kill the whole file.

Per-row flow inside `importRow` (returns `null` on success, a `RowError` on failure):

1. **`birthDate` first** — blank → `"birthDate is required"`; `DateTimeParseException` from
   `LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)` → `"birthDate must be yyyy-MM-dd"`.
   This check runs *before* bean validation because the raw CSV string must become a `LocalDate`
   before `PatientCreationRequest` can even be constructed.
2. **Bean validation, reused.** The row is wrapped into the existing `PatientCreationRequest`
   and validated with an injected `jakarta.validation.Validator` — **no validation rules are
   duplicated**. The constraint messages (`"Name is mandatory"`, `"Email not valid"`, …) are the
   exact same strings the JSON endpoints return; multiple violations are joined with `", "`.
3. **Persist with `saveAndFlush`.** The flush matters: it forces the SQL insert (and therefore
   the unique-constraint check) to happen *now*, inside the row's own transaction, so the
   exception can be attributed to this row.
4. **Catch `DataIntegrityViolationException` per row** and map it through `duplicateMessage`:

| Constraint in the exception message | Row error message |
|-------------------------------------|-------------------|
| `uk_patient_email` | `The email already exists` |
| `uk_patient_phone` | `The phone already exists` |
| anything else | `Duplicate or invalid data` |
| other `RuntimeException` during save | `Row could not be imported` |

These constraints come straight from `@Table(uniqueConstraints = …)` on `PatientEntity`. The
same messages exist in `GlobalExceptionHandler` for the single-create endpoint — there they
return `409`, here they ride inside a `200` summary.

Finally: `new PatientImportResponse(rows.size(), imported, errors.size(), errors)`.

### 5. The controller endpoint

```java
@PostMapping("/import")
public ResponseEntity<PatientImportResponse> importPatients(
        @RequestParam(value = "file", required = false) MultipartFile file) {
    if (file == null || file.isEmpty()) {
        throw new BusinessException("CSV file is required");
    }
    try {
        return ResponseEntity.ok(this.patientService.importPatients(file.getInputStream()));
    } catch (IOException e) {
        throw new UncheckedIOException("Failed to read the uploaded file", e);
    }
}
```

The one subtle line is **`required = false`**. With the default (`required = true`), a request
without the `file` part is rejected during argument resolution with
`MissingServletRequestPartException`, before any of our code runs — and this project's generic
`Exception` handler turns that into a `500 Server Internal Error`. With `required = false` the
parameter arrives as `null`, our guard throws `BusinessException("CSV file is required")`, and
the client gets a clean `400` with `"detail": "CSV file is required"`. The `file.isEmpty()` check
additionally catches zero-byte uploads.

No `@Valid` is needed (there is no request body), and no security changes were required: the
endpoint falls under the default authenticated rule of `SecurityConfig`, so `401` comes for free.

## Testing strategy — three layers

| Layer | Test | What it proves | Spring context? |
|-------|------|----------------|-----------------|
| Pure unit | `PatientCsvParserTest` (6 tests) | Physical line numbers across blank lines, any column order, missing-column/empty-file/blank-header rejections, short rows → `null` | None — `new PatientCsvParser()` over a `ByteArrayInputStream` |
| MVC slice | `PatientControllerTest` (5 tests, `@WebMvcTest`) | Multipart happy path returns `total`/`imported`/`failed`/`errors[0].row`; missing file → `400` with `$.detail = "CSV file is required"` | Spring MVC + `GlobalExceptionHandler` only; `PatientService` and the JWT filter are `@MockitoBean`s, security filters disabled via `addFilters = false` |
| Full integration | `PatientApiIntegrationTest` (9 tests, Testcontainers) | Real JWT login, real PostgreSQL unique constraints, partial success against the actual database | Full app: `@SpringBootTest` on a random port, `@ServiceConnection` `postgres:14-alpine` container (shared JVM-wide), `jwt.secret` and `rate-limit.enabled=false` overridden per test class |

The integration layer does the heavy lifting:

- **Multipart via `TestRestTemplate`** — a `LinkedMultiValueMap` with a `ByteArrayResource` whose
  `getFilename()` is overridden to `patients.csv`. Gotcha worth remembering: send **fresh
  headers** with `MediaType.MULTIPART_FORM_DATA`; reusing the JSON auth headers breaks the
  multipart boundary.
- **JWT login first** — `POST /api/auth/login` with the seeded dev user, then
  `setBearerAuth(accessToken)` on every request.
- **Real constraint violation** — a patient is seeded, a CSV row collides with it, and the test
  asserts `200` with `"The email already exists"` at the right row while *the other row still
  imports* — the partial-success guarantee, proven against PostgreSQL, not a mock.
- **Negative paths** — wrong header → `400` whose `detail` contains `header`; no token → `401`.

**Observed result** (run on this branch with Docker available): `./mvnw test` →
**Tests run: 43, Failures: 0, Errors: 0, Skipped: 0.**

## Design decisions and tradeoffs

| Decision | Chosen | Tradeoff accepted |
|----------|--------|-------------------|
| Partial success vs all-or-nothing | `200` + `errors[]`; good rows commit | The client must inspect `failed`; an import is not atomic — but spreadsheet users expect "fix these 3 rows" semantics, not "fix all 5,000" |
| Transaction scope | Per-row (no outer `@Transactional`) | No all-or-nothing atomicity, and no cross-row consistency snapshot — in exchange, one bad row can never roll back or poison good rows |
| CSV parsing | Apache Commons CSV 1.14.1 | One extra (unmanaged) dependency — in exchange, correct quoting, blank-line handling, and line numbers instead of a fragile `split` |
| Error shape | Structural errors → `400` `ProblemDetail`; row errors → inside `200` | Two response shapes to handle on the client — in exchange, an unusable file fails loudly while a usable file always delivers its good rows |
| Exception type | Reuse the existing `BusinessException` (already mapped to `400`) | The `title` is the generic `"Business Error"` rather than something import-specific — in exchange, zero new exception plumbing |
| Parser wiring | Concrete `@Component` injected into the service (no application port interface) | A slight deviation from the strict `presentation → application → domain ← infrastructure` direction — pragmatic while there is exactly one parser/format |
| Create-only | Duplicates become errors, never updates | Re-running a file reports everything as duplicate; upsert is future work (see below) |

## Try it yourself

**1. Get a token** (dev-only seeded credentials — see `DataInitializer`):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"rosasdr","password":"Admin1234"}' \
  | python -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
```

**2. Save a sample `patients.csv`** (line 4 duplicates line 2's email on purpose):

```csv
name,email,phone,birthDate
Ada Lovelace,ada@example.com,555-0001,1990-01-15
Grace Hopper,grace@example.com,555-0002,1991-02-16
Ada Lovelace,ada@example.com,555-0003,1992-03-17
```

**3. Import it:**

```bash
curl -X POST http://localhost:8080/patients/import \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@patients.csv"
```

**4. The response — `200`, partial success:**

```json
{
  "total": 3,
  "imported": 2,
  "failed": 1,
  "errors": [
    { "row": 4, "message": "The email already exists" }
  ]
}
```

`row: 4` is the physical line number (header is line 1). Running the *same* file a second time
yields `imported: 0, failed: 3` — the import is create-only by design.

**Structural error example** — send no `file` part at all:

```json
{ "title": "Business Error", "status": 400, "detail": "CSV file is required" }
```

A wrong header instead returns
`"detail": "CSV header must contain the columns: name, email, phone, birthDate"`, and no rows
are imported.

## Summary checklist

- [ ] `commons-csv` pinned explicitly (Boot does not manage it)
- [ ] Multipart limits raised above Spring's 1MB default
- [ ] Response DTO reports `total` / `imported` / `failed` / `errors[]` with physical line numbers
- [ ] Header validated: exact, case-sensitive, order-insensitive columns
- [ ] Structural failures throw `BusinessException` → `400`, never `500`
- [ ] Bean validation reused from `PatientCreationRequest` via an injected `Validator`
- [ ] **No `@Transactional` on the import** — per-row commits, `saveAndFlush` for immediate constraint checks
- [ ] `DataIntegrityViolationException` caught per row and mapped to friendly duplicate messages
- [ ] `required = false` on the `file` parameter for a clean `400`
- [ ] Three test layers green: parser unit, `@WebMvcTest` slice, Testcontainers integration

## Where to go next

- **Async imports** — for very large files, hand the rows to a background job and expose a
  status endpoint; a synchronous request thread should not babysit 100k rows.
- **Dry-run mode** — parse and validate without persisting so users can preview row errors
  before committing.
- **Upsert mode** — match on `email` and update existing patients instead of create-only, which
  would also make re-runs idempotent.
- **Frontend upload UI** — the sibling repo `dentalcare-front` can add a drag-and-drop page that
  renders `errors[]` inline against a preview of the uploaded file.
- **Streaming parsing** — `parse()` currently materializes every row into a `List` first; for
  very large files, iterate the `CSVParser` lazily and cap memory.
- **A dedicated import exception** — if more structural error kinds appear, a subclass of
  `BusinessException` (or an application-layer parser port) would restore strict layering.
