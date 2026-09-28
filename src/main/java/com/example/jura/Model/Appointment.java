package com.example.jura.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Check(constraints = "status in ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Patient is required")
    @Column(name = "patient_id", nullable = false)
    private Integer patientId;

    @NotNull(message = "Doctor is required")
    @Column(name = "doctor_id", nullable = false)
    private Integer doctorId;

    @NotNull(message = "Start time is required")
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @NotNull(message = "End time is required")
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Pattern(regexp = "^(PENDING|APPROVED|REJECTED|CANCELLED)$",
            message = "Status must be PENDING, APPROVED, REJECTED, or CANCELLED")
    @Column(nullable = false, length = 9)
    private String status;
}
