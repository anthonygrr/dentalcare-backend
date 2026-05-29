package com.dentalcare.tony.api.presentation.controller;


import com.dentalcare.tony.api.application.service.MedicalRecordService;
import com.dentalcare.tony.api.application.service.MedicalRecordServiceImpl;
import com.dentalcare.tony.api.domain.filter.MedicalRecordFilter;
import com.dentalcare.tony.api.presentation.dto.response.MedicalRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping
    public Page<MedicalRecordResponse> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String diagnosis,

            Pageable pageable
    ){

        // Order by name ascending
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("date").descending()
            );
        }

        MedicalRecordFilter filter = new MedicalRecordFilter(
                name,
                diagnosis
        );

        return this.medicalRecordService.findAll(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok().body(medicalRecordService.getById(id));
    }
}
