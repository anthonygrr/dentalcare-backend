package com.dentalcare.tony.api.infrastructure.persistence.specification;

import com.dentalcare.tony.api.domain.entity.DoctorEntity;
import com.dentalcare.tony.api.domain.filter.DoctorFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class DoctorSpecification {

    public static Specification<DoctorEntity> byFilter(DoctorFilter filter){
        return (root, query, cb) ->{
            List<Predicate> predicates = new ArrayList<>();
            if (filter.name() != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" +filter.name().toLowerCase() + "%"
                ));
            }
            return  cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
