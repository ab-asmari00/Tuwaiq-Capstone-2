package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DrugCache;
import com.example.jura.Service.DrugCacheService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/drug-cache")
@RequiredArgsConstructor
public class DrugCacheController {
    private final DrugCacheService drugCacheService;
    @GetMapping("/getAll")
    public ResponseEntity<?> all() {
        List<DrugCache> drugs = drugCacheService.getAllDrugCaches();
        if (drugs.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Drug cache list is empty"));
        }
        return ResponseEntity.status(200).body(drugs);
    }
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> byId(@PathVariable Integer id) {
        DrugCache drug = drugCacheService.getDrugCacheById(id);
        return ResponseEntity.status(200).body(drug);
    }
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid DrugCache drug) {
        drugCacheService.addDrugCache(drug);
        return ResponseEntity.status(201).body(new ApiResponse("Drug cache added successfully"));
    }
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid DrugCache drug) {
        drugCacheService.updateDrugCache(id, drug);
        return ResponseEntity.status(200).body(new ApiResponse("Drug cache updated successfully"));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        drugCacheService.deleteDrugCache(id);
        return ResponseEntity.status(200).body(new ApiResponse("Drug cache deleted successfully"));
    }

}
