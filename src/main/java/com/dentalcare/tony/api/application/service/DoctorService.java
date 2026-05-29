package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.filter.DoctorFilter;
import com.dentalcare.tony.api.presentation.dto.response.DoctorResponse;
import com.dentalcare.tony.api.presentation.dto.resquest.DoctorCreationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DoctorService {
    Page<DoctorResponse> findAll(DoctorFilter filter, Pageable pageable);
    DoctorResponse getDoctorById(Long id);
    DoctorResponse createDoctor(DoctorCreationRequest request);
    DoctorResponse updateDoctor(DoctorCreationRequest request, Long id);
    void deleteDoctor(Long id);
}
