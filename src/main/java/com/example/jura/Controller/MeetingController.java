package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Api.MeetingSummary;
import com.example.jura.Model.Meeting;
import com.example.jura.Service.MeetingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/meeting")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllMeetings() {
        List<MeetingSummary> meetings = meetingService.getAllMeetings().stream()
                .map(MeetingSummary::from)
                .toList();
        if (meetings.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Meetings list is empty"));
        }
        return ResponseEntity.status(200).body(meetings);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getMeetingById(@PathVariable Integer id) {
        Meeting meeting = meetingService.getMeetingById(id);
        if (meeting == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Meeting ID not found"));
        }
        return ResponseEntity.status(200).body(MeetingSummary.from(meeting));
    }

    @GetMapping("/get-by-appointment/{appointmentId}")
    public ResponseEntity<?> getMeetingByAppointmentId(@PathVariable Integer appointmentId) {
        Meeting meeting = meetingService.getMeetingByAppointmentId(appointmentId);
        if (meeting == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Meeting not found for this appointment"));
        }
        return ResponseEntity.status(200).body(MeetingSummary.from(meeting));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addMeeting(@RequestBody @Valid Meeting meeting, Errors errors) {
        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            return ResponseEntity.status(400).body(new ApiResponse(message));
        }

        int result = meetingService.addMeeting(meeting);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Appointment ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Appointment is not approved"));
        }
        if (result == 3) {
            return ResponseEntity.status(409).body(new ApiResponse("Appointment already has a meeting"));
        }
        return ResponseEntity.status(201).body(new ApiResponse("Meeting added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateMeeting(@PathVariable Integer id, @RequestBody @Valid Meeting meeting, Errors errors) {
        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            return ResponseEntity.status(400).body(new ApiResponse(message));
        }

        int result = meetingService.updateMeeting(id, meeting);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Meeting ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("Appointment ID cannot be changed"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Meeting updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Integer id) {
        boolean isFound = meetingService.deleteMeeting(id);
        if (!isFound) {
            return ResponseEntity.status(404).body(new ApiResponse("Meeting ID not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Meeting deleted successfully"));
    }

    @GetMapping("/join/{appointmentId}")
    public ResponseEntity<?> joinMeeting(@PathVariable Integer appointmentId) {
        MeetingService.JoinResult result = meetingService.getJoinUrl(appointmentId);
        if (result.code() == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Appointment ID not found"));
        }
        if (result.code() == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Appointment is not approved"));
        }
        if (result.code() == 3) {
            return ResponseEntity.status(403).body(new ApiResponse("Join link is available only during the appointment window"));
        }
        if (result.code() == 4) {
            return ResponseEntity.status(404).body(new ApiResponse("Meeting not found for this appointment"));
        }
        return ResponseEntity.status(200).body(Map.of("joinUrl", result.joinUrl()));
    }
}
