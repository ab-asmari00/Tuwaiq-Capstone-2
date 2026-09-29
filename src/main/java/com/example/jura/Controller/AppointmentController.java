package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.Appointment;
import com.example.jura.Service.AppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/get-by-patient/{patientId}")
    public ResponseEntity<?> patientAppointments(@PathVariable Integer patientId) {
        List<Appointment> appointments = appointmentService.getPatientAppointments(patientId);
        return ResponseEntity.status(200).body(appointments);
    }

    @GetMapping("/get-by-doctor/{doctorId}")
    public ResponseEntity<?> doctorAppointments(@PathVariable Integer doctorId) {
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId, false);
        return ResponseEntity.status(200).body(appointments);
    }

    @GetMapping("/get-pending-by-doctor/{doctorId}")
    public ResponseEntity<?> doctorRequests(@PathVariable Integer doctorId) {
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId, true);
        return ResponseEntity.status(200).body(appointments);
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllAppointments() {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        if (appointments.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Appointments list is empty"));
        }
        return ResponseEntity.status(200).body(appointments);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Integer id) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.status(200).body(appointment);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAppointment(@RequestBody @Valid Appointment appointment) {
        appointmentService.addAppointment(appointment);
        return ResponseEntity.status(201).body(new ApiResponse("Appointment requested successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Integer id, @RequestBody Appointment appointment) {
        appointmentService.updateAppointment(id, appointment);
        return ResponseEntity.status(200).body(new ApiResponse("Appointment updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Integer id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.status(200).body(new ApiResponse("Appointment deleted successfully"));
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveAppointment(@PathVariable Integer id) {
        appointmentService.approveAppointment(id);
        return ResponseEntity.status(200).body(new ApiResponse("Appointment approved successfully"));
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectAppointment(@PathVariable Integer id) {
        appointmentService.rejectAppointment(id);
        return ResponseEntity.status(200).body(new ApiResponse("Appointment rejected successfully"));
    }
}
