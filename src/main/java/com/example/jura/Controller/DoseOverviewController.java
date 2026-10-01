package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Api.TodayDose;
import com.example.jura.Service.DoseOverviewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/dose-schedule")
@RequiredArgsConstructor
public class DoseOverviewController {
    private final DoseOverviewService overviewService;
    @GetMapping("/today/{userId}")
    public ResponseEntity<?> today(@PathVariable Integer userId) {
        List<TodayDose> doses = overviewService.getToday(userId);
        if (doses.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Today's dose list is empty"));
        }
        return ResponseEntity.status(200).body(doses);
    }
    @GetMapping("/reminders/{userId}")
    public ResponseEntity<?> reminders(@PathVariable Integer userId) {
        List<TodayDose> doses = overviewService.getReminders(userId);
        if (doses.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Dose reminders list is empty"));
        }
        return ResponseEntity.status(200).body(doses);
    }
}
