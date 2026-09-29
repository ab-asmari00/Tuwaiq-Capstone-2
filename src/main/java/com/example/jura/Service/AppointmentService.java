package com.example.jura.Service;

import com.example.jura.Api.ApiException;
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
    private final AppointmentEmailService appointmentEmailService;

    public List<Appointment> getPatientAppointments(Integer patientId) {
        User user = userRepository.findById(patientId)
                .orElseThrow(() -> new ApiException("User not found"));
        if (!"PATIENT".equals(user.getRole())) {
            throw new ApiException("User is not a patient");
        }
        return appointmentRepository.findByPatientIdOrderByStartAtDesc(patientId);
    }

    public List<Appointment> getDoctorAppointments(Integer doctorId, boolean pendingOnly) {
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ApiException("Doctor not found"));
        if (!"DOCTOR".equals(doctor.getRole()) || !doctorProfileRepository.existsById(doctorId)) {
            throw new ApiException("Doctor profile not found");
        }
        return pendingOnly ? appointmentRepository.findByDoctorIdAndStatusOrderByStartAtAsc(doctorId, "PENDING")
                : appointmentRepository.findByDoctorIdOrderByStartAtDesc(doctorId);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Appointment getAppointmentById(Integer id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Appointment not found"));
    }

    @Transactional
    public void addAppointment(Appointment appointment) {
        User patient = userRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new ApiException("Patient ID not found"));
        if (!"PATIENT".equals(patient.getRole())) {
            throw new ApiException("User is not a patient");
        }

        User doctor = userRepository.findById(appointment.getDoctorId())
                .orElseThrow(() -> new ApiException("Doctor not found"));
        if (!"DOCTOR".equals(doctor.getRole())
                || !doctorProfileRepository.existsById(appointment.getDoctorId())) {
            throw new ApiException("Doctor profile not found");
        }

        if (!appointment.getEndAt().isAfter(appointment.getStartAt())) {
            throw new ApiException("End time must be after start time");
        }

        appointment.setId(null);
        appointment.setStatus("PENDING");
        appointmentRepository.save(appointment);
        appointmentEmailService.notifyRequested(appointment);
    }

    public void updateAppointment(Integer id, Appointment appointment) {
        Appointment oldAppointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Appointment ID not found"));
        if (!"PENDING".equals(oldAppointment.getStatus())) {
            throw new ApiException("Only pending appointments can be updated");
        }
        if (appointment.getStartAt() == null || appointment.getEndAt() == null
                || !appointment.getEndAt().isAfter(appointment.getStartAt())) {
            throw new ApiException("Start and end times are required, and end must be after start");
        }

        oldAppointment.setStartAt(appointment.getStartAt());
        oldAppointment.setEndAt(appointment.getEndAt());
        appointmentRepository.save(oldAppointment);
    }

    @Transactional
    public void deleteAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Appointment not found"));

        meetingRepository.deleteByAppointmentId(id);
        appointmentRepository.delete(appointment);
    }

    @Transactional
    public void approveAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Appointment not found"));
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new ApiException("Only pending appointments can be approved");
        }

        if (meetingRepository.existsByAppointmentId(id)) {
            throw new ApiException("Appointment already has a meeting");
        }
        if (!zoomService.isConfigured()) {
            throw new ApiException("Zoom credentials or host user ID are missing");
        }

        ZoomService.ZoomMeetingDetails zoomMeeting;
        try {
            zoomMeeting = zoomService.createMeeting(appointment);
        } catch (RestClientResponseException exception) {
            log.warn("Zoom returned HTTP {}: {}", exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new ApiException("Zoom rejected the request");
        } catch (RestClientException | IllegalStateException | IllegalArgumentException exception) {
            log.warn("Zoom meeting creation failed: {}", exception.getMessage());
            throw new ApiException("Zoom could not create the meeting");
        }

        Meeting meeting = new Meeting();
        meeting.setAppointmentId(id);
        meeting.setZoomMeetingId(zoomMeeting.id());
        meeting.setJoinUrl(zoomMeeting.joinUrl());
        meeting.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Riyadh")));
        meetingRepository.save(meeting);

        appointment.setStatus("APPROVED");
        appointmentRepository.save(appointment);
        appointmentEmailService.notifyApproved(appointment);
    }

    public void rejectAppointment(Integer id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Appointment ID not found"));
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new ApiException("Only pending appointments can be rejected");
        }

        appointment.setStatus("REJECTED");
        appointmentRepository.save(appointment);
    }
}
