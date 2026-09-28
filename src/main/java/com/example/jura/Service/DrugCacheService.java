package com.example.jura.Service;

import com.example.jura.Model.DrugCache;
import com.example.jura.Repository.DrugCacheRepository;
import com.example.jura.Repository.UserItemRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrugCacheService {
    private final DrugCacheRepository drugCacheRepository;
    private final UserItemRepository userItemRepository;
    private final Clock clock;

    public List<DrugCache> getAllDrugCaches() { return drugCacheRepository.findAll(); }
    public DrugCache getDrugCacheById(Integer id) { return drugCacheRepository.findById(id).orElse(null); }

    public int addDrugCache(DrugCache drug) {
        if (!hasTradeName(drug)) return 4; // At least one trade name is required
        drug.setSfdaRegNo(drug.getSfdaRegNo().trim());
        if (drugCacheRepository.existsBySfdaRegNo(drug.getSfdaRegNo())) return 2; // Registration number already exists
        drug.setId(null);
        prepare(drug);
        drugCacheRepository.save(drug);
        return 0; // Local cache record added
    }

    public int updateDrugCache(Integer id, DrugCache drug) {
        DrugCache old = getDrugCacheById(id);
        if (old == null) return 1; // Cache ID not found
        if (userItemRepository.existsByDrugCacheId(id)) return 3; // Referenced drug cache cannot be edited through course CRUD
        if (!hasTradeName(drug)) return 4; // At least one trade name is required
        String regNo = drug.getSfdaRegNo().trim();
        if (drugCacheRepository.existsBySfdaRegNoAndIdNot(regNo, id)) return 2; // Registration number already exists
        old.setSfdaRegNo(regNo);
        old.setTradeNameAr(drug.getTradeNameAr());
        old.setTradeNameEn(drug.getTradeNameEn());
        old.setScientificNameRaw(drug.getScientificNameRaw());
        prepare(old);
        drugCacheRepository.save(old);
        return 0; // Local cache record updated
    }

    public int deleteDrugCache(Integer id) {
        DrugCache drug = getDrugCacheById(id);
        if (drug == null) return 1; // Cache ID not found
        if (userItemRepository.existsByDrugCacheId(id)) return 3; // Drug cache is referenced by a user item
        drugCacheRepository.delete(drug);
        return 0; // Local cache record deleted
    }

    private boolean hasTradeName(DrugCache drug) {
        return (drug.getTradeNameAr() != null && !drug.getTradeNameAr().isBlank())
                || (drug.getTradeNameEn() != null && !drug.getTradeNameEn().isBlank());
    }
    private void prepare(DrugCache drug) {
        drug.setSearchNameAr(DrugSearchService.normalizeArabic(drug.getTradeNameAr()));
        drug.setSyncedAt(LocalDateTime.now(clock));
    }
}
