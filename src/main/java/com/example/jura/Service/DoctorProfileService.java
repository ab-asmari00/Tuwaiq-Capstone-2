package com.example.jura.Service;

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
        return doctorProfileRepository.findBySpecialtyContainingIgnoreCase(specialty.trim());
    }

    public List<DoctorProfile> getAllDoctorProfiles() {
        return doctorProfileRepository.findAll();
    }

    public DoctorProfile getDoctorProfileByUserId(Integer userId) {
        return doctorProfileRepository.findById(userId).orElse(null);
    }

    public int addDoctorProfile(DoctorProfile doctorProfile) {
        User user = userRepository.findUserById(doctorProfile.getUserId());
        if (user == null) {
            return 1; // User ID not found
        }

        if (!"DOCTOR".equals(user.getRole())) {
            return 2; // User is not a doctor
        }

        if (doctorProfileRepository.existsById(doctorProfile.getUserId())) {
            return 3; // Doctor profile already exists
        }

        doctorProfileRepository.save(doctorProfile);
        return 0; // Doctor profile added successfully
    }

    public int updateDoctorProfile(Integer userId, DoctorProfile doctorProfile) {
        DoctorProfile oldProfile = doctorProfileRepository.findById(userId).orElse(null);
        if (oldProfile == null) {
            return 1; // Doctor profile not found
        }

        oldProfile.setSpecialty(doctorProfile.getSpecialty());
        oldProfile.setBio(doctorProfile.getBio());
        doctorProfileRepository.save(oldProfile);
        return 0; // Doctor profile updated successfully
    }

    @Transactional
    public boolean deleteDoctorProfile(Integer userId) {
        DoctorProfile doctorProfile = doctorProfileRepository.findById(userId).orElse(null);
        if (doctorProfile == null) {
            return false;
        }

        dataCleanupService.cleanupDoctorAppointments(userId);
        doctorProfileRepository.delete(doctorProfile);
        return true;
    }
}
