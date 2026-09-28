package com.example.jura.Service;

import com.example.jura.Api.AdherenceSummary;
import com.example.jura.Model.DoseLog;
import com.example.jura.Model.DoseSchedule;
import com.example.jura.Model.User;
import com.example.jura.Model.UserItem;
import com.example.jura.Repository.DoseLogRepository;
import com.example.jura.Repository.DoseScheduleRepository;
import com.example.jura.Repository.UserItemRepository;
import com.example.jura.Repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoseLogService {

    private final DoseLogRepository doseLogRepository;
    private final DoseScheduleRepository doseScheduleRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public List<DoseLog> getAllDoseLogs() {
        return doseLogRepository.findAll();
    }

    public DoseLog getDoseLogById(Integer id) {
        return doseLogRepository.findById(id).orElse(null);
    }

    public List<DoseLog> getDoseLogsBySchedule(Integer scheduleId) {
        if (!doseScheduleRepository.existsById(scheduleId)) {
            return null;
        }
        return doseLogRepository.findByScheduleIdOrderByDueAtDesc(scheduleId);
    }

    public List<DoseLog> getDoseLogsByItem(Integer itemId) {
        if (!userItemRepository.existsById(itemId)) {
            return null;
        }
        List<Integer> scheduleIds = doseScheduleRepository.findByItemIdOrderByLocalTimeAsc(itemId).stream()
                .map(DoseSchedule::getId).toList();
        if (scheduleIds.isEmpty()) {
            return List.of();
        }
        return doseLogRepository.findByScheduleIdInOrderByDueAtDesc(scheduleIds);
    }

    public AdherenceSummary getAdherence(Integer userId) {
        User patient = userRepository.findUserById(userId);
        if (patient == null || !"PATIENT".equals(patient.getRole())) return null;
        List<Integer> scheduleIds = userItemRepository.findByUserId(userId).stream()
                .flatMap(item -> doseScheduleRepository.findByItemIdOrderByLocalTimeAsc(item.getId()).stream())
                .map(DoseSchedule::getId).toList();
        List<DoseLog> history = scheduleIds.isEmpty() ? List.of() : doseLogRepository.findByScheduleIdInOrderByDueAtDesc(scheduleIds);
        long taken = history.stream().filter(log -> "TAKEN".equals(log.getStatus())).count();
        long skipped = history.stream().filter(log -> "SKIPPED".equals(log.getStatus())).count();
        long missed = history.stream().filter(log -> "MISSED".equals(log.getStatus())).count();
        long total = taken + skipped + missed;
        return new AdherenceSummary(taken, skipped, missed, total,
                total == 0 ? null : Math.round(taken * 10000.0 / total) / 100.0);
    }

    public int addDoseLog(DoseLog doseLog) {
        int result = validateDoseLog(doseLog, true);
        if (result != 0) {
            return result; // Dose log details are invalid
        }
        if (doseLogRepository.existsByScheduleIdAndDueAt(doseLog.getScheduleId(), doseLog.getDueAt())) {
            return 5; // This scheduled dose already has a log; update the existing log
        }
        doseLog.setId(null);
        try {
            // Repository transaction completes here, allowing duplicate errors to be handled safely.
            doseLogRepository.saveAndFlush(doseLog);
        } catch (DataIntegrityViolationException exception) {
            if (doseLogRepository.existsByScheduleIdAndDueAt(doseLog.getScheduleId(), doseLog.getDueAt())) {
                return 5; // Another request already logged this scheduled dose
            }
            throw exception;
        }
        return 0; // Dose log added successfully
    }

    @Transactional
    public int updateDoseLog(Integer id, DoseLog doseLog) {
        DoseLog oldLog = doseLogRepository.findById(id).orElse(null);
        if (oldLog == null) {
            return 1; // Dose log ID not found
        }
        if (!oldLog.getScheduleId().equals(doseLog.getScheduleId())
                || !oldLog.getDueAt().equals(doseLog.getDueAt())) {
            return 6; // Schedule ID and due time cannot be changed
        }
        if ("TAKEN".equals(doseLog.getStatus()) && "TAKEN".equals(oldLog.getStatus())
                && doseLog.getTakenAt() == null) {
            doseLog.setTakenAt(oldLog.getTakenAt());
        }
        // Existing due time remains historical if the schedule was edited later.
        int result = validateDoseLog(doseLog, false);
        if (result != 0) {
            return result; // Dose log details are invalid
        }
        oldLog.setStatus(doseLog.getStatus());
        oldLog.setTakenAt(doseLog.getTakenAt());
        doseLogRepository.save(oldLog);
        return 0; // Dose log updated successfully
    }

    @Transactional
    public boolean deleteDoseLog(Integer id) {
        DoseLog doseLog = doseLogRepository.findById(id).orElse(null);
        if (doseLog == null) {
            return false;
        }
        doseLogRepository.delete(doseLog);
        return true;
    }

    private int validateDoseLog(DoseLog doseLog, boolean checkScheduleTime) {
        if (doseLog.getScheduleId() == null || doseLog.getDueAt() == null || doseLog.getStatus() == null) {
            return 9; // Required dose log fields are missing
        }
        DoseSchedule schedule = doseScheduleRepository.findById(doseLog.getScheduleId()).orElse(null);
        if (schedule == null) {
            return 2; // Dose schedule ID not found
        }
        UserItem item = userItemRepository.findById(schedule.getItemId()).orElse(null);
        if (item == null) {
            return 8; // Scheduled item or its patient no longer exists
        }
        User patient = userRepository.findUserById(item.getUserId());
        if (patient == null || !"PATIENT".equals(patient.getRole())) {
            return 8; // Scheduled item must belong to an existing patient
        }
        if (checkScheduleTime) {
            LocalDate date = doseLog.getDueAt().toLocalDate();
            if (date.isBefore(schedule.getStartDate())
                    || (schedule.getEndDate() != null && date.isAfter(schedule.getEndDate()))
                    || !Arrays.asList(schedule.getDaysOfWeek().split(",")).contains(date.getDayOfWeek().name())
                    || !doseLog.getDueAt().toLocalTime().equals(schedule.getLocalTime())) {
                return 3; // Due date and time do not match the schedule
            }
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if ("TAKEN".equals(doseLog.getStatus())) {
            if (doseLog.getTakenAt() == null) {
                doseLog.setTakenAt(now);
            }
            if (doseLog.getTakenAt().isAfter(now)) {
                return 4; // Taken time cannot be in the future
            }
            return 0; // Taken dose is valid
        }
        if ("SKIPPED".equals(doseLog.getStatus()) || "MISSED".equals(doseLog.getStatus())) {
            if (doseLog.getTakenAt() != null) {
                return 4; // Only TAKEN doses may have a taken time
            }
            if ("MISSED".equals(doseLog.getStatus()) && !doseLog.getDueAt().isBefore(now)) {
                return 7; // A dose can be marked MISSED only after it is due
            }
            return 0; // Skipped or missed dose is valid
        }
        return 9; // Status must be TAKEN, SKIPPED, or MISSED
    }
}
