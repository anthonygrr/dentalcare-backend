package com.dentalcare.tony.api.domain.repository;

import com.dentalcare.tony.api.domain.entity.PatientEntity;
import com.dentalcare.tony.api.domain.filter.PatientFilter;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    List<PatientEntity> findAllWithFilters(PatientFilter filter);

    PatientEntity save(PatientEntity patient);

    Optional<PatientEntity> findById(Long id);

    List<PatientEntity> findAll();

    void deleteById(Long id);
}
