package com.example.jura;

import com.example.jura.Controller.DoseLogController;
import com.example.jura.Model.*;
import com.example.jura.Repository.*;
import com.example.jura.Service.DoseLogService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoseLogFlowTests {
    private final DoseLogRepository logs = mock(DoseLogRepository.class);
    private final DoseScheduleRepository schedules = mock(DoseScheduleRepository.class);
    private final UserItemRepository items = mock(UserItemRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DoseLogService service = new DoseLogService(logs, schedules, items, users, java.time.Clock.system(ZoneId.of("Asia/Riyadh")));

    private DoseSchedule prepare() {
        DoseSchedule schedule = new DoseSchedule(7, 1, LocalTime.of(8, 0),
                "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY", LocalDate.of(2020, 1, 1), null);
        when(schedules.findById(7)).thenReturn(Optional.of(schedule));
        when(items.findById(1)).thenReturn(Optional.of(new UserItem(1, 5, "SUPPLEMENT", "Iron", null, null, true)));
        User patient = new User();
        patient.setRole("PATIENT");
        when(users.findUserById(5)).thenReturn(patient);
        return schedule;
    }

    private DoseLog log(String status) {
        return new DoseLog(null, 7, LocalDateTime.of(2020, 1, 6, 8, 0), status, null);
    }

    @Test
    void takenTimeCanBeExplicitOrDefaultsToCurrentSaudiTimeAndIdIsGenerated() {
        prepare();
        DoseLog taken = log("TAKEN");
        taken.setTakenAt(taken.getDueAt().plusMinutes(5));
        taken.setId(99);
        assertEquals(0, service.addDoseLog(taken));
        assertNull(taken.getId());
        assertEquals(LocalDateTime.of(2020, 1, 6, 8, 5), taken.getTakenAt());
        DoseLog automatic = log("TAKEN");
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));
        assertEquals(0, service.addDoseLog(automatic));
        assertFalse(automatic.getTakenAt().isBefore(before));
        assertFalse(automatic.getTakenAt().isAfter(LocalDateTime.now(ZoneId.of("Asia/Riyadh"))));
    }

    @Test
    void skippedAndMissedDosesCannotHaveTakenTimeAndTakenTimeCannotBeFuture() {
        prepare();
        for (String status : List.of("SKIPPED", "MISSED")) {
            DoseLog dose = log(status);
            dose.setTakenAt(dose.getDueAt());
            assertEquals(4, service.addDoseLog(dose));
        }
        DoseLog futureTaken = log("TAKEN");
        futureTaken.setTakenAt(LocalDateTime.now(ZoneId.of("Asia/Riyadh")).plusDays(1));
        assertEquals(4, service.addDoseLog(futureTaken));
        verify(logs, never()).saveAndFlush(any());
        assertEquals(0, service.addDoseLog(log("SKIPPED")));
        assertEquals(0, service.addDoseLog(log("MISSED")));
    }

    @Test
    void occurrenceMustMatchTimeDateRangeAndSelectedWeekday() {
        DoseSchedule schedule = prepare();
        DoseLog wrongTime = log("SKIPPED");
        wrongTime.setDueAt(wrongTime.getDueAt().plusMinutes(1));
        assertEquals(3, service.addDoseLog(wrongTime));
        schedule.setStartDate(LocalDate.of(2020, 1, 7));
        assertEquals(3, service.addDoseLog(log("SKIPPED")));
        schedule.setStartDate(LocalDate.of(2020, 1, 1));
        schedule.setEndDate(LocalDate.of(2020, 1, 5));
        assertEquals(3, service.addDoseLog(log("SKIPPED")));
        schedule.setEndDate(LocalDate.of(2020, 1, 6));
        assertEquals(0, service.addDoseLog(log("SKIPPED"))); // End date is inclusive
        schedule.setDaysOfWeek("TUESDAY");
        assertEquals(3, service.addDoseLog(log("SKIPPED")));
    }

    @Test
    void futureOccurrenceCannotBeMarkedMissed() {
        prepare();
        DoseLog dose = log("MISSED");
        dose.setDueAt(LocalDate.now(ZoneId.of("Asia/Riyadh")).plusDays(1).atTime(8, 0));
        assertEquals(7, service.addDoseLog(dose));
        verify(logs, never()).saveAndFlush(any());
    }

    @Test
    void duplicateOccurrenceAndDatabaseDuplicateRaceReturnConflictCode() {
        prepare();
        DoseLog dose = log("SKIPPED");
        when(logs.existsByScheduleIdAndDueAt(7, dose.getDueAt())).thenReturn(true);
        assertEquals(5, service.addDoseLog(dose));
        verify(logs, never()).saveAndFlush(any());
        when(logs.existsByScheduleIdAndDueAt(7, dose.getDueAt())).thenReturn(false, true);
        when(logs.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertEquals(5, service.addDoseLog(dose));
    }

    @Test
    void missingScheduleAndInvalidParentOrStatusCannotCreateLogs() {
        assertEquals(2, service.addDoseLog(log("TAKEN")));
        prepare();
        when(users.findUserById(5)).thenReturn(null);
        assertEquals(8, service.addDoseLog(log("SKIPPED")));
        prepare();
        when(items.findById(1)).thenReturn(Optional.empty());
        assertEquals(8, service.addDoseLog(log("SKIPPED")));
        prepare();
        assertEquals(9, service.addDoseLog(log("PENDING")));
        DoseLog missingDue = log("SKIPPED");
        missingDue.setDueAt(null);
        assertEquals(9, service.addDoseLog(missingDue));
        verify(logs, never()).saveAndFlush(any());
    }

    @Test
    void updatePreservesOccurrenceAndHistoricalTimeEvenAfterScheduleChanges() {
        DoseSchedule schedule = prepare();
        DoseLog saved = log("TAKEN");
        saved.setId(3);
        saved.setTakenAt(saved.getDueAt().plusMinutes(5));
        when(logs.findById(3)).thenReturn(Optional.of(saved));
        DoseLog update = log("TAKEN");
        update.setScheduleId(8);
        assertEquals(6, service.updateDoseLog(3, update));
        update.setScheduleId(7);
        update.setDueAt(saved.getDueAt().plusDays(1));
        assertEquals(6, service.updateDoseLog(3, update));
        update.setDueAt(saved.getDueAt());
        schedule.setLocalTime(LocalTime.of(9, 0));
        assertEquals(0, service.updateDoseLog(3, update));
        assertEquals(LocalDateTime.of(2020, 1, 6, 8, 5), saved.getTakenAt());
        update.setStatus("SKIPPED");
        update.setTakenAt(null);
        assertEquals(0, service.updateDoseLog(3, update));
        assertEquals("SKIPPED", saved.getStatus());
        assertNull(saved.getTakenAt());
        assertEquals(1, service.updateDoseLog(404, update));
    }

    @Test
    void existingEmptyHistoryIsDistinctFromMissingParentAndItemHistoryUsesAllSchedules() {
        assertNull(service.getDoseLogsBySchedule(404));
        when(schedules.existsById(7)).thenReturn(true);
        when(logs.findByScheduleIdOrderByDueAtDesc(7)).thenReturn(List.of());
        assertEquals(List.of(), service.getDoseLogsBySchedule(7));
        assertNull(service.getDoseLogsByItem(404));
        when(items.existsById(1)).thenReturn(true);
        when(schedules.findByItemIdOrderByLocalTimeAsc(1)).thenReturn(List.of());
        assertEquals(List.of(), service.getDoseLogsByItem(1));
        DoseSchedule schedule = prepare();
        when(schedules.findByItemIdOrderByLocalTimeAsc(1)).thenReturn(List.of(schedule));
        when(logs.findByScheduleIdInOrderByDueAtDesc(List.of(7))).thenReturn(List.of(log("TAKEN")));
        assertEquals(1, service.getDoseLogsByItem(1).size());
    }

    @Test
    void controllerRejectsInvalidStatusAndMapsDuplicateAndMissingSchedule() throws Exception {
        prepare();
        var mvc = MockMvcBuilders.standaloneSetup(new DoseLogController(service)).build();
        String body = "{\"scheduleId\":7,\"dueAt\":\"2020-01-06T08:00:00\",\"status\":\"SKIPPED\",\"takenAt\":null}";
        mvc.perform(post("/api/v1/dose-log/add").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/dose-log/add").contentType(MediaType.APPLICATION_JSON).content(body.replace("SKIPPED", "PENDING")))
                .andExpect(status().isBadRequest());
        when(logs.existsByScheduleIdAndDueAt(7, log("SKIPPED").getDueAt())).thenReturn(true);
        mvc.perform(post("/api/v1/dose-log/add").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/dose-log/add").contentType(MediaType.APPLICATION_JSON).content(body.replace("\"scheduleId\":7", "\"scheduleId\":404")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingMissingLogIsFalseAndExistingLogIsDeleted() {
        assertFalse(service.deleteDoseLog(404));
        DoseLog saved = log("TAKEN");
        when(logs.findById(3)).thenReturn(Optional.of(saved));
        assertTrue(service.deleteDoseLog(3));
        verify(logs).delete(saved);
    }
}
