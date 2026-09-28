package com.example.jura.Controller;

import com.example.jura.Api.AdherenceSummary;
import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DoseLog;
import com.example.jura.Service.DoseLogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/dose-log")
@RequiredArgsConstructor
public class DoseLogController {

    private final DoseLogService doseLogService;

    @GetMapping("/adherence-by-user/{userId}")
    public ResponseEntity<?> adherence(@PathVariable Integer userId) {
        AdherenceSummary summary = doseLogService.getAdherence(userId);
        if (summary == null) return ResponseEntity.status(404).body(new ApiResponse("Patient not found"));
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
        if (log == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose log ID not found"));
        }
        return ResponseEntity.status(200).body(log);
    }

    @GetMapping("/get-by-schedule/{scheduleId}")
    public ResponseEntity<?> getDoseLogsBySchedule(@PathVariable Integer scheduleId) {
        List<DoseLog> logs = doseLogService.getDoseLogsBySchedule(scheduleId);
        if (logs == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose schedule ID not found"));
        }
        return ResponseEntity.status(200).body(logs);
    }

    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getDoseLogsByItem(@PathVariable Integer itemId) {
        List<DoseLog> logs = doseLogService.getDoseLogsByItem(itemId);
        if (logs == null) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        return ResponseEntity.status(200).body(logs);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDoseLog(@RequestBody @Valid DoseLog log, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        }
        int result = doseLogService.addDoseLog(log);
        if (result != 0) {
            return doseLogError(result);
        }
        return ResponseEntity.status(201).body(new ApiResponse("Dose log added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDoseLog(@PathVariable Integer id, @RequestBody @Valid DoseLog log, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        }
        int result = doseLogService.updateDoseLog(id, log);
        if (result != 0) {
            return doseLogError(result);
        }
        return ResponseEntity.status(200).body(new ApiResponse("Dose log updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDoseLog(@PathVariable Integer id) {
        if (!doseLogService.deleteDoseLog(id)) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose log ID not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Dose log deleted successfully"));
    }

    private ResponseEntity<?> doseLogError(int result) {
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose log ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose schedule ID not found"));
        }
        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Due date and time do not match the schedule"));
        }
        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("Only TAKEN doses may have takenAt, and it cannot be in the future"));
        }
        if (result == 5) {
            return ResponseEntity.status(409).body(new ApiResponse("This scheduled dose already has a log; update the existing log"));
        }
        if (result == 6) {
            return ResponseEntity.status(400).body(new ApiResponse("Schedule ID and due time cannot be changed"));
        }
        if (result == 7) {
            return ResponseEntity.status(400).body(new ApiResponse("A dose can be marked MISSED only after it is due"));
        }
        if (result == 8) {
            return ResponseEntity.status(400).body(new ApiResponse("Scheduled item must belong to an existing patient"));
        }
        return ResponseEntity.status(400).body(new ApiResponse("Required fields are missing or status is invalid"));
    }
}
