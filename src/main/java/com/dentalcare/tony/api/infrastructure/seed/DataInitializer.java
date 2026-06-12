package com.dentalcare.tony.api.infrastructure.seed;

import com.dentalcare.tony.api.domain.entity.Permission;
import com.dentalcare.tony.api.domain.entity.Role;
import com.dentalcare.tony.api.domain.entity.User;
import com.dentalcare.tony.api.domain.enums.PermissionName;
import com.dentalcare.tony.api.domain.enums.RoleName;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaPermissionRepository;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaRoleRepository;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JpaUserRepository userRepository;
    private final JpaRoleRepository roleRepository;
    private final JpaPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        createPermissions();

        Role adminRole = createAdminRole();

        createDoctorUser(adminRole);

        log.info("Security seed completed");
    }

    private void createPermissions() {

        Arrays.stream(PermissionName.values())
                .forEach(permissionName -> {

                    boolean exists = permissionRepository
                            .findByName(permissionName)
                            .isPresent();

                    if (!exists) {

                        Permission permission = Permission.builder()
                                .name(permissionName)
                                .build();

                        permissionRepository.save(permission);

                        log.info(
                                "Permission created: {}",
                                permissionName
                        );
                    }
                });
    }

    private Role createAdminRole() {

        return roleRepository
                .findByName(RoleName.ADMIN)
                .orElseGet(() -> {

                    Set<Permission> permissions =
                            new HashSet<>(
                                    permissionRepository.findAll()
                            );

                    Role role = Role.builder()
                            .name(RoleName.ADMIN)
                            .permissions(permissions)
                            .build();

                    roleRepository.save(role);

                    log.info("Role ADMIN created");

                    return role;
                });
    }

    private void createDoctorUser(Role adminRole) {

        boolean exists = userRepository
                .findByUsernameOrEmail(
                        "doctor",
                        "doctor@clinic.com"
                )
                .isPresent();

        if (exists) {
            return;
        }

        User doctor = User.builder()
                .fullName("Dr. Rosas")
                .username("rosasdr")
                .email("doctor@clinic.com")
                .password(
                        passwordEncoder.encode("Admin1234")
                )
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(doctor);

        log.info("Doctor user created");
        log.info("username: rosasdr");
        log.info("email: doctor@clinic.com");
        log.info("password: Admin1234");
    }
}
