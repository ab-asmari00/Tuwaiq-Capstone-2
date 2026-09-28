package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Service.DrugSearchService;
import com.example.jura.Service.SfdaSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("api/v1/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugSearchService drugSearchService;
    private final SfdaSyncService sfdaSyncService;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam(defaultValue = "1") Integer page) {
        if (q.isBlank() || q.trim().length() > 100) {
            return ResponseEntity.status(400).body(new ApiResponse("Search name must be 1 to 100 characters"));
        }
        if (page == null || page < 1) {
            return ResponseEntity.status(400).body(new ApiResponse("Page must be at least 1"));
        }
        if (drugSearchService.isCatalogEmpty()) {
            return ResponseEntity.status(503).body(new ApiResponse("Drug catalog has not been synced yet"));
        }
        return ResponseEntity.status(200).body(drugSearchService.search(q, page));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync() {
        try {
            return ResponseEntity.status(200).body(sfdaSyncService.syncAllDrugs());
        } catch (RestClientException | IllegalStateException exception) {
            return ResponseEntity.status(502).body(new ApiResponse("SFDA drug sync failed: " + exception.getMessage()));
        }
    }
}
