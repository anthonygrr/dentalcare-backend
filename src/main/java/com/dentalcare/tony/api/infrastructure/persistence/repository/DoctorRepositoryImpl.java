package com.dentalcare.tony.api.infrastructure.persistence.repository;

import com.dentalcare.tony.api.domain.entity.DoctorEntity;
import com.dentalcare.tony.api.domain.filter.DoctorFilter;
import com.dentalcare.tony.api.domain.repository.DoctorRepository;
import com.dentalcare.tony.api.infrastructure.persistence.specification.DoctorSpecification;
import com.dentalcare.tony.api.infrastructure.persistence.specification.PatientSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DoctorRepositoryImpl implements DoctorRepository {

    private final JpaDoctorRepository doctorRepository;

    @Override
    public List<DoctorEntity> findAllWithFilters(DoctorFilter filter) {
        return doctorRepository.findAll(
                DoctorSpecification.byFilter(filter)
        );
    }

    @Override
    public DoctorEntity save(DoctorEntity doctor) {
        return doctorRepository.save(doctor);
    }

    @Override
    public Optional<DoctorEntity> findById(Long id) {
        return doctorRepository.findById(id);
    }

    @Override
    public List<DoctorEntity> findAll() {
        return  doctorRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        doctorRepository.deleteById(id);
    }
}
