package com.dentalcare.tony.api.infrastructure.persistence.repository;

import com.dentalcare.tony.api.domain.entity.AppointmentEntity;
import com.dentalcare.tony.api.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long>,
        JpaSpecificationExecutor<AppointmentEntity> {
    boolean existsByDoctorIdAndDateAndTimeAndStatusNot(
            Long doctorId,
            LocalDate date,
            LocalTime time,
            AppointmentStatus status
    );
}
