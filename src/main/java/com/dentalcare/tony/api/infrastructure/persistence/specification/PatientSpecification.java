package com.dentalcare.tony.api.infrastructure.persistence.specification;


import com.dentalcare.tony.api.domain.entity.PatientEntity;
import com.dentalcare.tony.api.domain.filter.PatientFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class PatientSpecification {

    public static Specification<PatientEntity> byFilter(PatientFilter filter) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter.name() != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + filter.name().toLowerCase() + "%"
                ));
            }

            if (filter.email() != null) {
                predicates.add(cb.equal(root.get("email"), filter.email()));
            }

            if (filter.phone() != null) {
                predicates.add(cb.equal(root.get("phone"), filter.phone()));
            }

            if (filter.birthDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("birthDate"),
                        filter.birthDateFrom()
                ));
            }

            if (filter.birthDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("birthDate"),
                        filter.birthDateTo()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
