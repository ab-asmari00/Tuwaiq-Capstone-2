package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DoctorProfile;
import com.example.jura.Service.DoctorProfileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/doctor")
@RequiredArgsConstructor
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    @GetMapping("/search")
    public ResponseEntity<?> searchDoctors(@RequestParam String specialty) {
        if (specialty.isBlank() || specialty.length() > 100) return ResponseEntity.status(400).body(new ApiResponse("Specialty must be 1 to 100 characters"));
        return ResponseEntity.status(200).body(doctorProfileService.searchDoctors(specialty));
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllDoctorProfiles() {
        List<DoctorProfile> profiles = doctorProfileService.getAllDoctorProfiles();
        if (profiles.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Doctor profiles list is empty"));
        }
        return ResponseEntity.status(200).body(profiles);
    }

    @GetMapping("/get-by-user-id/{userId}")
    public ResponseEntity<?> getDoctorProfileByUserId(@PathVariable Integer userId) {
        DoctorProfile profile = doctorProfileService.getDoctorProfileByUserId(userId);
        if (profile == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Doctor profile not found"));
        }
        return ResponseEntity.status(200).body(profile);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDoctorProfile(@RequestBody @Valid DoctorProfile doctorProfile, Errors errors) {
        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            return ResponseEntity.status(400).body(new ApiResponse(message));
        }

        int result = doctorProfileService.addDoctorProfile(doctorProfile);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("User is not a doctor"));
        }
        if (result == 3) {
            return ResponseEntity.status(409).body(new ApiResponse("Doctor profile already exists"));
        }
        return ResponseEntity.status(201).body(new ApiResponse("Doctor profile added successfully"));
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<?> updateDoctorProfile(@PathVariable Integer userId,
                                                  @RequestBody @Valid DoctorProfile doctorProfile,
                                                  Errors errors) {
        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            return ResponseEntity.status(400).body(new ApiResponse(message));
        }

        if (!userId.equals(doctorProfile.getUserId())) {
            return ResponseEntity.status(400).body(new ApiResponse("User ID in body must match URL"));
        }

        int result = doctorProfileService.updateDoctorProfile(userId, doctorProfile);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Doctor profile not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Doctor profile updated successfully"));
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteDoctorProfile(@PathVariable Integer userId) {
        boolean isFound = doctorProfileService.deleteDoctorProfile(userId);
        if (!isFound) {
            return ResponseEntity.status(404).body(new ApiResponse("Doctor profile not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Doctor profile deleted successfully"));
    }
}
