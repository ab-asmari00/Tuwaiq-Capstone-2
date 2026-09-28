package com.example.jura;

import com.example.jura.Api.*;
import com.example.jura.Model.*;
import com.example.jura.Repository.*;
import com.example.jura.Service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"jura.doses.auto-missed-enabled=false", "spring.jpa.show-sql=false"})
@Import(BackendDatabaseFlowTests.FixedTime.class)
@Transactional
class BackendDatabaseFlowTests {
    @Autowired private UserService userService;
    @Autowired private UserItemService itemService;
    @Autowired private DoseScheduleService scheduleService;
    @Autowired private DoseLogService logService;
    @Autowired private DoseOverviewService overview;
    @Autowired private DoctorProfileService doctorService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private UserRepository users;
    @Autowired private UserItemRepository items;
    @Autowired private ItemIngredientRepository ingredients;
    @Autowired private DoseScheduleRepository schedules;
    @Autowired private DoseLogRepository logs;
    @Autowired private AppointmentRepository appointments;
    @Autowired private MeetingRepository meetings;
    @Autowired private DoctorProfileRepository doctors;

    private User createUser(String role) {
        User user = new User(null, "Rollback Demo", UUID.randomUUID() + "@example.com", "course-password", role, "");
        assertEquals(0, userService.addUser(user));
        return user;
    }

    @Test
    void databasePatientJourneyIncludesLoginScheduleAutomaticMissedLateTakenAndDeletion() {
        User patient = createUser("PATIENT");
        assertEquals(patient.getId(), userService.login(new LoginRequest(patient.getEmail(), "course-password")).getId());
        assertEquals(0, itemService.addSupplement(new SupplementRequest(patient.getId(), "Rollback iron", null,
                List.of(new SupplementRequest.IngredientInput("Iron", "حديد")))));
        UserItem item = items.findByUserId(patient.getId()).get(0);
        assertEquals(1, ingredients.countByItemId(item.getId()));
        DoseSchedule schedule = new DoseSchedule(null, item.getId(), LocalTime.MIDNIGHT,
                "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY", LocalDate.of(2026, 9, 28), null);
        assertEquals(0, scheduleService.addDoseSchedule(schedule));
        TodayDose today = overview.getToday(patient.getId()).get(0);
        assertEquals("MISSED", today.getStatus());
        DoseLog missed = logs.findById(today.getLogId()).orElseThrow();
        assertEquals(5, logService.addDoseLog(new DoseLog(null, schedule.getId(), missed.getDueAt(), "MISSED", null)));
        assertEquals(0, logService.updateDoseLog(missed.getId(), new DoseLog(null, schedule.getId(), missed.getDueAt(),
                "TAKEN", LocalDateTime.of(2026, 9, 28, 1, 0))));
        assertEquals("TAKEN", overview.getToday(patient.getId()).get(0).getStatus());
        assertEquals(100.0, logService.getAdherence(patient.getId()).getTakenPercentage());
        assertTrue(userService.deleteUser(patient.getId()));
        assertFalse(users.existsById(patient.getId()));
        assertFalse(items.existsById(item.getId()));
        assertEquals(0, ingredients.countByItemId(item.getId()));
        assertFalse(schedules.existsById(schedule.getId()));
        assertFalse(logs.existsById(missed.getId()));
    }

    @Test
    void databaseDoctorProfileDeletionRemovesDependentAppointmentsAndLocalMeetings() {
        User patient = createUser("PATIENT");
        User doctor = createUser("DOCTOR");
        assertEquals(0, doctorService.addDoctorProfile(new DoctorProfile(doctor.getId(), "General medicine", "Rollback demo")));
        Appointment appointment = new Appointment();
        appointment.setPatientId(patient.getId()); appointment.setDoctorId(doctor.getId());
        appointment.setStartAt(LocalDateTime.of(2026, 9, 29, 12, 0));
        appointment.setEndAt(LocalDateTime.of(2026, 9, 29, 12, 30));
        assertEquals(0, appointmentService.addAppointment(appointment));
        assertEquals(1, appointmentService.getDoctorAppointments(doctor.getId(), true).size());
        assertEquals(1, appointmentService.getPatientAppointments(patient.getId()).size());
        Meeting meeting = meetings.saveAndFlush(new Meeting(null, appointment.getId(), "local-rollback-test",
                "https://example.com/rollback-meeting", LocalDateTime.of(2026, 9, 28, 10, 0)));
        assertTrue(doctorService.deleteDoctorProfile(doctor.getId()));
        assertFalse(doctors.existsById(doctor.getId()));
        assertFalse(appointments.existsById(appointment.getId()));
        assertFalse(meetings.existsById(meeting.getId()));
        assertTrue(users.existsById(doctor.getId()));
        assertTrue(users.existsById(patient.getId()));
    }

    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock courseTestClock() {
            return Clock.fixed(Instant.parse("2026-09-28T07:00:00Z"), ZoneId.of("Asia/Riyadh"));
        }
    }
}
