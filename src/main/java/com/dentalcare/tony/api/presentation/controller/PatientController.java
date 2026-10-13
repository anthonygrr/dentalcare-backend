package com.dentalcare.tony.api.presentation.controller;

import com.dentalcare.tony.api.application.service.PatientService;
import com.dentalcare.tony.api.domain.filter.PatientFilter;
import com.dentalcare.tony.api.presentation.dto.response.PatientResponse;
import com.dentalcare.tony.api.presentation.dto.request.PatientCreationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Patients")
@RestController
@RequestMapping("/patients")
@CrossOrigin(origins = "http://localhost:8080")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    @GetMapping
    public Page<PatientResponse> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate birthDateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate birthDateTo,

            Pageable pageable
    ){

        // Order by name ascending
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("name").ascending()
            );
        }

        PatientFilter filter = new PatientFilter(
                name,
                email,
                phone,
                birthDateFrom,
                birthDateTo
        );

        return this.patientService.findAll(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> findPatientByPatientId(@PathVariable Long id){
        return ResponseEntity.ok(this.patientService.getPatientById(id));
    }

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientCreationRequest request){
        return ResponseEntity.ok(this.patientService.createPatient(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> updatePatient(@Valid @RequestBody PatientCreationRequest request, @PathVariable Long id){
        return ResponseEntity.ok(this.patientService.updatePatient(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id){
        this.patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

}
