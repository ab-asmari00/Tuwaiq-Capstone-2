package com.example.jura.Service;

import com.example.jura.Api.ApiException;
import com.example.jura.Model.Appointment;
import com.example.jura.Model.Meeting;
import com.example.jura.Repository.AppointmentRepository;
import com.example.jura.Repository.MeetingRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");

    private final MeetingRepository meetingRepository;
    private final AppointmentRepository appointmentRepository;

    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    public Meeting getMeetingById(Integer id) {
        return meetingRepository.findById(id).orElseThrow(() -> new ApiException("Meeting ID not found"));
    }

    public Meeting getMeetingByAppointmentId(Integer appointmentId) {
        return java.util.Optional.ofNullable(meetingRepository.findMeetingByAppointmentId(appointmentId))
                .orElseThrow(() -> new ApiException("Meeting not found for this appointment"));
    }

    public void addMeeting(Meeting meeting) {
        Appointment appointment = appointmentRepository.findById(meeting.getAppointmentId()).orElseThrow(() -> new ApiException("Appointment ID not found"));
        if (!"APPROVED".equals(appointment.getStatus())) {
            throw new ApiException("Appointment is not approved");
        }
        if (meetingRepository.existsByAppointmentId(meeting.getAppointmentId())) {
            throw new ApiException("Appointment already has a meeting");
        }

        meeting.setId(null);
        meeting.setCreatedAt(LocalDateTime.now(RIYADH));
        meetingRepository.save(meeting);
    }

    public void updateMeeting(Integer id, Meeting meeting) {
        Meeting oldMeeting = meetingRepository.findById(id).orElseThrow(() -> new ApiException("Meeting ID not found"));
        if (!oldMeeting.getAppointmentId().equals(meeting.getAppointmentId())) {
            throw new ApiException("Appointment ID cannot be changed");
        }

        oldMeeting.setZoomMeetingId(meeting.getZoomMeetingId());
        oldMeeting.setJoinUrl(meeting.getJoinUrl());
        meetingRepository.save(oldMeeting);
    }

    public void deleteMeeting(Integer id) {
        Meeting meeting = meetingRepository.findById(id).orElseThrow(() -> new ApiException("Meeting ID not found"));

        meetingRepository.delete(meeting);
    }

    public String getJoinUrl(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(() -> new ApiException("Appointment ID not found"));
        if (!"APPROVED".equals(appointment.getStatus())) {
            throw new ApiException("Appointment is not approved");
        }

        LocalDateTime now = LocalDateTime.now(RIYADH);
        if (now.isBefore(appointment.getStartAt()) || now.isAfter(appointment.getEndAt())) {
            throw new ApiException("Join link is available only during the appointment window");
        }

        Meeting meeting = meetingRepository.findMeetingByAppointmentId(appointmentId);
        if (meeting == null) {
            throw new ApiException("Meeting not found for this appointment");
        }
        return meeting.getJoinUrl(); // Join URL is available
    }

}
