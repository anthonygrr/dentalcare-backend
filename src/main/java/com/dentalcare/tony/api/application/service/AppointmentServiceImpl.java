package com.dentalcare.tony.api.application.service;


import com.dentalcare.tony.api.domain.entity.AppointmentEntity;
import com.dentalcare.tony.api.domain.entity.DoctorEntity;
import com.dentalcare.tony.api.domain.entity.MedicalRecordEntity;
import com.dentalcare.tony.api.domain.entity.PatientEntity;
import com.dentalcare.tony.api.domain.enums.AppointmentStatus;
import com.dentalcare.tony.api.domain.filter.AppointmentFilter;
import com.dentalcare.tony.api.infrastructure.persistence.repository.AppointmentRepository;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaDoctorRepository;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaMedicalRecordRepository;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaPatientRepository;
import com.dentalcare.tony.api.infrastructure.persistence.specification.AppointmentSpecification;
import com.dentalcare.tony.api.presentation.dto.response.AppointmentResponse;
import com.dentalcare.tony.api.presentation.dto.request.AppointmentCreateRequest;
import com.dentalcare.tony.api.presentation.dto.request.AppointmentUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final JpaPatientRepository patientRepository;
    private final JpaDoctorRepository doctorRepository;
    private final JpaMedicalRecordRepository medicalRecordRepository;


    // GET ALL WITH PAGINATION + FILTER
    @Transactional
    public Page<AppointmentResponse> findAll(
            AppointmentFilter filter,
            Pageable pageable
    ) {

        return appointmentRepository.findAll(
                        AppointmentSpecification.byFilter(filter),
                        pageable
                )
                .map(this::mapToResponse);
    }

    // GET BY ID
    @Transactional
    public AppointmentResponse getAppointmentById(Long id) {

        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        return mapToResponse(appointment);
    }

    // CREATE
    @Transactional
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {

        PatientEntity patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        DoctorEntity doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        //validate Hour and date
        boolean exists = appointmentRepository
                .existsByDoctorIdAndDateAndTimeAndStatusNot(
                        doctor.getId(),
                        request.date(),
                        request.time(),
                        AppointmentStatus.CANCELLED
                );

        if (exists) {
            throw new RuntimeException(
                    "The doctor already has an appointment at this time"
            );
        }

        AppointmentEntity appointment = AppointmentEntity.builder()
                .date(request.date())
                .time(request.time())
                .procedure(request.procedure())
                .status(AppointmentStatus.SCHEDULED)
                .patient(patient)
                .doctor(doctor)
                .build();

        AppointmentEntity saved = appointmentRepository.save(appointment);

        return mapToResponse(saved);
    }

    // UPDATE
    @Override
    @Transactional
    public AppointmentResponse updateAppointment(AppointmentUpdateRequest request, Long id) {

        AppointmentEntity appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (request.date() != null) {
            appointment.setDate(request.date());
        }

        if (request.time() != null) {
            appointment.setTime(request.time());
        }

        if (request.procedure() != null) {
            appointment.setProcedure(request.procedure());
        }

        if (request.status() != null) {

            appointment.setStatus(request.status());

            // if the appointment has finished
            if (request.status() == AppointmentStatus.COMPLETED) {

                // avoid duplicates
                if (appointment.getMedicalRecord() == null) {

                    MedicalRecordEntity medicalRecord =
                            MedicalRecordEntity.builder()
                                    .date(LocalDate.now())
                                    .diagnosis(request.diagnosis())
                                    .treatment(request.treatment())
                                    .patient(appointment.getPatient())
                                    .appointment(appointment)
                                    .build();

                    medicalRecordRepository.save(medicalRecord);
                }
            }
        }

        AppointmentEntity updated = appointmentRepository.save(appointment);

        return mapToResponse(updated);
    }


    // DELETE
    @Transactional
    public void deleteAppointment(Long id) {
        if (id != null) {
            appointmentRepository.deleteById(id);
        }
    }

    // MAPPER
    private AppointmentResponse mapToResponse(
            AppointmentEntity appointment
    ) {

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getProcedure(),
                appointment.getStatus(),
                appointment.getPatient().getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getName()
        );
    }
}
