package com.example.jura.Service;

import com.example.jura.Api.TodayDose;
import com.example.jura.Model.*;
import com.example.jura.Repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoseOverviewService {
    private final UserRepository users;
    private final UserItemRepository items;
    private final DoseScheduleRepository schedules;
    private final DoseLogRepository logs;
    private final DoseLogService doseLogService;
    private final Clock clock;

    public List<TodayDose> getToday(Integer userId) {
        LocalDate date = LocalDate.now(clock);
        return getDoses(userId, date, date);
    }

    private List<TodayDose> getDoses(Integer userId, LocalDate firstDate, LocalDate lastDate) {
        User patient = users.findUserById(userId);
        if (patient == null || !"PATIENT".equals(patient.getRole())) return null;
        LocalDateTime now = LocalDateTime.now(clock);
        List<TodayDose> result = new ArrayList<>();
        for (UserItem item : items.findByUserIdAndActiveTrue(userId)) {
            for (DoseSchedule schedule : schedules.findByItemIdOrderByLocalTimeAsc(item.getId())) {
                for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
                    if (!occursOn(schedule, date)) continue;
                    LocalDateTime dueAt = date.atTime(schedule.getLocalTime());
                    markMissed(schedule, dueAt, now);
                    DoseLog log = logs.findByScheduleIdAndDueAt(schedule.getId(), dueAt);
                    String status = log == null ? "PENDING" : log.getStatus();
                    result.add(new TodayDose(schedule.getId(), item.getId(), item.getDisplayName(), item.getDosageText(),
                            dueAt, status, log == null ? null : log.getId(), log == null ? null : log.getTakenAt(),
                            log == null && !now.isBefore(dueAt) && now.isBefore(dueAt.plusHours(2))));
                }
            }
        }
        result.sort(Comparator.comparing(TodayDose::getDueAt).thenComparing(TodayDose::getScheduleId));
        return result;
    }

    public List<TodayDose> getReminders(Integer userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<TodayDose> today = getDoses(userId, now.minusHours(2).toLocalDate(), now.toLocalDate());
        if (today == null) return null;
        return today.stream().filter(TodayDose::isReminderDue).toList();
    }

    public int refreshMissed(Integer userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        int added = 0;
        for (UserItem item : items.findByUserIdAndActiveTrue(userId)) {
            for (DoseSchedule schedule : schedules.findByItemIdOrderByLocalTimeAsc(item.getId())) {
                LocalDate last = now.toLocalDate();
                if (schedule.getEndDate() != null && schedule.getEndDate().isBefore(last)) last = schedule.getEndDate();
                if (schedule.getStartDate().isAfter(last)) continue;
                Set<LocalDateTime> recorded = new HashSet<>();
                for (DoseLog log : logs.findByScheduleIdOrderByDueAtDesc(schedule.getId())) recorded.add(log.getDueAt());
                for (LocalDate date = schedule.getStartDate(); !date.isAfter(last); date = date.plusDays(1)) {
                    if (!occursOn(schedule, date)) continue;
                    LocalDateTime dueAt = date.atTime(schedule.getLocalTime());
                    if (!recorded.contains(dueAt) && markMissed(schedule, dueAt, now)) added++;
                    if (added >= 200) return added; // Catch up long histories in bounded batches
                }
            }
        }
        return added;
    }

    private boolean occursOn(DoseSchedule schedule, LocalDate date) {
        return !date.isBefore(schedule.getStartDate())
                && (schedule.getEndDate() == null || !date.isAfter(schedule.getEndDate()))
                && Arrays.asList(schedule.getDaysOfWeek().split(",")).contains(date.getDayOfWeek().name());
    }

    private boolean markMissed(DoseSchedule schedule, LocalDateTime dueAt, LocalDateTime now) {
        if (now.isBefore(dueAt.plusHours(2)) || logs.existsByScheduleIdAndDueAt(schedule.getId(), dueAt)) return false;
        return doseLogService.addDoseLog(new DoseLog(null, schedule.getId(), dueAt, "MISSED", null)) == 0;
    }
}
