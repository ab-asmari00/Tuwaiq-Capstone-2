package com.example.jura.Repository;

import com.example.jura.Model.DrugCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrugCacheRepository extends JpaRepository<DrugCache, Integer> {

    boolean existsBySfdaRegNo(String regNo);

    boolean existsBySfdaRegNoAndIdNot(String regNo, Integer id);

    DrugCache findDrugCacheBySfdaRegNo(String sfdaRegNo);

    Page<DrugCache> findBySearchNameArContaining(String name, Pageable pageable);

    Page<DrugCache> findByTradeNameEnContainingIgnoreCase(String name, Pageable pageable);
}
