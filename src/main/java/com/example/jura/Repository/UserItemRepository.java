package com.example.jura.Repository;

import com.example.jura.Model.UserItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Integer> {

    List<UserItem> findByUserId(Integer userId);

    boolean existsByDrugCacheId(Integer drugCacheId);

    List<UserItem> findByUserIdAndActiveTrue(Integer userId);
}
