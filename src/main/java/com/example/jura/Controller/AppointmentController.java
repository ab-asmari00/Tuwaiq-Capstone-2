package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.Appointment;
import com.example.jura.Service.AppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/get-by-patient/{patientId}")
    public ResponseEntity<?> patientAppointments(@PathVariable Integer patientId) {
        List<Appointment> appointments = appointmentService.getPatientAppointments(patientId);
        if (appointments == null) return ResponseEntity.status(404).body(new ApiResponse("Patient not found"));
        return ResponseEntity.status(200).body(appointments);
    }

    @GetMapping("/get-by-doctor/{doctorId}")
    public ResponseEntity<?> doctorAppointments(@PathVariable Integer doctorId) {
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId, false);
        if (appointments == null) return ResponseEntity.status(404).body(new ApiResponse("Doctor profile not found"));
        return ResponseEntity.status(200).body(appointments);
    }

    @GetMapping("/get-pending-by-doctor/{doctorId}")
    public ResponseEntity<?> doctorRequests(@PathVariable Integer doctorId) {
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId, true);
        if (appointments == null) return ResponseEntity.status(404).body(new ApiResponse("Doctor profile not found"));
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
        if (appointment == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Appointment ID not found"));
        }
        return ResponseEntity.status(200).body(appointment);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAppointment(@RequestBody @Valid Appointment appointment, Errors errors) {
        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            return ResponseEntity.status(400).body(new ApiResponse(message));
        }

        int result = appointmentService.addAppointment(appointment);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Patient ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("User is not a patient"));
        }
        if (result == 3) {
            return ResponseEntity.status(404).body(new ApiResponse("Doctor profile not found"));
        }
        if (result == 4) {
            return ResponseEntity.status(400).body(new ApiResponse("End time must be after start time"));
        }
        return ResponseEntity.status(201).body(new ApiResponse("Appointment requested successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Integer id, @RequestBody Appointment appointment) {
        int result = appointmentService.updateAppointment(id, appointment);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Appointment ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Only pending appointments can be updated"));
        }
        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Start and end times are required, and end must be after start"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Appointment updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Integer id) {
        boolean isFound = appointmentService.deleteAppointment(id);
        if (!isFound) {
            return ResponseEntity.status(404).body(new ApiResponse("Appointment ID not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Appointment deleted successfully"));
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveAppointment(@PathVariable Integer id) {
        int result = appointmentService.approveAppointment(id);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Appointment ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Only pending appointments can be approved"));
        }
        if (result == 3) {
            return ResponseEntity.status(503).body(new ApiResponse("Zoom is not configured"));
        }
        if (result == 4) {
            return ResponseEntity.status(502).body(new ApiResponse("Zoom could not create the meeting"));
        }
        if (result == 5) {
            return ResponseEntity.status(409).body(new ApiResponse("Appointment already has a meeting"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Appointment approved successfully"));
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectAppointment(@PathVariable Integer id) {
        int result = appointmentService.rejectAppointment(id);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Appointment ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Only pending appointments can be rejected"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Appointment rejected successfully"));
    }
}
