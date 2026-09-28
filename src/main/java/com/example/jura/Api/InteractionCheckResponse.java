package com.example.jura.Api;

import com.example.jura.Model.AiInteractionResult;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InteractionCheckResponse {
    private String message;
    private List<AiInteractionResult> results;
}
