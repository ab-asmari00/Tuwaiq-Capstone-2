package com.example.jura.Repository;

import com.example.jura.Model.DoseLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoseLogRepository extends JpaRepository<DoseLog, Integer> {

    List<DoseLog> findByScheduleIdOrderByDueAtDesc(Integer scheduleId);

    List<DoseLog> findByScheduleIdInOrderByDueAtDesc(List<Integer> scheduleIds);

    boolean existsByScheduleIdAndDueAt(Integer scheduleId, LocalDateTime dueAt);

    DoseLog findByScheduleIdAndDueAt(Integer scheduleId, LocalDateTime dueAt);

    void deleteByScheduleId(Integer scheduleId);
}
