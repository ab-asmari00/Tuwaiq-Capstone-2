package com.example.jura;

import com.example.jura.Api.*;
import com.example.jura.Controller.*;
import com.example.jura.Model.*;
import com.example.jura.Repository.*;
import com.example.jura.Service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RemainingFeaturesTests {
    private final UserRepository users = mock(UserRepository.class);
    private final UserItemRepository items = mock(UserItemRepository.class);
    private final DoseScheduleRepository schedules = mock(DoseScheduleRepository.class);
    private final DoseLogRepository logs = mock(DoseLogRepository.class);
    private final MutableClock clock = new MutableClock();
    private final DoseLogService logService = new DoseLogService(logs, schedules, items, users, clock);
    private final DoseOverviewService overview = new DoseOverviewService(users, items, schedules, logs, logService, clock);
    private final Map<Integer, DoseLog> storedLogs = new LinkedHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger();

    private DoseSchedule prepare() {
        User patient = new User(5, "Demo", "demo@example.com", "plain-course-password", "PATIENT", "private-condition");
        UserItem item = new UserItem(1, 5, "SUPPLEMENT", "Iron", null, "demo dosage", true);
        DoseSchedule schedule = new DoseSchedule(7, 1, LocalTime.of(8, 0), "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY", LocalDate.of(2026, 9, 28), null);
        when(users.findUserById(5)).thenReturn(patient);
        when(items.findById(1)).thenReturn(Optional.of(item));
        when(items.findByUserIdAndActiveTrue(5)).thenReturn(List.of(item));
        when(items.findByUserId(5)).thenReturn(List.of(item));
        when(schedules.findById(7)).thenReturn(Optional.of(schedule));
        when(schedules.findByItemIdOrderByLocalTimeAsc(1)).thenReturn(List.of(schedule));
        when(logs.existsByScheduleIdAndDueAt(any(), any())).thenAnswer(call -> find(call.getArgument(0), call.getArgument(1)) != null);
        when(logs.findByScheduleIdAndDueAt(any(), any())).thenAnswer(call -> find(call.getArgument(0), call.getArgument(1)));
        when(logs.findByScheduleIdOrderByDueAtDesc(any())).thenAnswer(call -> storedLogs.values().stream().filter(log -> log.getScheduleId().equals(call.getArgument(0))).toList());
        when(logs.findById(any())).thenAnswer(call -> Optional.ofNullable(storedLogs.get(call.getArgument(0))));
        when(logs.saveAndFlush(any())).thenAnswer(call -> { DoseLog log = call.getArgument(0); log.setId(nextId.incrementAndGet()); storedLogs.put(log.getId(), log); return log; });
        when(logs.save(any())).thenAnswer(call -> { DoseLog log = call.getArgument(0); storedLogs.put(log.getId(), log); return log; });
        return schedule;
    }

    private DoseLog find(Integer id, LocalDateTime due) {
        return storedLogs.values().stream().filter(log -> id.equals(log.getScheduleId()) && due.equals(log.getDueAt())).findFirst().orElse(null);
    }

    @Test
    void exactlyTwoHoursCreatesMissedOnceAndLateTakenCorrectionRemovesReminder() throws Exception {
        prepare();
        var mvc = MockMvcBuilders.standaloneSetup(new DoseOverviewController(overview), new DoseLogController(logService)).build();
        clock.at("2026-09-28T06:59:59Z"); //09:59:59 Riyadh
        mvc.perform(get("/api/v1/dose-schedule/today/5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].reminderDue").value(true));
        assertTrue(storedLogs.isEmpty());
        clock.at("2026-09-28T07:00:00Z"); //10:00 Riyadh, exactly two hours
        mvc.perform(get("/api/v1/dose-schedule/today/5"))
                .andExpect(jsonPath("$[0].status").value("MISSED"))
                .andExpect(jsonPath("$[0].logId").value(1));
        overview.getToday(5);
        assertEquals(1, storedLogs.size());
        mvc.perform(put("/api/v1/dose-log/update/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduleId\":7,\"dueAt\":\"2026-09-28T08:00:00\",\"status\":\"TAKEN\",\"takenAt\":\"2026-09-28T09:00:00\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/dose-schedule/today/5")).andExpect(jsonPath("$[0].status").value("TAKEN"));
        assertEquals(List.of(), overview.getReminders(5));
    }

    @Test
    void remindersStartAtDueTimeAndTodayHonorsWeekdayDateRangeAndActiveFilter() {
        DoseSchedule schedule = prepare();
        clock.at("2026-09-28T04:59:59Z"); //07:59:59
        assertEquals(List.of(), overview.getReminders(5));
        clock.at("2026-09-28T05:00:00Z"); //08:00
        assertEquals(1, overview.getReminders(5).size());
        schedule.setDaysOfWeek("TUESDAY");
        assertEquals(List.of(), overview.getToday(5));
        schedule.setDaysOfWeek("MONDAY");
        schedule.setEndDate(LocalDate.of(2026, 9, 27));
        assertEquals(List.of(), overview.getToday(5));
        schedule.setEndDate(null);
        schedule.setStartDate(LocalDate.of(2026, 9, 29));
        assertEquals(List.of(), overview.getToday(5));
        when(items.findByUserIdAndActiveTrue(5)).thenReturn(List.of());
        assertEquals(List.of(), overview.getToday(5));
        assertNull(overview.getToday(404));
    }

    @Test
    void reminderWindowCrossingMidnightIncludesPreviousDaysPendingDose() {
        DoseSchedule schedule = prepare();
        schedule.setLocalTime(LocalTime.of(23, 30));
        clock.at("2026-09-28T21:30:00Z"); //00:30 Saudi on September29
        List<TodayDose> reminders = overview.getReminders(5);
        assertEquals(1, reminders.size());
        assertEquals(LocalDateTime.of(2026, 9, 28, 23, 30), reminders.get(0).getDueAt());
        assertEquals("PENDING", reminders.get(0).getStatus());
        assertFalse(overview.getToday(5).get(0).isReminderDue());
        clock.at("2026-09-28T22:30:00Z"); //01:30, two-hour boundary
        assertEquals(List.of(), overview.getReminders(5));
        assertEquals("MISSED", storedLogs.values().iterator().next().getStatus());
    }

    @Test
    void restartCatchupUsesPastDatesAndPreservesRecordedDoses() {
        DoseSchedule schedule = prepare();
        schedule.setStartDate(LocalDate.of(2026, 9, 25));
        schedule.setEndDate(LocalDate.of(2026, 9, 27));
        clock.at("2026-09-28T07:00:00Z");
        DoseLog taken = new DoseLog(100, 7, LocalDateTime.of(2026, 9, 26, 8, 0), "TAKEN", LocalDateTime.of(2026, 9, 26, 8, 5));
        storedLogs.put(100, taken);
        assertEquals(2, overview.refreshMissed(5));
        assertEquals(3, storedLogs.size());
        assertEquals("TAKEN", storedLogs.get(100).getStatus());
        assertEquals(0, overview.refreshMissed(5));
        assertEquals(3, storedLogs.size());
    }

    @Test
    void veryLongCatchupHistoryIsBoundedToTwoHundredInsertsPerCycle() {
        DoseSchedule schedule = prepare();
        schedule.setStartDate(LocalDate.of(2020, 1, 1));
        clock.at("2026-09-28T07:00:00Z");
        assertEquals(200, overview.refreshMissed(5));
        assertEquals(200, storedLogs.size());
        assertEquals(200, overview.refreshMissed(5));
        assertEquals(400, storedLogs.size());
    }

    @Test
    void backgroundFailureForOnePatientDoesNotPreventOtherPatientsFromBeingScanned() {
        User patient1 = new User(); patient1.setId(1);
        User patient2 = new User(); patient2.setId(2);
        when(users.findByRole("PATIENT")).thenReturn(List.of(patient1, patient2));
        DoseOverviewService scanner = mock(DoseOverviewService.class);
        when(scanner.refreshMissed(1)).thenThrow(new IllegalStateException("database temporarily unavailable"));
        new MissedDoseJob(users, scanner).markMissedDoses();
        verify(scanner).refreshMissed(2);
    }

    @Test
    void loginValidatesCredentialsAndReturnsNoPasswordOrMedicalConditions() throws Exception {
        User user = new User(5, "Demo", "demo@example.com", "plain-course-password", "PATIENT", "private-condition");
        when(users.findUserByEmail("demo@example.com")).thenReturn(user);
        UserService service = new UserService(users, mock(DataCleanupService.class));
        var mvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();
        mvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"demo@example.com\",\"password\":\"plain-course-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.medicalConditions").doesNotExist());
        mvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"demo@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        assertNull(service.login(new LoginRequest("missing@example.com", "anything")));
    }

    @Test
    void deletionCleansDoseChildrenAndConsultationChildrenBeforeDeletingUser() {
        prepare();
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        MeetingRepository meetings = mock(MeetingRepository.class);
        DoctorProfileRepository doctors = mock(DoctorProfileRepository.class);
        AppointmentService appointmentService = new AppointmentService(appointments, users, doctors, meetings, mock(ZoomService.class));
        DoseScheduleService scheduleService = new DoseScheduleService(schedules, items, users, logs);
        ItemIngredientRepository ingredients = mock(ItemIngredientRepository.class);
        DrugCacheRepository drugCaches = mock(DrugCacheRepository.class);
        AiInteractionResultRepository interactions = mock(AiInteractionResultRepository.class);
        UserItemService itemService = new UserItemService(items, users, drugCaches,
                new ItemIngredientService(ingredients, items, drugCaches), interactions, scheduleService);
        DataCleanupService cleanup = new DataCleanupService(items, itemService, appointments, appointmentService, doctors);
        Appointment appointment = new Appointment(); appointment.setId(10);
        when(appointments.findByPatientIdOrDoctorId(5, 5)).thenReturn(List.of(appointment));
        when(appointments.findById(10)).thenReturn(Optional.of(appointment));
        when(doctors.existsById(5)).thenReturn(true);
        UserService service = new UserService(users, cleanup);
        assertTrue(service.deleteUser(5));
        var order = inOrder(logs, schedules, interactions, ingredients, items, meetings, appointments, doctors, users);
        order.verify(logs).deleteByScheduleId(7);
        order.verify(schedules).deleteAll(any());
        order.verify(interactions).deleteByItemAIdOrItemBId(1, 1);
        order.verify(ingredients).deleteByItemId(1);
        order.verify(items).delete(any());
        order.verify(meetings).deleteByAppointmentId(10);
        order.verify(appointments).delete(appointment);
        order.verify(doctors).deleteById(5);
        order.verify(users).delete(any());
    }

    @Test
    void doctorDeletionCleansAppointmentsAndMeetingsButKeepsUser() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        MeetingRepository meetings = mock(MeetingRepository.class);
        DoctorProfileRepository doctors = mock(DoctorProfileRepository.class);
        AppointmentService appointmentService = new AppointmentService(appointments, users, doctors, meetings, mock(ZoomService.class));
        DataCleanupService cleanup = new DataCleanupService(items, mock(UserItemService.class), appointments, appointmentService, doctors);
        Appointment appointment = new Appointment(); appointment.setId(10);
        when(appointments.findByDoctorIdOrderByStartAtDesc(2)).thenReturn(List.of(appointment));
        when(appointments.findById(10)).thenReturn(Optional.of(appointment));
        DoctorProfile doctor = new DoctorProfile(2, "General medicine", "Demo");
        when(doctors.findById(2)).thenReturn(Optional.of(doctor));
        assertTrue(new DoctorProfileService(doctors, users, cleanup).deleteDoctorProfile(2));
        var order = inOrder(meetings, appointments, doctors);
        order.verify(meetings).deleteByAppointmentId(10);
        order.verify(appointments).delete(appointment);
        order.verify(doctors).delete(doctor);
        verify(users, never()).delete(any());
    }

    @Test
    void patientAndDoctorListsRejectWrongRolesAndUseCorrectRepositoryFilters() {
        prepare();
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        DoctorProfileRepository doctors = mock(DoctorProfileRepository.class);
        User doctor = new User(); doctor.setRole("DOCTOR");
        when(users.findUserById(2)).thenReturn(doctor);
        when(doctors.existsById(2)).thenReturn(true);
        when(appointments.findByPatientIdOrderByStartAtDesc(5)).thenReturn(List.of());
        when(appointments.findByDoctorIdAndStatusOrderByStartAtAsc(2, "PENDING")).thenReturn(List.of());
        AppointmentService service = new AppointmentService(appointments, users, doctors, mock(MeetingRepository.class), mock(ZoomService.class));
        assertEquals(List.of(), service.getPatientAppointments(5));
        assertEquals(List.of(), service.getDoctorAppointments(2, true));
        assertNull(service.getPatientAppointments(2));
        assertNull(service.getDoctorAppointments(5, false));
        verify(appointments).findByDoctorIdAndStatusOrderByStartAtAsc(2, "PENDING");
    }

    @Test
    void cacheCrudGeneratesArabicSearchKeyAndBlocksReferencedRowsAndDuplicateRegistration() {
        DrugCacheRepository cache = mock(DrugCacheRepository.class);
        DrugCacheService service = new DrugCacheService(cache, items, clock);
        DrugCache drug = new DrugCache(99, " DEMO-1 ", "بَانَادُول", "Demo", "ignored", "Demo ingredient", null);
        assertEquals(0, service.addDrugCache(drug));
        assertNull(drug.getId()); assertEquals("DEMO-1", drug.getSfdaRegNo());
        assertEquals("بانادول", drug.getSearchNameAr()); assertNotNull(drug.getSyncedAt());
        when(cache.existsBySfdaRegNo("DEMO-1")).thenReturn(true);
        assertEquals(2, service.addDrugCache(drug));
        drug.setId(9); when(cache.findById(9)).thenReturn(Optional.of(drug));
        when(items.existsByDrugCacheId(9)).thenReturn(true);
        assertEquals(3, service.updateDrugCache(9, drug));
        assertEquals(3, service.deleteDrugCache(9));
        verify(cache, never()).delete(any());
    }

    @Test
    void manualResultCrudCannotImpersonateGeminiOrMoveBetweenPatients() {
        prepare();
        AiInteractionResultRepository results = mock(AiInteractionResultRepository.class);
        InteractionService service = new InteractionService(results, items, mock(ItemIngredientRepository.class), users, mock(GeminiService.class));
        UserItem second = new UserItem(2, 5, "SUPPLEMENT", "Calcium", null, null, true);
        when(items.findById(2)).thenReturn(Optional.of(second));
        AiInteractionResult result = new AiInteractionResult(null, 1, 2, "UNKNOWN", "Manual demo", "Demo", "fake references", "gemini-3.8-flash", null);
        assertEquals(0, service.addResult(result));
        assertEquals("MANUAL", result.getModelName()); assertEquals("[]", result.getSourceLinksJson());
        assertNotNull(result.getCheckedAt());
        result.setId(3); when(results.findById(3)).thenReturn(Optional.of(result));
        AiInteractionResult update = new AiInteractionResult(null, 1, 2, "MODERATE", "Manual edited", "Demo", null, null, null);
        assertEquals(0, service.updateResult(3, update));
        assertEquals("MODERATE", result.getResultStatus()); assertEquals("MANUAL", result.getModelName());
        update.setItemBId(99); assertEquals(4, service.updateResult(3, update));
        update.setItemBId(2); second.setUserId(20); assertEquals(3, service.addResult(update));
    }

    @Test
    void adherenceUsesRecordedStatusesAndEmptyHistoryHasNoPercentage() {
        prepare();
        when(logs.findByScheduleIdInOrderByDueAtDesc(List.of(7))).thenReturn(List.of());
        assertNull(logService.getAdherence(5).getTakenPercentage());
        when(logs.findByScheduleIdInOrderByDueAtDesc(List.of(7))).thenReturn(List.of(
                new DoseLog(null, 7, LocalDateTime.now(clock), "TAKEN", null),
                new DoseLog(null, 7, LocalDateTime.now(clock), "TAKEN", null),
                new DoseLog(null, 7, LocalDateTime.now(clock), "SKIPPED", null),
                new DoseLog(null, 7, LocalDateTime.now(clock), "MISSED", null)));
        AdherenceSummary summary = logService.getAdherence(5);
        assertEquals(50.0, summary.getTakenPercentage()); assertEquals(4, summary.getTotalRecorded());
        assertEquals(2, summary.getTaken()); assertEquals(1, summary.getSkipped()); assertEquals(1, summary.getMissed());
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-09-28T05:00:00Z");
        void at(String value) { instant = Instant.parse(value); }
        public ZoneId getZone() { return ZoneId.of("Asia/Riyadh"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant, zone); }
        public Instant instant() { return instant; }
    }
}
