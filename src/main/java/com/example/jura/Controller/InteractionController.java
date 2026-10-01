package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.AiInteractionResult;
import com.example.jura.Service.InteractionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/check/{itemId}")
    public ResponseEntity<?> checkInteractions(@PathVariable Integer itemId) {
        return ResponseEntity.status(200).body(interactionService.checkInteractions(itemId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addResult(@RequestBody @Valid AiInteractionResult result) {
        interactionService.addResult(result);
        return ResponseEntity.status(201).body(new ApiResponse("Manual interaction result added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateResult(@PathVariable Integer id, @RequestBody @Valid AiInteractionResult result) {
        interactionService.updateResult(id, result);
        return ResponseEntity.status(200).body(new ApiResponse("Interaction result manually updated"));
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllResults() {
        List<AiInteractionResult> results = interactionService.getAllResults();
        if (results.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Interaction results list is empty"));
        }
        return ResponseEntity.status(200).body(results);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getResultById(@PathVariable Integer id) {
        AiInteractionResult result = interactionService.getResultById(id);
        return ResponseEntity.status(200).body(result);
    }

    // Historical assessments: check again after ingredients, active items, or conditions change.
    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getResultsByItem(@PathVariable Integer itemId) {
        List<AiInteractionResult> results = interactionService.getResultsByItem(itemId);
        if (results.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Interaction results list is empty"));
        }
        return ResponseEntity.status(200).body(results);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Integer id) {
        interactionService.deleteResult(id);
        return ResponseEntity.status(200).body(new ApiResponse("Interaction result deleted successfully"));
    }
}
