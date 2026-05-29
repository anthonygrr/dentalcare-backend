package com.dentalcare.tony.api.infrastructure.persistence.repository;

import com.dentalcare.tony.api.domain.entity.PatientEntity;
import com.dentalcare.tony.api.domain.filter.PatientFilter;
import com.dentalcare.tony.api.domain.repository.PatientRepository;
import com.dentalcare.tony.api.infrastructure.persistence.specification.PatientSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PatientRepositoryImpl implements PatientRepository {

    private final JpaPatientRepository patientRepository;

    @Override
    public List<PatientEntity> findAllWithFilters(PatientFilter filter) {
        return patientRepository.findAll(
                PatientSpecification.byFilter(filter)
        );
    }

    @Override
    public PatientEntity save(PatientEntity patient) {
        return patientRepository.save(patient);
    }

    @Override
    public Optional<PatientEntity> findById(Long id) {
        return patientRepository.findById(id);
    }

    @Override
    public List<PatientEntity> findAll() {
        return patientRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        patientRepository.deleteById(id);
    }

}
