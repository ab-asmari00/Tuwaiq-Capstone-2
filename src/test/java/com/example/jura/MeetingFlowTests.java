package com.example.jura;

import com.example.jura.Model.Appointment;
import com.example.jura.Model.Meeting;
import com.example.jura.Repository.AppointmentRepository;
import com.example.jura.Repository.DoctorProfileRepository;
import com.example.jura.Repository.MeetingRepository;
import com.example.jura.Repository.UserRepository;
import com.example.jura.Service.AppointmentService;
import com.example.jura.Service.MeetingService;
import com.example.jura.Service.ZoomService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class MeetingFlowTests {

    @Test
    void zoomCreatesScheduledMeetingFromAccountToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ZoomService zoomService = new ZoomService(builder, "account123", "client123", "secret123", "host123");

        server.expect(once(), requestTo("https://zoom.us/oauth/token?grant_type=account_credentials&account_id=account123"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic Y2xpZW50MTIzOnNlY3JldDEyMw=="))
                .andRespond(withSuccess("{\"access_token\":\"token123\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.zoom.us/v2/users/host123/meetings"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andExpect(jsonPath("$.start_time").value("2026-10-01T12:00:00Z"))
                .andExpect(jsonPath("$.duration").value(30))
                .andExpect(jsonPath("$.settings.join_before_host").value(true))
                .andExpect(jsonPath("$.settings.jbh_time").value(5))
                .andExpect(jsonPath("$.settings.waiting_room").value(false))
                .andRespond(withSuccess("{\"id\":12345678901,\"join_url\":\"https://zoom.us/j/12345678901\"}", MediaType.APPLICATION_JSON));

        Appointment appointment = new Appointment();
        appointment.setStartAt(LocalDateTime.of(2026, 10, 1, 15, 0));
        appointment.setEndAt(LocalDateTime.of(2026, 10, 1, 15, 30));

        ZoomService.ZoomMeetingDetails meeting = zoomService.createMeeting(appointment);

        assertEquals("12345678901", meeting.id());
        assertEquals("https://zoom.us/j/12345678901", meeting.joinUrl());
        server.verify();
    }

    @Test
    void failedZoomCallLeavesAppointmentPending() {
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        MeetingRepository meetingRepository = mock(MeetingRepository.class);
        ZoomService zoomService = mock(ZoomService.class);
        AppointmentService service = new AppointmentService(appointmentRepository, mock(UserRepository.class),
                mock(DoctorProfileRepository.class), meetingRepository, zoomService);

        Appointment appointment = new Appointment();
        appointment.setId(7);
        appointment.setStatus("PENDING");
        when(appointmentRepository.findById(7)).thenReturn(Optional.of(appointment));
        when(zoomService.isConfigured()).thenReturn(true);
        when(zoomService.createMeeting(appointment)).thenThrow(new RestClientException("Zoom unavailable"));

        assertEquals(4, service.approveAppointment(7));
        assertEquals("PENDING", appointment.getStatus());
        verify(meetingRepository, never()).save(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void approvalSavesMeetingAndJoinUrlIsTimeGated() {
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        MeetingRepository meetingRepository = mock(MeetingRepository.class);
        ZoomService zoomService = mock(ZoomService.class);
        AppointmentService appointmentService = new AppointmentService(appointmentRepository,
                mock(UserRepository.class), mock(DoctorProfileRepository.class), meetingRepository, zoomService);
        MeetingService meetingService = new MeetingService(meetingRepository, appointmentRepository);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));
        Appointment appointment = new Appointment();
        appointment.setId(8);
        appointment.setStatus("PENDING");
        appointment.setStartAt(now.minusMinutes(1));
        appointment.setEndAt(now.plusMinutes(1));
        when(appointmentRepository.findById(8)).thenReturn(Optional.of(appointment));
        when(zoomService.isConfigured()).thenReturn(true);
        when(zoomService.createMeeting(appointment))
                .thenReturn(new ZoomService.ZoomMeetingDetails("zoom8", "https://zoom.us/j/8"));

        assertEquals(0, appointmentService.approveAppointment(8));
        assertEquals("APPROVED", appointment.getStatus());
        verify(meetingRepository).save(argThat(meeting -> meeting.getAppointmentId() == 8
                && "zoom8".equals(meeting.getZoomMeetingId())));

        Meeting savedMeeting = new Meeting();
        savedMeeting.setAppointmentId(8);
        savedMeeting.setJoinUrl("https://zoom.us/j/8");
        when(meetingRepository.findMeetingByAppointmentId(8)).thenReturn(savedMeeting);
        assertEquals("https://zoom.us/j/8", meetingService.getJoinUrl(8).joinUrl());

        appointment.setStartAt(now.plusHours(1));
        appointment.setEndAt(now.plusHours(2));
        assertEquals(3, meetingService.getJoinUrl(8).code());
        assertTrue(meetingService.getJoinUrl(8).joinUrl() == null);
    }
}
