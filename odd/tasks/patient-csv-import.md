# Feature: Patient CSV Import + Blog Guide

## Objective

Add `POST /patients/import`, an endpoint that accepts a `multipart/form-data` CSV file containing new patients, imports valid rows with row-level error reporting, and write a blog-style Markdown guide documenting how the feature was implemented.

## Problem / Why

Creating patients one HTTP call at a time does not scale when a clinic migrates from spreadsheets. A CSV bulk import removes the manual entry bottleneck. The accompanying guide documents the real implementation for a technical blog audience.

## Scope

**In scope**

- `POST /patients/import` (authenticated) accepting a CSV file.
- CSV parsing with a real library (Apache Commons CSV), never hand-rolled string splitting.
- Per-row validation reusing `PatientCreationRequest` constraints.
- Partial-success semantics: invalid/duplicate rows are reported, valid rows are imported.
- Summary response DTO with counts and row-level errors.
- Tests: parser unit test, controller slice test, Testcontainers integration test.
- Blog guide Markdown document under `docs/blog/`.

**Out of scope**

- Updating existing patients from CSV (import creates only).
- Async/background import jobs, progress polling, or CSV export.
- Frontend UI (sibling repo `dentalcare-front`).
- Changing existing patient endpoints or the inconsistent route prefixes.

## Constraints & Decisions

| Topic | Decision |
|-------|----------|
| CSV columns | Header required, exact names: `name,email,phone,birthDate` |
| Date format | ISO `yyyy-MM-dd` (matches the API's `@DateTimeFormat(iso = DATE)`) |
| Library | `org.apache.commons:commons-csv` (current managed version from Maven Central) |
| Validation | Reuse `PatientCreationRequest` bean-validation constraints via injected `jakarta.validation.Validator`; do not duplicate rules |
| Duplicates | Unique `email`/`phone` constraints (`uk_patient_email`, `uk_patient_phone`) caught per row as `DataIntegrityViolationException` → row error, other rows continue |
| Transactionality | No outer `@Transactional` on the import method so each `repository.save` runs in its own transaction; a failing row must not roll back successful rows |
| Layering | Parsing in a dedicated component; orchestration in `PatientServiceImpl`; controller stays thin (project convention) |
| Response | `200` with summary (`total`, `imported`, `failed`, `errors[]`); `400` only for structurally unusable files (missing file, missing/wrong header) |
| Auth | Endpoint inherits the default authenticated rule; general `api` rate-limit bucket (not `login`) |
| TDD | Off (source: no strict-TDD config in project/session; frameworks present do not enable it). Ordinary functional checks. Runner: `./mvnw test` (requires Docker for Testcontainers) |
| RDD | Off (global). No native review ceremony; ordinary checks only. |

## Delivery

- **Forecast (authored changed lines, generated excluded):** ~750
  - Implementation: ~200
  - Tests: ~250
  - Blog guide MD: ~300
- **Budget heuristic:** ~400 authored lines per work unit → forecast exceeds it.
- **Strategy (user decision, recorded):** no PRs. Local feature branch only — the user handles PR delivery themselves. The 400-line budget therefore governs the user's own future PR split, not this session.
- **Branch:** `feature/patient-csv-import` (created from `main`).

## Tasks

| ID | Task | Route | Trigger evidence |
|----|------|-------|------------------|
| T1 | Add `commons-csv` dependency to `pom.xml` | delegated | part of writer batch |
| T2 | Create import response DTOs (`PatientImportResponse` + row error type) | delegated | part of writer batch |
| T3 | Create CSV parser component (header check, row mapping, blank-line skip) | delegated | part of writer batch |
| T4 | Extend `PatientService` / `PatientServiceImpl` with import orchestration | delegated | part of writer batch |
| T5 | Add `POST /patients/import` to `PatientController` | delegated | part of writer batch |
| T6 | Tests: parser unit, controller slice, integration (happy path, bad rows, duplicates, bad header, 401) | delegated | part of writer batch |
| T7 | Run `./mvnw test` and record observed results | delegated (writer) + parent spot check | per-action rule |
| T8 | Work-unit commit(s) on the feature branch, Conventional Commit | inline (parent) | git state command |
| T9 | Write blog guide `docs/blog/patient-csv-import.md` from the real diff | delegated | reading prepares a write |
| T10 | Docs commit | inline (parent) | git state command |

**Writer-batch rationale (T1–T6):** 6 non-trivial files across 4 packages → mandatory writer trigger; one bounded writer keeps the parent context thin.

## Acceptance Criteria

- [ ] `POST /patients/import` with a valid CSV returns `200` with accurate `total`/`imported`/`failed` counts.
- [ ] Rows failing bean validation are reported with row number + message; valid rows still import (partial success).
- [ ] Rows duplicating an existing or in-file `email`/`phone` become row errors, not a `500` and not a full rollback.
- [ ] Missing file, missing header, or wrong header → `400` with a clear message.
- [ ] Unauthenticated call → `401` (covered by the integration suite).
- [ ] Existing behavior unchanged: full `./mvnw test` green with Docker running.
- [ ] Blog guide exists at `docs/blog/patient-csv-import.md` and documents the actual implemented design (not a hypothetical one).

## Progress

- [x] Exploration (CodeGraph + DTO/entity/exception/tests read)
- [ ] T1–T6 implemented
- [ ] T7 verification observed
- [ ] T8 code commit(s)
- [ ] T9 blog guide
- [ ] T10 docs commit

## Verification evidence

(pending — record `<command>: <result>` per check after T7)
