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
        return ResponseEntity.status(200).body(MeetingSummary.from(meeting));
    }

    @GetMapping("/get-by-appointment/{appointmentId}")
    public ResponseEntity<?> getMeetingByAppointmentId(@PathVariable Integer appointmentId) {
        Meeting meeting = meetingService.getMeetingByAppointmentId(appointmentId);
        return ResponseEntity.status(200).body(MeetingSummary.from(meeting));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addMeeting(@RequestBody @Valid Meeting meeting) {
        meetingService.addMeeting(meeting);
        return ResponseEntity.status(201).body(new ApiResponse("Meeting added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateMeeting(@PathVariable Integer id, @RequestBody @Valid Meeting meeting) {
        meetingService.updateMeeting(id, meeting);
        return ResponseEntity.status(200).body(new ApiResponse("Meeting updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Integer id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.status(200).body(new ApiResponse("Meeting deleted successfully"));
    }

    @GetMapping("/join/{appointmentId}")
    public ResponseEntity<?> joinMeeting(@PathVariable Integer appointmentId) {
        String joinUrl = meetingService.getJoinUrl(appointmentId);
        return ResponseEntity.status(200).body(Map.of("joinUrl", joinUrl));
    }
}
