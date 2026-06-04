package com.dentalcare.tony.api.infrastructure.persistence.repository;

import com.dentalcare.tony.api.domain.entity.Permission;
import com.dentalcare.tony.api.domain.enums.PermissionName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(PermissionName name);
}
