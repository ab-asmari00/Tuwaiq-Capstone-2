package com.example.jura.Service;

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
        return doseScheduleRepository.findById(id).orElse(null);
    }

    public List<DoseSchedule> getDoseSchedulesByItem(Integer itemId) {
        if (!userItemRepository.existsById(itemId)) {
            return null;
        }
        return doseScheduleRepository.findByItemIdOrderByLocalTimeAsc(itemId);
    }

    @Transactional
    public int addDoseSchedule(DoseSchedule schedule) {
        int result = validateSchedule(schedule);
        if (result != 0) {
            return result; // Schedule details are invalid
        }
        schedule.setId(null);
        doseScheduleRepository.save(schedule);
        return 0; // Dose schedule added successfully
    }

    @Transactional
    public int updateDoseSchedule(Integer id, DoseSchedule schedule) {
        DoseSchedule oldSchedule = doseScheduleRepository.findById(id).orElse(null);
        if (oldSchedule == null) {
            return 1; // Dose schedule ID not found
        }
        if (!oldSchedule.getItemId().equals(schedule.getItemId())) {
            return 7; // Item ID cannot be changed
        }
        int result = validateSchedule(schedule);
        if (result != 0) {
            return result; // Schedule details are invalid
        }
        oldSchedule.setLocalTime(schedule.getLocalTime());
        oldSchedule.setDaysOfWeek(schedule.getDaysOfWeek());
        oldSchedule.setStartDate(schedule.getStartDate());
        oldSchedule.setEndDate(schedule.getEndDate());
        doseScheduleRepository.save(oldSchedule);
        return 0; // Dose schedule updated successfully
    }

    @Transactional
    public boolean deleteDoseSchedule(Integer id) {
        DoseSchedule schedule = doseScheduleRepository.findById(id).orElse(null);
        if (schedule == null) {
            return false;
        }
        doseLogRepository.deleteByScheduleId(id);
        doseScheduleRepository.delete(schedule);
        return true;
    }

    @Transactional
    public void deleteDoseSchedulesByItem(Integer itemId) {
        List<DoseSchedule> schedules = doseScheduleRepository.findByItemIdOrderByLocalTimeAsc(itemId);
        for (DoseSchedule schedule : schedules) {
            doseLogRepository.deleteByScheduleId(schedule.getId());
        }
        doseScheduleRepository.deleteAll(schedules);
    }

    private int validateSchedule(DoseSchedule schedule) {
        if (schedule.getItemId() == null || schedule.getLocalTime() == null
                || schedule.getStartDate() == null || schedule.getDaysOfWeek() == null
                || schedule.getDaysOfWeek().isBlank()) {
            return 9; // Required schedule fields are missing
        }
        UserItem item = userItemRepository.findById(schedule.getItemId()).orElse(null);
        if (item == null) {
            return 2; // User item ID not found
        }
        if (!Boolean.TRUE.equals(item.getActive())) {
            return 3; // User item must be active
        }
        User patient = userRepository.findUserById(item.getUserId());
        if (patient == null || !"PATIENT".equals(patient.getRole())) {
            return 8; // Item must belong to an existing patient
        }
        if (schedule.getEndDate() != null && schedule.getEndDate().isBefore(schedule.getStartDate())) {
            return 5; // End date cannot be before start date
        }
        if (schedule.getDaysOfWeek().length() > 100) {
            return 4; // Days of week are invalid
        }
        List<String> validDays = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        Set<String> selectedDays = new HashSet<>();
        for (String day : schedule.getDaysOfWeek().split(",", -1)) {
            if (!validDays.contains(day)) {
                return 4; // Use uppercase day names separated by commas, without spaces
            }
            if (!selectedDays.add(day)) {
                return 6; // The same day cannot be repeated
            }
        }
        return 0; // Schedule details are valid
    }
}
