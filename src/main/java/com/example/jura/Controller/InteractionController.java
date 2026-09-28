package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.AiInteractionResult;
import com.example.jura.Service.InteractionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/check/{itemId}")
    public ResponseEntity<?> checkInteractions(@PathVariable Integer itemId) {
        InteractionService.CheckOutcome outcome = interactionService.checkInteractions(itemId);
        if (outcome.code() == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        if (outcome.code() == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("Selected item must be active"));
        }
        if (outcome.code() == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Item must belong to an existing patient"));
        }
        if (outcome.code() == 4) {
            return ResponseEntity.status(503).body(new ApiResponse("Gemini is not configured"));
        }
        if (outcome.code() == 5) {
            return ResponseEntity.status(502).body(new ApiResponse("Gemini could not check interactions. No results were saved; retry later."));
        }
        return ResponseEntity.status(200).body(outcome.response());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addResult(@RequestBody @Valid AiInteractionResult result, Errors errors) {
        if (errors.hasErrors()) return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        int code = interactionService.addResult(result);
        if (code != 0) return manualError(code);
        return ResponseEntity.status(201).body(new ApiResponse("Manual interaction result added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateResult(@PathVariable Integer id, @RequestBody @Valid AiInteractionResult result, Errors errors) {
        if (errors.hasErrors()) return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        int code = interactionService.updateResult(id, result);
        if (code != 0) return manualError(code);
        return ResponseEntity.status(200).body(new ApiResponse("Interaction result manually updated"));
    }

    private ResponseEntity<?> manualError(int code) {
        if (code == 1) return ResponseEntity.status(404).body(new ApiResponse("Interaction result ID not found"));
        if (code == 2) return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        if (code == 4) return ResponseEntity.status(400).body(new ApiResponse("Pair IDs cannot be changed"));
        return ResponseEntity.status(400).body(new ApiResponse("Two different items belonging to the same patient are required"));
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllResults() {
        return ResponseEntity.status(200).body(interactionService.getAllResults());
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getResultById(@PathVariable Integer id) {
        AiInteractionResult result = interactionService.getResultById(id);
        if (result == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Interaction result ID not found"));
        }
        return ResponseEntity.status(200).body(result);
    }

    // Historical assessments: check again after ingredients, active items, or conditions change.
    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getResultsByItem(@PathVariable Integer itemId) {
        List<AiInteractionResult> results = interactionService.getResultsByItem(itemId);
        if (results == null) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        return ResponseEntity.status(200).body(results);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Integer id) {
        if (!interactionService.deleteResult(id)) {
            return ResponseEntity.status(404).body(new ApiResponse("Interaction result ID not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Interaction result deleted successfully"));
    }
}
