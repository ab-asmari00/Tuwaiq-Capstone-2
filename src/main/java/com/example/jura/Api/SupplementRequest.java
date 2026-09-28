package com.example.jura.Api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplementRequest {

    @NotNull(message = "User is required")
    private Integer userId;

    @NotBlank(message = "Supplement name is required")
    @Size(max = 255, message = "Supplement name must not exceed 255 characters")
    private String displayName;

    @Size(max = 255, message = "Dosage must not exceed 255 characters")
    private String dosageText;

    @NotEmpty(message = "At least one ingredient is required")
    private List<@NotNull(message = "Ingredient must not be null") @Valid IngredientInput> ingredients;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IngredientInput {

        @NotBlank(message = "English ingredient name is required")
        @Size(max = 255, message = "English ingredient name must not exceed 255 characters")
        private String nameEn;

        @Size(max = 255, message = "Arabic ingredient name must not exceed 255 characters")
        private String nameAr;
    }
}
