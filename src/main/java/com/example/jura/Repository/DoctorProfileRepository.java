package com.example.jura.Repository;

import com.example.jura.Model.DoctorProfile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Integer> {
    List<DoctorProfile> findBySpecialtyContainingIgnoreCase(String specialty);
}
