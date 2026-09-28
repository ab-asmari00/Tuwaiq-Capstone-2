package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DrugCache;
import com.example.jura.Service.DrugCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/drug-cache")
@RequiredArgsConstructor
public class DrugCacheController {
    private final DrugCacheService drugCacheService;
    @GetMapping("/getAll")
    public ResponseEntity<?> all() { return ResponseEntity.status(200).body(drugCacheService.getAllDrugCaches()); }
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> byId(@PathVariable Integer id) {
        DrugCache drug = drugCacheService.getDrugCacheById(id);
        if (drug == null) return error(1);
        return ResponseEntity.status(200).body(drug);
    }
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid DrugCache drug, Errors errors) {
        if (errors.hasErrors()) return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        int result = drugCacheService.addDrugCache(drug);
        return result == 0 ? ResponseEntity.status(201).body(new ApiResponse("Drug cache added successfully")) : error(result);
    }
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid DrugCache drug, Errors errors) {
        if (errors.hasErrors()) return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        int result = drugCacheService.updateDrugCache(id, drug);
        return result == 0 ? ResponseEntity.status(200).body(new ApiResponse("Drug cache updated successfully")) : error(result);
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        int result = drugCacheService.deleteDrugCache(id);
        return result == 0 ? ResponseEntity.status(200).body(new ApiResponse("Drug cache deleted successfully")) : error(result);
    }
    private ResponseEntity<?> error(int code) {
        if (code == 1) return ResponseEntity.status(404).body(new ApiResponse("Drug cache ID not found"));
        if (code == 2) return ResponseEntity.status(409).body(new ApiResponse("SFDA registration number already exists"));
        if (code == 3) return ResponseEntity.status(409).body(new ApiResponse("Drug cache is referenced by a user item"));
        return ResponseEntity.status(400).body(new ApiResponse("At least one trade name is required"));
    }
}
