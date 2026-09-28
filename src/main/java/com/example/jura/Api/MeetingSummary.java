package com.example.jura.Api;

import com.example.jura.Model.Meeting;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeetingSummary {

    private Integer id;
    private Integer appointmentId;
    private String zoomMeetingId;
    private LocalDateTime createdAt;

    public static MeetingSummary from(Meeting meeting) {
        return new MeetingSummary(meeting.getId(), meeting.getAppointmentId(),
                meeting.getZoomMeetingId(), meeting.getCreatedAt());
    }
}
