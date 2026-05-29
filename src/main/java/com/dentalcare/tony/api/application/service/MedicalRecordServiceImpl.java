package com.dentalcare.tony.api.application.service;


import com.dentalcare.tony.api.domain.entity.AppointmentEntity;
import com.dentalcare.tony.api.domain.entity.MedicalRecordEntity;
import com.dentalcare.tony.api.domain.filter.MedicalRecordFilter;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaMedicalRecordRepository;
import com.dentalcare.tony.api.infrastructure.persistence.specification.MedicalRecordSpecification;
import com.dentalcare.tony.api.presentation.dto.response.MedicalRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService{

    private final JpaMedicalRecordRepository medicalRecordRepository;

    @Transactional
    public Page<MedicalRecordResponse> findAll(MedicalRecordFilter filter, Pageable pageable){
        return medicalRecordRepository.findAll(
                MedicalRecordSpecification.byFilter(filter),
                pageable
        ).map(this::mapToResponse);
    }

    // GET BY ID
    public MedicalRecordResponse getById(Long id) {

        MedicalRecordEntity medicalRecord =
                medicalRecordRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Medical record not found"));

        return mapToResponse(medicalRecord);
    }

    // MAPPER
    private MedicalRecordResponse mapToResponse(
            MedicalRecordEntity medicalRecord
    ) {

        AppointmentEntity appointment =
                medicalRecord.getAppointment();

        return new MedicalRecordResponse(

                // medical record
                medicalRecord.getId(),
                medicalRecord.getDate(),
                medicalRecord.getDiagnosis(),
                medicalRecord.getTreatment(),

                // patient
                medicalRecord.getPatient().getId(),
                medicalRecord.getPatient().getName(),

                // appointment
                appointment.getId(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getProcedure(),
                appointment.getStatus(),

                // doctor
                appointment.getDoctor().getName()
        );
    }
}
