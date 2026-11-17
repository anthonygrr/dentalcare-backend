package com.dentalcare.tony.api.application.service;


import com.dentalcare.tony.api.domain.filter.PatientFilter;
import com.dentalcare.tony.api.presentation.dto.response.PatientImportResponse;
import com.dentalcare.tony.api.presentation.dto.response.PatientResponse;
import com.dentalcare.tony.api.presentation.dto.request.PatientCreationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;

public interface PatientService {

    Page<PatientResponse> findAll(PatientFilter filter, Pageable pageable);
    PatientResponse getPatientById(Long id);
    PatientResponse createPatient(PatientCreationRequest request);
    PatientResponse updatePatient(PatientCreationRequest request, Long id);
    void deletePatient(Long id);
    PatientImportResponse importPatients(InputStream csv);

}
