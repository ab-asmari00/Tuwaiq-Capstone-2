package com.example.jura.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Check(constraints = "result_status in ('HIGH', 'MODERATE', 'NONE_IDENTIFIED', 'UNKNOWN')")
public class AiInteractionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "First item is required")
    @Column(name = "item_a_id", nullable = false)
    private Integer itemAId;

    @NotNull(message = "Second item is required")
    @Column(name = "item_b_id", nullable = false)
    private Integer itemBId;

    @NotBlank(message = "Result status is required")
    @Pattern(regexp = "^(HIGH|MODERATE|NONE_IDENTIFIED|UNKNOWN)$",
            message = "Result status must be HIGH, MODERATE, NONE_IDENTIFIED, or UNKNOWN")
    @Column(name = "result_status", nullable = false, length = 15)
    private String resultStatus;

    @Column(name = "explanation_ar", columnDefinition = "text")
    private String explanationAr;

    @Column(name = "advice_ar", columnDefinition = "text")
    private String adviceAr;

    @Column(name = "source_links_json", columnDefinition = "text")
    private String sourceLinksJson;

    @Size(max = 100, message = "Model name must not exceed 100 characters")
    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
}
