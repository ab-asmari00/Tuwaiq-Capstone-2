package com.example.jura.Service;

import com.example.jura.Api.ApiException;
import com.example.jura.Model.DoctorProfile;
import com.example.jura.Model.User;
import com.example.jura.Repository.DoctorProfileRepository;
import com.example.jura.Repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorProfileService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final DataCleanupService dataCleanupService;

    public List<DoctorProfile> searchDoctors(String specialty) {
        if (specialty == null || specialty.isBlank() || specialty.length() > 100) {
            throw new ApiException("Specialty must be 1 to 100 characters");
        }
        return doctorProfileRepository.findBySpecialtyContainingIgnoreCase(specialty.trim());
    }

    public List<DoctorProfile> getAllDoctorProfiles() {
        return doctorProfileRepository.findAll();
    }

    public DoctorProfile getDoctorProfileByUserId(Integer userId) {
        return doctorProfileRepository.findById(userId).orElseThrow(() -> new ApiException("Doctor profile not found"));
    }

    public void addDoctorProfile(DoctorProfile doctorProfile) {
        User user = userRepository.findById(doctorProfile.getUserId()).orElseThrow(() -> new ApiException("User ID not found"));

        if (!"DOCTOR".equals(user.getRole())) {
            throw new ApiException("User is not a doctor");
        }

        if (doctorProfileRepository.existsById(doctorProfile.getUserId())) {
            throw new ApiException("Doctor profile already exists");
        }

        doctorProfileRepository.save(doctorProfile);
    }

    public void updateDoctorProfile(Integer userId, DoctorProfile doctorProfile) {
        if (!userId.equals(doctorProfile.getUserId())) {
            throw new ApiException("User ID in body must match URL");
        }
        DoctorProfile oldProfile = doctorProfileRepository.findById(userId).orElseThrow(() -> new ApiException("Doctor profile not found"));

        oldProfile.setSpecialty(doctorProfile.getSpecialty());
        oldProfile.setBio(doctorProfile.getBio());
        doctorProfileRepository.save(oldProfile);
    }

    @Transactional
    public void deleteDoctorProfile(Integer userId) {
        DoctorProfile doctorProfile = doctorProfileRepository.findById(userId).orElseThrow(() -> new ApiException("Doctor profile not found"));

        dataCleanupService.cleanupDoctorAppointments(userId);
        doctorProfileRepository.delete(doctorProfile);
    }
}
