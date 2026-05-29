package com.dentalcare.tony.api.domain.repository;

import com.dentalcare.tony.api.domain.entity.AppointmentEntity;
import com.dentalcare.tony.api.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long>,
        JpaSpecificationExecutor<AppointmentEntity> {
    boolean existsByDoctorIdAndDateAndTimeAndStatusNot(
            Long doctorId,
            LocalDate date,
            LocalTime time,
            AppointmentStatus status
    );
}
