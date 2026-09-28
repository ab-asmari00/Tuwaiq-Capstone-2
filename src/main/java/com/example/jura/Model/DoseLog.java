package com.example.jura.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_dose_log_schedule_due", columnNames = {"schedule_id", "due_at"}))
@Check(constraints = "status in ('TAKEN', 'SKIPPED', 'MISSED')")
public class DoseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Schedule is required")
    @Column(name = "schedule_id", nullable = false)
    private Integer scheduleId;

    @NotNull(message = "Due time is required")
    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(TAKEN|SKIPPED|MISSED)$",
            message = "Status must be TAKEN, SKIPPED, or MISSED")
    @Column(nullable = false, length = 7)
    private String status;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;
}
