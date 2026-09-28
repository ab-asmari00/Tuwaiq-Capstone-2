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
public class DrugCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "SFDA registration number is required")
    @Size(max = 100, message = "SFDA registration number must not exceed 100 characters")
    @Column(name = "sfda_reg_no", nullable = false, unique = true, length = 100)
    private String sfdaRegNo;

    @Size(max = 255, message = "Arabic trade name must not exceed 255 characters")
    @Column(name = "trade_name_ar", length = 255)
    private String tradeNameAr;

    @Size(max = 255, message = "English trade name must not exceed 255 characters")
    @Column(name = "trade_name_en", length = 255)
    private String tradeNameEn;

    @Size(max = 255, message = "Arabic search name must not exceed 255 characters")
    @Column(name = "search_name_ar", length = 255)
    private String searchNameAr;

    @Column(name = "scientific_name_raw", columnDefinition = "text")
    private String scientificNameRaw;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
}
