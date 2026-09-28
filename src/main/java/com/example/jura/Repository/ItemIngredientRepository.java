package com.example.jura.Repository;

import com.example.jura.Model.ItemIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemIngredientRepository extends JpaRepository<ItemIngredient, Integer> {

    List<ItemIngredient> findByItemId(Integer itemId);

    long countByItemId(Integer itemId);

    boolean existsByItemIdAndNameEnIgnoreCase(Integer itemId, String nameEn);

    boolean existsByItemIdAndNameEnIgnoreCaseAndIdNot(Integer itemId, String nameEn, Integer id);

    void deleteByItemId(Integer itemId);
}
