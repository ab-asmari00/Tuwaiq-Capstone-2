package com.example.jura.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Appointment is required")
    @Column(name = "appointment_id", nullable = false, unique = true)
    private Integer appointmentId;

    @NotBlank(message = "Zoom meeting ID is required")
    @Size(max = 100, message = "Zoom meeting ID must not exceed 100 characters")
    @Column(name = "zoom_meeting_id", nullable = false, length = 100)
    private String zoomMeetingId;

    @NotBlank(message = "Join URL is required")
    @Size(max = 2048, message = "Join URL must not exceed 2048 characters")
    @Column(name = "join_url", nullable = false, length = 2048)
    private String joinUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
