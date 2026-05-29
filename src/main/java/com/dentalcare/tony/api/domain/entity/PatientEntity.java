package com.dentalcare.tony.api.domain.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name = "patients", uniqueConstraints = {
        @UniqueConstraint(name = "uk_patient_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_patient_phone", columnNames = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEntity {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;

    // 🔹 ONE TO MANY
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<AppointmentEntity> appointments = new ArrayList<>();


}
