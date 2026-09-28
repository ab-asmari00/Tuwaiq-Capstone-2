package com.example.jura.Service;

import com.example.jura.Model.Appointment;
import com.example.jura.Model.Meeting;
import com.example.jura.Model.User;
import com.example.jura.Repository.AppointmentRepository;
import com.example.jura.Repository.DoctorProfileRepository;
import com.example.jura.Repository.MeetingRepository;
import com.example.jura.Repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final MeetingRepository meetingRepository;
    private final ZoomService zoomService;

    public List<Appointment> getPatientAppointments(Integer patientId) {
        User patient = userRepository.findUserById(patientId);
        if (patient == null || !"PATIENT".equals(patient.getRole())) return null;
        return appointmentRepository.findByPatientIdOrderByStartAtDesc(patientId);
    }

    public List<Appointment> getDoctorAppointments(Integer doctorId, boolean pendingOnly) {
        User doctor = userRepository.findUserById(doctorId);
        if (doctor == null || !"DOCTOR".equals(doctor.getRole()) || !doctorProfileRepository.existsById(doctorId)) return null;
        return pendingOnly ? appointmentRepository.findByDoctorIdAndStatusOrderByStartAtAsc(doctorId, "PENDING")
                : appointmentRepository.findByDoctorIdOrderByStartAtDesc(doctorId);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Appointment getAppointmentById(Integer id) {
        return appointmentRepository.findById(id).orElse(null);
    }

    public int addAppointment(Appointment appointment) {
        User patient = userRepository.findUserById(appointment.getPatientId());
        if (patient == null) {
            return 1; // Patient ID not found
        }
        if (!"PATIENT".equals(patient.getRole())) {
            return 2; // User is not a patient
        }

        User doctor = userRepository.findUserById(appointment.getDoctorId());
        if (doctor == null || !"DOCTOR".equals(doctor.getRole())
                || !doctorProfileRepository.existsById(appointment.getDoctorId())) {
            return 3; // Doctor profile not found
        }

        if (!appointment.getEndAt().isAfter(appointment.getStartAt())) {
            return 4; // End time must be after start time
        }

        appointment.setId(null);
        appointment.setStatus("PENDING");
        appointmentRepository.save(appointment);
        return 0; // Appointment requested successfully
    }

    public int updateAppointment(Integer id, Appointment appointment) {
        Appointment oldAppointment = appointmentRepository.findById(id).orElse(null);
        if (oldAppointment == null) {
            return 1; // Appointment ID not found
        }
        if (!"PENDING".equals(oldAppointment.getStatus())) {
            return 2; // Only pending appointments can be updated
        }
        if (appointment.getStartAt() == null || appointment.getEndAt() == null
                || !appointment.getEndAt().isAfter(appointment.getStartAt())) {
            return 3; // Start and end times are required, and end must be after start
        }

        oldAppointment.setStartAt(appointment.getStartAt());
        oldAppointment.setEndAt(appointment.getEndAt());
        appointmentRepository.save(oldAppointment);
        return 0; // Appointment updated successfully
    }

    @Transactional
    public boolean deleteAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            return false;
        }

        meetingRepository.deleteByAppointmentId(id);
        appointmentRepository.delete(appointment);
        return true;
    }

    @Transactional
    public int approveAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            return 1; // Appointment ID not found
        }
        if (!"PENDING".equals(appointment.getStatus())) {
            return 2; // Only pending appointments can be approved
        }

        if (meetingRepository.existsByAppointmentId(id)) {
            return 5; // Appointment already has a meeting
        }
        if (!zoomService.isConfigured()) {
            return 3; // Zoom credentials or host user ID are missing
        }

        ZoomService.ZoomMeetingDetails zoomMeeting;
        try {
            zoomMeeting = zoomService.createMeeting(appointment);
        } catch (RestClientResponseException exception) {
            log.warn("Zoom returned HTTP {}: {}", exception.getStatusCode(), exception.getResponseBodyAsString());
            return 4; // Zoom rejected the request
        } catch (RestClientException | IllegalStateException | IllegalArgumentException exception) {
            log.warn("Zoom meeting creation failed: {}", exception.getMessage());
            return 4; // Zoom could not create the meeting
        }

        Meeting meeting = new Meeting();
        meeting.setAppointmentId(id);
        meeting.setZoomMeetingId(zoomMeeting.id());
        meeting.setJoinUrl(zoomMeeting.joinUrl());
        meeting.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Riyadh")));
        meetingRepository.save(meeting);

        appointment.setStatus("APPROVED");
        appointmentRepository.save(appointment);
        return 0; // Appointment approved successfully
    }

    public int rejectAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            return 1; // Appointment ID not found
        }
        if (!"PENDING".equals(appointment.getStatus())) {
            return 2; // Only pending appointments can be rejected
        }

        appointment.setStatus("REJECTED");
        appointmentRepository.save(appointment);
        return 0; // Appointment rejected successfully
    }
}
