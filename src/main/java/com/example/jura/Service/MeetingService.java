package com.example.jura.Service;

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
        return meetingRepository.findById(id).orElse(null);
    }

    public Meeting getMeetingByAppointmentId(Integer appointmentId) {
        return meetingRepository.findMeetingByAppointmentId(appointmentId);
    }

    public int addMeeting(Meeting meeting) {
        Appointment appointment = appointmentRepository.findById(meeting.getAppointmentId()).orElse(null);
        if (appointment == null) {
            return 1; // Appointment ID not found
        }
        if (!"APPROVED".equals(appointment.getStatus())) {
            return 2; // Appointment is not approved
        }
        if (meetingRepository.existsByAppointmentId(meeting.getAppointmentId())) {
            return 3; // Appointment already has a meeting
        }

        meeting.setId(null);
        meeting.setCreatedAt(LocalDateTime.now(RIYADH));
        meetingRepository.save(meeting);
        return 0; // Meeting added successfully
    }

    public int updateMeeting(Integer id, Meeting meeting) {
        Meeting oldMeeting = meetingRepository.findById(id).orElse(null);
        if (oldMeeting == null) {
            return 1; // Meeting ID not found
        }
        if (!oldMeeting.getAppointmentId().equals(meeting.getAppointmentId())) {
            return 2; // Appointment ID cannot be changed
        }

        oldMeeting.setZoomMeetingId(meeting.getZoomMeetingId());
        oldMeeting.setJoinUrl(meeting.getJoinUrl());
        meetingRepository.save(oldMeeting);
        return 0; // Meeting updated successfully
    }

    public boolean deleteMeeting(Integer id) {
        Meeting meeting = meetingRepository.findById(id).orElse(null);
        if (meeting == null) {
            return false;
        }

        meetingRepository.delete(meeting);
        return true;
    }

    public JoinResult getJoinUrl(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return new JoinResult(1, null); // Appointment ID not found
        }
        if (!"APPROVED".equals(appointment.getStatus())) {
            return new JoinResult(2, null); // Appointment is not approved
        }

        LocalDateTime now = LocalDateTime.now(RIYADH);
        if (now.isBefore(appointment.getStartAt()) || now.isAfter(appointment.getEndAt())) {
            return new JoinResult(3, null); // Outside the appointment window
        }

        Meeting meeting = meetingRepository.findMeetingByAppointmentId(appointmentId);
        if (meeting == null) {
            return new JoinResult(4, null); // Meeting not found
        }
        return new JoinResult(0, meeting.getJoinUrl()); // Join URL is available
    }

    public record JoinResult(int code, String joinUrl) {
    }
}
