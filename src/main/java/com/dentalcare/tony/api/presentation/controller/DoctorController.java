package com.dentalcare.tony.api.presentation.controller;

import com.dentalcare.tony.api.application.service.DoctorService;
import com.dentalcare.tony.api.domain.filter.DoctorFilter;
import com.dentalcare.tony.api.presentation.dto.response.DoctorResponse;
import com.dentalcare.tony.api.presentation.dto.request.DoctorCreationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public Page<DoctorResponse> getAll(
            @RequestParam(required = false) String name,
            Pageable pageable
    ){
        DoctorFilter filter = new DoctorFilter(name);

        return this.doctorService.findAll(filter, pageable);
    }

//    @GetMapping
//    public ResponseEntity<List<DoctorResponse>> getAll(){
//        return ResponseEntity.ok().body(doctorService.findAll());
//    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> findDoctorByDoctorId(@PathVariable Long id){
        return ResponseEntity.ok(this.doctorService.getDoctorById(id));
    }

    @PostMapping
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorCreationRequest request){
        return ResponseEntity.ok(this.doctorService.createDoctor(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorResponse> updateDoctor(@Valid @RequestBody DoctorCreationRequest request, @PathVariable Long id){
        return ResponseEntity.ok(this.doctorService.updateDoctor(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id){
        this.doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

}
