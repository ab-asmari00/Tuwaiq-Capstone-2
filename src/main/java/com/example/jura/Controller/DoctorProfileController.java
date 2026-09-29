package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.DoctorProfile;
import com.example.jura.Service.DoctorProfileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/doctor")
@RequiredArgsConstructor
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    @GetMapping("/search")
    public ResponseEntity<?> searchDoctors(@RequestParam String specialty) {
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
        return ResponseEntity.status(200).body(profile);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDoctorProfile(@RequestBody @Valid DoctorProfile doctorProfile) {
        doctorProfileService.addDoctorProfile(doctorProfile);
        return ResponseEntity.status(201).body(new ApiResponse("Doctor profile added successfully"));
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<?> updateDoctorProfile(@PathVariable Integer userId,
                                                  @RequestBody @Valid DoctorProfile doctorProfile) {
        doctorProfileService.updateDoctorProfile(userId, doctorProfile);
        return ResponseEntity.status(200).body(new ApiResponse("Doctor profile updated successfully"));
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteDoctorProfile(@PathVariable Integer userId) {
        doctorProfileService.deleteDoctorProfile(userId);
        return ResponseEntity.status(200).body(new ApiResponse("Doctor profile deleted successfully"));
    }
}
