package com.dentalcare.tony.api.presentation.controller;


import com.dentalcare.tony.api.application.service.AppointmentService;
import com.dentalcare.tony.api.application.service.AppointmentServiceImpl;
import com.dentalcare.tony.api.domain.enums.AppointmentStatus;
import com.dentalcare.tony.api.domain.filter.AppointmentFilter;


import com.dentalcare.tony.api.presentation.dto.response.AppointmentResponse;

import com.dentalcare.tony.api.presentation.dto.request.AppointmentCreateRequest;
import com.dentalcare.tony.api.presentation.dto.request.AppointmentUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public Page<AppointmentResponse> getAll(

            @RequestParam(required = false)
            AppointmentStatus status,

            @RequestParam(required = false)
            LocalDate date,

            @RequestParam(required = false)
            String procedure,

            @RequestParam(required = false)
            String name,

            Pageable pageable
    ) {

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("date").descending()
            );
        }

        AppointmentFilter filter = new AppointmentFilter(
                status,
                date,
                procedure,
                name
        );

        return appointmentService.findAll(filter, pageable);
    }


    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @RequestBody AppointmentCreateRequest request
    ) {

        return ResponseEntity.ok().body(appointmentService.createAppointment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable Long id,
            @RequestBody AppointmentUpdateRequest request
    ) {

        return ResponseEntity.ok().body(appointmentService.updateAppointment(request, id));
    }


    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok().body(appointmentService.getAppointmentById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }
}