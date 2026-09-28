package com.example.jura.Repository;

import com.example.jura.Model.DoseSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoseScheduleRepository extends JpaRepository<DoseSchedule, Integer> {

    List<DoseSchedule> findByItemIdOrderByLocalTimeAsc(Integer itemId);
}
