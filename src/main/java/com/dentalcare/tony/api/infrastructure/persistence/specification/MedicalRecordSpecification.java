package com.dentalcare.tony.api.infrastructure.persistence.specification;


import com.dentalcare.tony.api.domain.entity.AppointmentEntity;
import com.dentalcare.tony.api.domain.entity.MedicalRecordEntity;
import com.dentalcare.tony.api.domain.entity.PatientEntity;
import com.dentalcare.tony.api.domain.filter.AppointmentFilter;
import com.dentalcare.tony.api.domain.filter.MedicalRecordFilter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class MedicalRecordSpecification {

    public static Specification<MedicalRecordEntity> byFilter(
            MedicalRecordFilter filter
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // JOIN con patient
            Join<MedicalRecordEntity, PatientEntity> patientJoin =
                    root.join("patient");


            if (filter.diagnosis() != null
                    && !filter.diagnosis().isBlank()) {

                predicates.add(
                        cb.like(
                                cb.lower(root.get("diagnosis")),
                                "%" + filter.diagnosis().toLowerCase() + "%"
                        )
                );
            }

            // nombre del paciente
            if (filter.name() != null
                    && !filter.name().isBlank()) {

                predicates.add(
                        cb.like(
                                cb.lower(patientJoin.get("name")),
                                "%" + filter.name().toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}