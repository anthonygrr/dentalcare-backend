package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.entity.DoctorEntity;
import com.dentalcare.tony.api.domain.entity.PatientEntity;
import com.dentalcare.tony.api.domain.filter.DoctorFilter;
import com.dentalcare.tony.api.domain.filter.PatientFilter;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaDoctorRepository;
import com.dentalcare.tony.api.infrastructure.persistence.specification.DoctorSpecification;
import com.dentalcare.tony.api.infrastructure.persistence.specification.PatientSpecification;
import com.dentalcare.tony.api.presentation.dto.response.DoctorResponse;
import com.dentalcare.tony.api.presentation.dto.response.PatientResponse;
import com.dentalcare.tony.api.presentation.dto.request.DoctorCreationRequest;
import com.dentalcare.tony.api.presentation.dto.request.PatientCreationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.print.Doc;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final JpaDoctorRepository doctorRepository;

    @Transactional
    public Page<DoctorResponse> findAll(DoctorFilter filter, Pageable pageable){
        return doctorRepository.findAll(
                DoctorSpecification.byFilter(filter),
                pageable
        ).map(this::mapToResponse);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public List<DoctorResponse> findAll() {
//        return this.doctorRepository.findAll()
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }


    @Override
    @Transactional
    public DoctorResponse getDoctorById(Long id) {
        DoctorEntity entity = this.doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("doctor found"));
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorCreationRequest request) {
        DoctorEntity entity = mapToEntity(request);
        return mapToResponse(doctorRepository.save(entity));
    }

    @Override
    public DoctorResponse updateDoctor(DoctorCreationRequest request, Long id) {
        DoctorEntity entity = this.doctorRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("doctor not found"));

        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setAddress(request.address());
        entity.setSpecialty(request.specialty());

        return mapToResponse(this.doctorRepository.save(entity));
    }

    @Override
    public void deleteDoctor(Long id) {
        if (!this.doctorRepository.existsById(id)) {
            throw new IllegalArgumentException("Doctor not exists");
        }
        this.doctorRepository.deleteById(id);
    }

    public DoctorResponse mapToResponse(DoctorEntity doctor) {

        if (doctor == null) return null;

        return new DoctorResponse(
                doctor.getId(),
                doctor.getName(),
                doctor.getEmail(),
                doctor.getPhone(),
                doctor.getAddress(),
                doctor.getSpecialty()
        );

    }

    public DoctorEntity mapToEntity(DoctorCreationRequest request) {

        if (request == null) return null;

        DoctorEntity entity = new DoctorEntity();
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setAddress(request.address());
        entity.setSpecialty(request.specialty());
        return entity;
    }


}
