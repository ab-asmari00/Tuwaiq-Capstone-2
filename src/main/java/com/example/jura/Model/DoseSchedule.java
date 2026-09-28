package com.example.jura.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class DoseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @NotNull(message = "Local time is required")
    @Column(name = "local_time", nullable = false)
    private LocalTime localTime;

    @NotBlank(message = "Days of week are required")
    @Size(max = 100, message = "Days of week must not exceed 100 characters")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)(,(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY))*$",
            message = "Use uppercase day names separated by commas, without spaces")
    @Column(name = "days_of_week", nullable = false, length = 100)
    private String daysOfWeek;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
