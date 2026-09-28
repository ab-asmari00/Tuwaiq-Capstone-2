package com.example.jura.Repository;

import com.example.jura.Model.Appointment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByPatientIdOrderByStartAtDesc(Integer patientId);
    List<Appointment> findByDoctorIdOrderByStartAtDesc(Integer doctorId);
    List<Appointment> findByDoctorIdAndStatusOrderByStartAtAsc(Integer doctorId, String status);
    List<Appointment> findByPatientIdOrDoctorId(Integer patientId, Integer doctorId);
}
