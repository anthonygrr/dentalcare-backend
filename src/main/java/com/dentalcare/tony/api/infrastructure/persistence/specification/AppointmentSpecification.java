package com.dentalcare.tony.api.infrastructure.persistence.specification;


import com.dentalcare.tony.api.domain.entity.AppointmentEntity;
import com.dentalcare.tony.api.domain.entity.PatientEntity;
import com.dentalcare.tony.api.domain.filter.AppointmentFilter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
public class AppointmentSpecification {

    public static Specification<AppointmentEntity> byFilter(
            AppointmentFilter filter
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // JOIN con patient
            Join<AppointmentEntity, PatientEntity> patientJoin =
                    root.join("patient");

            if (filter.status() != null) {
                predicates.add(
                        cb.equal(
                                root.get("status"),
                                filter.status()
                        )
                );
            }

            if (filter.procedure() != null
                    && !filter.procedure().isBlank()) {

                predicates.add(
                        cb.like(
                                cb.lower(root.get("procedure")),
                                "%" + filter.procedure().toLowerCase() + "%"
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

            if (filter.date() != null) {

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("date"),
                                filter.date()
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}