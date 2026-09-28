package com.example.jura;

import com.example.jura.Controller.DoseScheduleController;
import com.example.jura.Model.DoseSchedule;
import com.example.jura.Model.User;
import com.example.jura.Model.UserItem;
import com.example.jura.Repository.*;
import com.example.jura.Service.DoseScheduleService;
import com.example.jura.Service.ItemIngredientService;
import com.example.jura.Service.UserItemService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoseScheduleFlowTests {

    private final DoseScheduleRepository schedules = mock(DoseScheduleRepository.class);
    private final UserItemRepository items = mock(UserItemRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DoseLogRepository logs = mock(DoseLogRepository.class);
    private final DoseScheduleService service = new DoseScheduleService(schedules, items, users, logs);

    private DoseSchedule validSchedule() {
        return new DoseSchedule(null, 1, LocalTime.of(8, 0), "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY",
                LocalDate.of(2026, 9, 28), null);
    }

    private UserItem preparePatientItem() {
        UserItem item = new UserItem(1, 5, "SUPPLEMENT", "Iron", null, null, true);
        when(items.findById(1)).thenReturn(Optional.of(item));
        User patient = new User();
        patient.setRole("PATIENT");
        when(users.findUserById(5)).thenReturn(patient);
        return item;
    }

    @Test
    void dailySchedulesFitAndTwoTimesAreStoredAsSeparateRows() {
        preparePatientItem();
        DoseSchedule morning = validSchedule();
        morning.setId(100);
        assertEquals(56, morning.getDaysOfWeek().length());
        assertEquals(0, service.addDoseSchedule(morning));
        assertNull(morning.getId());
        DoseSchedule evening = validSchedule();
        evening.setLocalTime(LocalTime.of(20, 0));
        assertEquals(0, service.addDoseSchedule(evening));
        verify(schedules).save(morning);
        verify(schedules).save(evening);
    }

    @Test
    void invalidAndRepeatedDaysCannotBeSaved() {
        preparePatientItem();
        DoseSchedule schedule = validSchedule();
        for (String invalid : List.of("MONDAY,", "EVERYDAY", "Monday", "MONDAY, TUESDAY")) {
            schedule.setDaysOfWeek(invalid);
            assertEquals(4, service.addDoseSchedule(schedule));
        }
        schedule.setDaysOfWeek("MONDAY,MONDAY");
        assertEquals(6, service.addDoseSchedule(schedule));
        verify(schedules, never()).save(any());
    }

    @Test
    void dateRangeCannotBeReversedButMayBeOpenEndedOrOneDay() {
        preparePatientItem();
        DoseSchedule schedule = validSchedule();
        schedule.setEndDate(schedule.getStartDate().minusDays(1));
        assertEquals(5, service.addDoseSchedule(schedule));
        verify(schedules, never()).save(any());
        schedule.setEndDate(schedule.getStartDate());
        assertEquals(0, service.addDoseSchedule(schedule));
        schedule.setEndDate(null);
        assertEquals(0, service.addDoseSchedule(schedule));
    }

    @Test
    void missingInactiveAndNonPatientItemsAreRejected() {
        assertEquals(2, service.addDoseSchedule(validSchedule()));
        UserItem item = preparePatientItem();
        item.setActive(false);
        assertEquals(3, service.addDoseSchedule(validSchedule()));
        item.setActive(true);
        users.findUserById(5).setRole("DOCTOR");
        assertEquals(8, service.addDoseSchedule(validSchedule()));
        when(users.findUserById(5)).thenReturn(null);
        assertEquals(8, service.addDoseSchedule(validSchedule()));
        DoseSchedule schedule = validSchedule();
        schedule.setLocalTime(null);
        assertEquals(9, service.addDoseSchedule(schedule));
        verify(schedules, never()).save(any());
    }

    @Test
    void scheduleUpdateCannotMoveItemsButCanChangeTimeDaysAndEndDate() {
        preparePatientItem();
        DoseSchedule saved = validSchedule();
        saved.setId(9);
        when(schedules.findById(9)).thenReturn(Optional.of(saved));
        DoseSchedule update = validSchedule();
        update.setItemId(2);
        assertEquals(7, service.updateDoseSchedule(9, update));
        assertEquals(1, service.updateDoseSchedule(404, update));
        update.setItemId(1);
        update.setLocalTime(LocalTime.of(8, 30));
        update.setDaysOfWeek("SUNDAY,TUESDAY,THURSDAY");
        update.setEndDate(LocalDate.of(2026, 10, 28));
        assertEquals(0, service.updateDoseSchedule(9, update));
        assertEquals(9, saved.getId());
        assertEquals(1, saved.getItemId());
        assertEquals(LocalTime.of(8, 30), saved.getLocalTime());
        assertEquals("SUNDAY,TUESDAY,THURSDAY", saved.getDaysOfWeek());
        assertEquals(update.getEndDate(), saved.getEndDate());
        verify(schedules).save(saved);
    }

    @Test
    void deletingScheduleDeletesDependentLogsFirstAndMissingIdIsFalse() {
        assertFalse(service.deleteDoseSchedule(404));
        DoseSchedule schedule = validSchedule();
        schedule.setId(9);
        when(schedules.findById(9)).thenReturn(Optional.of(schedule));
        assertTrue(service.deleteDoseSchedule(9));
        var order = inOrder(logs, schedules);
        order.verify(logs).deleteByScheduleId(9);
        order.verify(schedules).delete(schedule);
    }

    @Test
    void itemDeletionCleansAllSchedulesAndTheirLogsBeforeDeletingTheItem() {
        UserItem item = preparePatientItem();
        DoseSchedule morning = validSchedule();
        morning.setId(9);
        DoseSchedule evening = validSchedule();
        evening.setId(10);
        evening.setLocalTime(LocalTime.of(20, 0));
        List<DoseSchedule> rows = List.of(morning, evening);
        when(schedules.findByItemIdOrderByLocalTimeAsc(1)).thenReturn(rows);
        UserItemService itemService = new UserItemService(items, users, mock(DrugCacheRepository.class),
                mock(ItemIngredientService.class), mock(AiInteractionResultRepository.class), service);
        assertTrue(itemService.deleteUserItem(1));
        var order = inOrder(logs, schedules, items);
        order.verify(logs).deleteByScheduleId(9);
        order.verify(logs).deleteByScheduleId(10);
        order.verify(schedules).deleteAll(rows);
        order.verify(items).delete(item);
    }

    @Test
    void controllerAcceptsAllSevenDaysAndRejectsMalformedDaysAndMissingTime() throws Exception {
        preparePatientItem();
        var mvc = MockMvcBuilders.standaloneSetup(new DoseScheduleController(service)).build();
        String daily = "{\"itemId\":1,\"localTime\":\"08:00:00\",\"daysOfWeek\":\"MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY\",\"startDate\":\"2026-09-28\",\"endDate\":null}";
        mvc.perform(post("/api/v1/dose-schedule/add").contentType(MediaType.APPLICATION_JSON).content(daily))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/dose-schedule/add").contentType(MediaType.APPLICATION_JSON)
                        .content(daily.replace("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY", "EVERYDAY")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/dose-schedule/add").contentType(MediaType.APPLICATION_JSON)
                        .content(daily.replace("\"08:00:00\"", "null")))
                .andExpect(status().isBadRequest());
        verify(schedules, times(1)).save(any());
    }

    @Test
    void getByItemDistinguishesMissingItemFromExistingEmptyScheduleList() {
        assertNull(service.getDoseSchedulesByItem(404));
        when(items.existsById(1)).thenReturn(true);
        when(schedules.findByItemIdOrderByLocalTimeAsc(1)).thenReturn(List.of());
        assertEquals(List.of(), service.getDoseSchedulesByItem(1));
    }
}
