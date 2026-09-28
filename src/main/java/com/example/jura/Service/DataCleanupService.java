package com.example.jura.Service;
import com.example.jura.Model.*;
import com.example.jura.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataCleanupService {
    private final UserItemRepository itemRepository;
    private final UserItemService itemService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final DoctorProfileRepository doctorRepository;

    @Transactional
    public void cleanupUser(Integer userId) {
        for (UserItem item : itemRepository.findByUserId(userId)) itemService.deleteUserItem(item.getId());
        for (Appointment appointment : appointmentRepository.findByPatientIdOrDoctorId(userId, userId))
            appointmentService.deleteAppointment(appointment.getId());
        if (doctorRepository.existsById(userId)) doctorRepository.deleteById(userId);
    }

    @Transactional
    public void cleanupDoctorAppointments(Integer doctorId) {
        for (Appointment appointment : appointmentRepository.findByDoctorIdOrderByStartAtDesc(doctorId))
            appointmentService.deleteAppointment(appointment.getId());
    }
}
