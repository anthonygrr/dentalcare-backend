package com.dentalcare.tony.api.infrastructure.persistence.repository;

import com.dentalcare.tony.api.domain.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaMedicalRecordRepository extends
        JpaRepository<MedicalRecordEntity, Long>,
        JpaSpecificationExecutor<MedicalRecordEntity> {
}
