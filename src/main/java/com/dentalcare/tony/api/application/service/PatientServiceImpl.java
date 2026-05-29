package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.filter.PatientFilter;
import com.dentalcare.tony.api.presentation.dto.request.PatientCreationRequest;
import com.dentalcare.tony.api.presentation.dto.response.PatientResponse;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaPatientRepository;
import com.dentalcare.tony.api.infrastructure.persistence.specification.PatientSpecification;

import com.dentalcare.tony.api.domain.entity.PatientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final JpaPatientRepository patientRepository;

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
