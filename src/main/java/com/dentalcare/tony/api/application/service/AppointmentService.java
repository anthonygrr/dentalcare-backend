package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.filter.AppointmentFilter;
import com.dentalcare.tony.api.presentation.dto.response.AppointmentResponse;
import com.dentalcare.tony.api.presentation.dto.resquest.AppointmentCreateRequest;
import com.dentalcare.tony.api.presentation.dto.resquest.AppointmentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentService {
    Page<AppointmentResponse> findAll(AppointmentFilter filter, Pageable pageable);
    AppointmentResponse getAppointmentById(Long id);
    AppointmentResponse createAppointment(AppointmentCreateRequest request);
    AppointmentResponse updateAppointment(AppointmentUpdateRequest request, Long id);
    void deleteAppointment(Long id);
}
