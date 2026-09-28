package com.example.jura.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Check(constraints = "type in ('DRUG', 'SUPPLEMENT')")
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "User is required")
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(DRUG|SUPPLEMENT)$",
            message = "Type must be DRUG or SUPPLEMENT")
    @Column(nullable = false, length = 10)
    private String type;

    @Size(max = 255, message = "Display name must not exceed 255 characters")
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "drug_cache_id")
    private Integer drugCacheId;

    @Size(max = 255, message = "Dosage must not exceed 255 characters")
    @Column(name = "dosage_text", length = 255)
    private String dosageText;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}
