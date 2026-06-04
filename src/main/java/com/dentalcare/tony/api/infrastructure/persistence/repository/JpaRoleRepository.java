package com.dentalcare.tony.api.infrastructure.persistence.repository;

import com.dentalcare.tony.api.domain.entity.Role;
import com.dentalcare.tony.api.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
