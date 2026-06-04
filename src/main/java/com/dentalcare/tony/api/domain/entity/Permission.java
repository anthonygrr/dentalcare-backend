package com.dentalcare.tony.api.domain.entity;

import com.dentalcare.tony.api.domain.enums.PermissionName;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PermissionName name;
}
