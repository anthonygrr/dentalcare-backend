package com.dentalcare.tony.api.domain.entity;

import com.dentalcare.tony.api.domain.enums.Specialty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_doctor_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_doctor_phone", columnNames = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private Specialty specialty;
}
