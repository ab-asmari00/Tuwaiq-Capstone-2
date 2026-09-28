package com.example.jura.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class DoctorProfile {

    @Id
    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @NotBlank(message = "Specialty is required")
    @Size(max = 100, message = "Specialty must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String specialty;

    @Column(columnDefinition = "text")
    private String bio;
}
