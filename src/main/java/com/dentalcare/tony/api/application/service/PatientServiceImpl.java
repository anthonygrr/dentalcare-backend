package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.filter.PatientFilter;
import com.dentalcare.tony.api.infrastructure.csv.PatientCsvParser;
import com.dentalcare.tony.api.infrastructure.csv.PatientCsvRow;
import com.dentalcare.tony.api.presentation.dto.request.PatientCreationRequest;
import com.dentalcare.tony.api.presentation.dto.response.PatientImportResponse;
import com.dentalcare.tony.api.presentation.dto.response.PatientResponse;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaPatientRepository;
import com.dentalcare.tony.api.infrastructure.persistence.specification.PatientSpecification;

import com.dentalcare.tony.api.domain.entity.PatientEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final JpaPatientRepository patientRepository;
    private final PatientCsvParser patientCsvParser;
    private final Validator validator;

    @Transactional
    public Page<PatientResponse> findAll(PatientFilter filter, Pageable pageable){
        return patientRepository.findAll(
                PatientSpecification.byFilter(filter),
                pageable
        ).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return this.patientRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        PatientEntity entity = this.patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("patient found"));
        return mapToResponse(entity);
    }

    @Transactional
    public PatientResponse createPatient(PatientCreationRequest request) {
        PatientEntity entity = mapToEntity(request);
        return mapToResponse(patientRepository.save(entity));
    }

    @Transactional
    public PatientResponse updatePatient(PatientCreationRequest request, Long id) {
        PatientEntity entity = this.patientRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setBirthDate(request.birthDate());

        return mapToResponse(this.patientRepository.save(entity));
    }

    @Transactional
    public void deletePatient(Long id) {
        if (!this.patientRepository.existsById(id)) {
            throw new IllegalArgumentException("Patient not exists");
        }
        this.patientRepository.deleteById(id);
    }

    /**
     * Imports every valid CSV row and reports the rest as row errors.
     *
     * <p>Deliberately NOT {@code @Transactional}: each {@code saveAndFlush} runs in its
     * own transaction, so a failing row never rolls back rows already imported.</p>
     */
    public PatientImportResponse importPatients(InputStream csv) {
        List<PatientCsvRow> rows = patientCsvParser.parse(csv);

        int imported = 0;
        List<PatientImportResponse.RowError> errors = new ArrayList<>();

        for (PatientCsvRow row : rows) {
            PatientImportResponse.RowError error = importRow(row);
            if (error == null) {
                imported++;
            } else {
                errors.add(error);
            }
        }

        return new PatientImportResponse(rows.size(), imported, errors.size(), errors);
    }

    /**
     * Imports one row; returns {@code null} on success or the row error.
     */
    private PatientImportResponse.RowError importRow(PatientCsvRow row) {
        LocalDate birthDate;
        if (row.birthDate() == null || row.birthDate().isBlank()) {
            return new PatientImportResponse.RowError(row.row(), "birthDate is required");
        }
        try {
            birthDate = LocalDate.parse(row.birthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return new PatientImportResponse.RowError(row.row(), "birthDate must be yyyy-MM-dd");
        }

        PatientCreationRequest request = new PatientCreationRequest(
                row.name(), row.email(), row.phone(), birthDate);

        Set<ConstraintViolation<PatientCreationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            return new PatientImportResponse.RowError(row.row(), message);
        }

        try {
            patientRepository.saveAndFlush(mapToEntity(request));
            return null;
        } catch (DataIntegrityViolationException e) {
            return new PatientImportResponse.RowError(row.row(), duplicateMessage(e));
        } catch (RuntimeException e) {
            return new PatientImportResponse.RowError(row.row(), "Row could not be imported");
        }
    }

    private String duplicateMessage(DataIntegrityViolationException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("uk_patient_email")) {
            return "The email already exists";
        }
        if (message.contains("uk_patient_phone")) {
            return "The phone already exists";
        }
        return "Duplicate or invalid data";
    }


    public PatientResponse mapToResponse(PatientEntity patient) {

        if (patient == null) return null;

        return new PatientResponse(
                patient.getId(),
                patient.getName(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getBirthDate()
        );

    }

    public PatientEntity mapToEntity(PatientCreationRequest request) {

        if (request == null) return null;

        PatientEntity entity = new PatientEntity();
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setBirthDate(request.birthDate());
        return entity;
    }


}
