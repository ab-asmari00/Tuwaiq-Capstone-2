package com.example.jura.Repository;

import com.example.jura.Model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    void deleteByAppointmentId(Integer appointmentId);

    Meeting findMeetingByAppointmentId(Integer appointmentId);

    boolean existsByAppointmentId(Integer appointmentId);
}
