package com.example.jura.Service;

import com.example.jura.Api.ApiException;
import com.example.jura.Model.DoseSchedule;
import com.example.jura.Model.User;
import com.example.jura.Model.UserItem;
import com.example.jura.Repository.DoseLogRepository;
import com.example.jura.Repository.DoseScheduleRepository;
import com.example.jura.Repository.UserItemRepository;
import com.example.jura.Repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoseScheduleService {

    private final DoseScheduleRepository doseScheduleRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final DoseLogRepository doseLogRepository;

    public List<DoseSchedule> getAllDoseSchedules() {
        return doseScheduleRepository.findAll();
    }

    public DoseSchedule getDoseScheduleById(Integer id) {
        return doseScheduleRepository.findById(id).orElseThrow(() -> new ApiException("Dose schedule ID not found"));
    }

    public List<DoseSchedule> getDoseSchedulesByItem(Integer itemId) {
        if (!userItemRepository.existsById(itemId)) {
            throw new ApiException("User item ID not found");
        }
        return doseScheduleRepository.findByItemIdOrderByLocalTimeAsc(itemId);
    }

    @Transactional
    public void addDoseSchedule(DoseSchedule schedule) {
        validateSchedule(schedule);
        schedule.setId(null);
        doseScheduleRepository.save(schedule);
    }

    @Transactional
    public void updateDoseSchedule(Integer id, DoseSchedule schedule) {
        DoseSchedule oldSchedule = doseScheduleRepository.findById(id).orElseThrow(() -> new ApiException("Dose schedule ID not found"));
        if (!oldSchedule.getItemId().equals(schedule.getItemId())) {
            throw new ApiException("Item ID cannot be changed");
        }
        validateSchedule(schedule);
        oldSchedule.setLocalTime(schedule.getLocalTime());
        oldSchedule.setDaysOfWeek(schedule.getDaysOfWeek());
        oldSchedule.setStartDate(schedule.getStartDate());
        oldSchedule.setEndDate(schedule.getEndDate());
        doseScheduleRepository.save(oldSchedule);
    }

    @Transactional
    public void deleteDoseSchedule(Integer id) {
        DoseSchedule schedule = doseScheduleRepository.findById(id).orElseThrow(() -> new ApiException("Dose schedule ID not found"));
        doseLogRepository.deleteByScheduleId(id);
        doseScheduleRepository.delete(schedule);
    }

    @Transactional
    public void deleteDoseSchedulesByItem(Integer itemId) {
        List<DoseSchedule> schedules = doseScheduleRepository.findByItemIdOrderByLocalTimeAsc(itemId);
        for (DoseSchedule schedule : schedules) {
            doseLogRepository.deleteByScheduleId(schedule.getId());
        }
        doseScheduleRepository.deleteAll(schedules);
    }

    private void validateSchedule(DoseSchedule schedule) {
        if (schedule.getItemId() == null || schedule.getLocalTime() == null
                || schedule.getStartDate() == null || schedule.getDaysOfWeek() == null
                || schedule.getDaysOfWeek().isBlank()) {
            throw new ApiException("Required schedule fields are missing");
        }
        UserItem item = userItemRepository.findById(schedule.getItemId()).orElseThrow(() -> new ApiException("User item ID not found"));
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new ApiException("User item must be active");
        }
        User patient = userRepository.findById(item.getUserId()).orElseThrow(() -> new ApiException("Item must belong to an existing patient"));
        if (!"PATIENT".equals(patient.getRole())) {
            throw new ApiException("Item must belong to an existing patient");
        }
        if (schedule.getEndDate() != null && schedule.getEndDate().isBefore(schedule.getStartDate())) {
            throw new ApiException("End date cannot be before start date");
        }
        if (schedule.getDaysOfWeek().length() > 100) {
            throw new ApiException("Days of week are invalid");
        }
        List<String> validDays = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        Set<String> selectedDays = new HashSet<>();
        for (String day : schedule.getDaysOfWeek().split(",", -1)) {
            if (!validDays.contains(day)) {
                throw new ApiException("Use uppercase day names separated by commas, without spaces");
            }
            if (!selectedDays.add(day)) {
                throw new ApiException("The same day cannot be repeated");
            }
        }
    }
}
