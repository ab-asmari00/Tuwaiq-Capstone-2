package com.example.jura.Repository;

import com.example.jura.Model.AiInteractionResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiInteractionResultRepository extends JpaRepository<AiInteractionResult, Integer> {

    List<AiInteractionResult> findByItemAIdOrItemBIdOrderByCheckedAtDesc(Integer itemAId, Integer itemBId);

    void deleteByItemAIdOrItemBId(Integer itemAId, Integer itemBId);
}
