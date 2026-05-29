package com.dentalcare.tony.api.domain.entity;

import com.dentalcare.tony.api.domain.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    private LocalDateTime scheduledAt;
//    private LocalDateTime startedAt;
//    private LocalDateTime endedAt;
    private LocalDate date;
    private LocalTime time;

    private String procedure;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    // 🔹 MANY TO ONE (muchas citas → un paciente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    // 🔹 MANY TO ONE (muchas citas → un doctor)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorEntity doctor;

    // 👇 relación inversa opcional
    @OneToOne(mappedBy = "appointment")
    private MedicalRecordEntity medicalRecord;
}
