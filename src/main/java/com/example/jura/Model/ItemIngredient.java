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
public class ItemIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @NotBlank(message = "English ingredient name is required")
    @Size(max = 255, message = "English ingredient name must not exceed 255 characters")
    @Column(name = "name_en", nullable = false, length = 255)
    private String nameEn;

    @Size(max = 255, message = "Arabic ingredient name must not exceed 255 characters")
    @Column(name = "name_ar", length = 255)
    private String nameAr;
}
