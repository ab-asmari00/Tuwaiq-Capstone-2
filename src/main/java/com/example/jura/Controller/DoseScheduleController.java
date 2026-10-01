package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DoseSchedule;
import com.example.jura.Service.DoseScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.status(200).body(schedule);
    }

    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getDoseSchedulesByItem(@PathVariable Integer itemId) {
        List<DoseSchedule> schedules = doseScheduleService.getDoseSchedulesByItem(itemId);
        if (schedules.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Dose schedules list is empty"));
        }
        return ResponseEntity.status(200).body(schedules);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDoseSchedule(@RequestBody @Valid DoseSchedule schedule) {
        doseScheduleService.addDoseSchedule(schedule);
        return ResponseEntity.status(201).body(new ApiResponse("Dose schedule added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDoseSchedule(@PathVariable Integer id, @RequestBody @Valid DoseSchedule schedule) {
        doseScheduleService.updateDoseSchedule(id, schedule);
        return ResponseEntity.status(200).body(new ApiResponse("Dose schedule updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDoseSchedule(@PathVariable Integer id) {
        doseScheduleService.deleteDoseSchedule(id);
        return ResponseEntity.status(200).body(new ApiResponse("Dose schedule deleted successfully"));
    }

}
