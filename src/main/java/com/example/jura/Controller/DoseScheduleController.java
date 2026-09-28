package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DoseSchedule;
import com.example.jura.Service.DoseScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/dose-schedule")
@RequiredArgsConstructor
public class DoseScheduleController {

    private final DoseScheduleService doseScheduleService;

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllDoseSchedules() {
        List<DoseSchedule> schedules = doseScheduleService.getAllDoseSchedules();
        if (schedules.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Dose schedules list is empty"));
        }
        return ResponseEntity.status(200).body(schedules);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getDoseScheduleById(@PathVariable Integer id) {
        DoseSchedule schedule = doseScheduleService.getDoseScheduleById(id);
        if (schedule == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose schedule ID not found"));
        }
        return ResponseEntity.status(200).body(schedule);
    }

    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getDoseSchedulesByItem(@PathVariable Integer itemId) {
        List<DoseSchedule> schedules = doseScheduleService.getDoseSchedulesByItem(itemId);
        if (schedules == null) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        return ResponseEntity.status(200).body(schedules);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDoseSchedule(@RequestBody @Valid DoseSchedule schedule, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        }
        int result = doseScheduleService.addDoseSchedule(schedule);
        if (result != 0) {
            return scheduleError(result);
        }
        return ResponseEntity.status(201).body(new ApiResponse("Dose schedule added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDoseSchedule(@PathVariable Integer id, @RequestBody @Valid DoseSchedule schedule, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        }
        int result = doseScheduleService.updateDoseSchedule(id, schedule);
        if (result != 0) {
            return scheduleError(result);
        }
        return ResponseEntity.status(200).body(new ApiResponse("Dose schedule updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDoseSchedule(@PathVariable Integer id) {
        if (!doseScheduleService.deleteDoseSchedule(id)) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose schedule ID not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Dose schedule deleted successfully"));
    }

    private ResponseEntity<?> scheduleError(int result) {
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Dose schedule ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("User item must be active"));
        }
        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("Use uppercase day names separated by commas, without spaces"));
        }
        if (result == 5) {
            return ResponseEntity.status(400).body(new ApiResponse("End date cannot be before start date"));
        }
        if (result == 6) {
            return ResponseEntity.status(400).body(new ApiResponse("The same day cannot be repeated"));
        }
        if (result == 7) {
            return ResponseEntity.status(400).body(new ApiResponse("Item ID cannot be changed"));
        }
        if (result == 8) {
            return ResponseEntity.status(400).body(new ApiResponse("Item must belong to an existing patient"));
        }
        return ResponseEntity.status(400).body(new ApiResponse("Required schedule fields are missing"));
    }
}
