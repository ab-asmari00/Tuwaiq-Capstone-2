package com.example.jura.Controller;

import com.example.jura.Api.AdherenceSummary;
import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DoseLog;
import com.example.jura.Service.DoseLogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/dose-log")
@RequiredArgsConstructor
public class DoseLogController {

    private final DoseLogService doseLogService;

    @GetMapping("/adherence-by-user/{userId}")
    public ResponseEntity<?> adherence(@PathVariable Integer userId) {
        AdherenceSummary summary = doseLogService.getAdherence(userId);
        return ResponseEntity.status(200).body(summary);
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllDoseLogs() {
        List<DoseLog> logs = doseLogService.getAllDoseLogs();
        if (logs.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Dose logs list is empty"));
        }
        return ResponseEntity.status(200).body(logs);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getDoseLogById(@PathVariable Integer id) {
        DoseLog log = doseLogService.getDoseLogById(id);
        return ResponseEntity.status(200).body(log);
    }

    @GetMapping("/get-by-schedule/{scheduleId}")
    public ResponseEntity<?> getDoseLogsBySchedule(@PathVariable Integer scheduleId) {
        List<DoseLog> logs = doseLogService.getDoseLogsBySchedule(scheduleId);
        return ResponseEntity.status(200).body(logs);
    }

    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getDoseLogsByItem(@PathVariable Integer itemId) {
        List<DoseLog> logs = doseLogService.getDoseLogsByItem(itemId);
        return ResponseEntity.status(200).body(logs);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDoseLog(@RequestBody @Valid DoseLog log) {
        doseLogService.addDoseLog(log);
        return ResponseEntity.status(201).body(new ApiResponse("Dose log added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDoseLog(@PathVariable Integer id, @RequestBody @Valid DoseLog log) {
        doseLogService.updateDoseLog(id, log);
        return ResponseEntity.status(200).body(new ApiResponse("Dose log updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDoseLog(@PathVariable Integer id) {
        doseLogService.deleteDoseLog(id);
        return ResponseEntity.status(200).body(new ApiResponse("Dose log deleted successfully"));
    }

}
