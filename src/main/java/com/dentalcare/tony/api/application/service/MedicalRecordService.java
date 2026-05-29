package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.filter.MedicalRecordFilter;
import com.dentalcare.tony.api.presentation.dto.response.MedicalRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicalRecordService {

    Page<MedicalRecordResponse> findAll(MedicalRecordFilter filter, Pageable pageable);
    MedicalRecordResponse getById(Long id);

}
