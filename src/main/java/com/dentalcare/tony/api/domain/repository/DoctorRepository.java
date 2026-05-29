package com.dentalcare.tony.api.domain.repository;

import com.dentalcare.tony.api.domain.entity.DoctorEntity;
import com.dentalcare.tony.api.domain.filter.DoctorFilter;
import com.dentalcare.tony.api.presentation.dto.response.DoctorResponse;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository {

    List<DoctorEntity> findAllWithFilters(DoctorFilter filter);

    DoctorEntity save(DoctorEntity doctor);

    Optional<DoctorEntity> findById(Long id);


    List<DoctorEntity> findAll();

    void deleteById(Long id);
}
