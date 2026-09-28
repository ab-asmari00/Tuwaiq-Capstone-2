package com.example.jura.Api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiInteractionResponse {

    @NotNull
    private List<@NotNull @Valid PairResult> results;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PairResult {
        @NotNull
        private Integer otherItemId;

        @NotBlank
        @Pattern(regexp = "^(HIGH|MODERATE|NONE_IDENTIFIED|UNKNOWN)$")
        private String resultStatus;

        @NotBlank
        @Size(max = 4000)
        private String explanationAr;

        @NotBlank
        @Size(max = 4000)
        private String adviceAr;
    }
}
