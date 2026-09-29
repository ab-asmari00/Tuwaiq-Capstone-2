package com.example.jura.Service;

import com.example.jura.Api.ApiException;
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
        return doseLogRepository.findById(id).orElseThrow(() -> new ApiException("Dose log ID not found"));
    }

    public List<DoseLog> getDoseLogsBySchedule(Integer scheduleId) {
        if (!doseScheduleRepository.existsById(scheduleId)) {
            throw new ApiException("Dose schedule ID not found");
        }
        return doseLogRepository.findByScheduleIdOrderByDueAtDesc(scheduleId);
    }

    public List<DoseLog> getDoseLogsByItem(Integer itemId) {
        if (!userItemRepository.existsById(itemId)) {
            throw new ApiException("User item ID not found");
        }
        List<Integer> scheduleIds = doseScheduleRepository.findByItemIdOrderByLocalTimeAsc(itemId).stream()
                .map(DoseSchedule::getId).toList();
        if (scheduleIds.isEmpty()) {
            return List.of();
        }
        return doseLogRepository.findByScheduleIdInOrderByDueAtDesc(scheduleIds);
    }

    public AdherenceSummary getAdherence(Integer userId) {
        User patient = userRepository.findById(userId).orElseThrow(() -> new ApiException("Patient not found"));
        if (!"PATIENT".equals(patient.getRole())) throw new ApiException("Patient not found");
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

    public void addDoseLog(DoseLog doseLog) {
        validateDoseLog(doseLog, true);
        if (doseLogRepository.existsByScheduleIdAndDueAt(doseLog.getScheduleId(), doseLog.getDueAt())) {
            throw new ApiException("This scheduled dose already has a log; update the existing log");
        }
        doseLog.setId(null);
        try {
            // Repository transaction completes here, allowing duplicate errors to be handled safely.
            doseLogRepository.saveAndFlush(doseLog);
        } catch (DataIntegrityViolationException exception) {
            if (doseLogRepository.existsByScheduleIdAndDueAt(doseLog.getScheduleId(), doseLog.getDueAt())) {
                throw new ApiException("This scheduled dose already has a log; update the existing log");
            }
            throw exception;
        }
    }

    @Transactional
    public void updateDoseLog(Integer id, DoseLog doseLog) {
        DoseLog oldLog = doseLogRepository.findById(id).orElseThrow(() -> new ApiException("Dose log ID not found"));
        if (!oldLog.getScheduleId().equals(doseLog.getScheduleId())
                || !oldLog.getDueAt().equals(doseLog.getDueAt())) {
            throw new ApiException("Schedule ID and due time cannot be changed");
        }
        if ("TAKEN".equals(doseLog.getStatus()) && "TAKEN".equals(oldLog.getStatus())
                && doseLog.getTakenAt() == null) {
            doseLog.setTakenAt(oldLog.getTakenAt());
        }
        // Existing due time remains historical if the schedule was edited later.
        validateDoseLog(doseLog, false);
        oldLog.setStatus(doseLog.getStatus());
        oldLog.setTakenAt(doseLog.getTakenAt());
        doseLogRepository.save(oldLog);
    }

    @Transactional
    public void deleteDoseLog(Integer id) {
        DoseLog doseLog = doseLogRepository.findById(id).orElseThrow(() -> new ApiException("Dose log ID not found"));
        doseLogRepository.delete(doseLog);
    }

    private void validateDoseLog(DoseLog doseLog, boolean checkScheduleTime) {
        if (doseLog.getScheduleId() == null || doseLog.getDueAt() == null || doseLog.getStatus() == null) {
            throw new ApiException("Required fields are missing or status is invalid");
        }
        DoseSchedule schedule = doseScheduleRepository.findById(doseLog.getScheduleId()).orElseThrow(() -> new ApiException("Dose schedule ID not found"));
        UserItem item = userItemRepository.findById(schedule.getItemId()).orElseThrow(() -> new ApiException("Scheduled item must belong to an existing patient"));
        User patient = userRepository.findById(item.getUserId()).orElseThrow(() -> new ApiException("Scheduled item must belong to an existing patient"));
        if (!"PATIENT".equals(patient.getRole())) {
            throw new ApiException("Scheduled item must belong to an existing patient");
        }
        if (checkScheduleTime) {
            LocalDate date = doseLog.getDueAt().toLocalDate();
            if (date.isBefore(schedule.getStartDate())
                    || (schedule.getEndDate() != null && date.isAfter(schedule.getEndDate()))
                    || !Arrays.asList(schedule.getDaysOfWeek().split(",")).contains(date.getDayOfWeek().name())
                    || !doseLog.getDueAt().toLocalTime().equals(schedule.getLocalTime())) {
                throw new ApiException("Due date and time do not match the schedule");
            }
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if ("TAKEN".equals(doseLog.getStatus())) {
            if (doseLog.getTakenAt() == null) {
                doseLog.setTakenAt(now);
            }
            if (doseLog.getTakenAt().isAfter(now)) {
                throw new ApiException("Only TAKEN doses may have takenAt, and it cannot be in the future");
            }
            return;
        }
        if ("SKIPPED".equals(doseLog.getStatus()) || "MISSED".equals(doseLog.getStatus())) {
            if (doseLog.getTakenAt() != null) {
                throw new ApiException("Only TAKEN doses may have takenAt, and it cannot be in the future");
            }
            if ("MISSED".equals(doseLog.getStatus()) && !doseLog.getDueAt().isBefore(now)) {
                throw new ApiException("A dose can be marked MISSED only after it is due");
            }
            return;
        }
        throw new ApiException("Required fields are missing or status is invalid");
    }
}
