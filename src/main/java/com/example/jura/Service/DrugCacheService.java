package com.example.jura.Service;

import com.example.jura.Api.ApiException;
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
    public DrugCache getDrugCacheById(Integer id) { return drugCacheRepository.findById(id).orElseThrow(() -> new ApiException("Drug cache ID not found")); }

    public void addDrugCache(DrugCache drug) {
        if (!hasTradeName(drug)) throw new ApiException("At least one trade name is required");
        drug.setSfdaRegNo(drug.getSfdaRegNo().trim());
        if (drugCacheRepository.existsBySfdaRegNo(drug.getSfdaRegNo())) throw new ApiException("Registration number already exists");
        drug.setId(null);
        prepare(drug);
        drugCacheRepository.save(drug);
    }

    public void updateDrugCache(Integer id, DrugCache drug) {
        DrugCache old = getDrugCacheById(id);
        if (userItemRepository.existsByDrugCacheId(id)) throw new ApiException("Referenced drug cache cannot be edited through course CRUD");
        if (!hasTradeName(drug)) throw new ApiException("At least one trade name is required");
        String regNo = drug.getSfdaRegNo().trim();
        if (drugCacheRepository.existsBySfdaRegNoAndIdNot(regNo, id)) throw new ApiException("Registration number already exists");
        old.setSfdaRegNo(regNo);
        old.setTradeNameAr(drug.getTradeNameAr());
        old.setTradeNameEn(drug.getTradeNameEn());
        old.setScientificNameRaw(drug.getScientificNameRaw());
        prepare(old);
        drugCacheRepository.save(old);
    }

    public void deleteDrugCache(Integer id) {
        DrugCache drug = getDrugCacheById(id);
        if (userItemRepository.existsByDrugCacheId(id)) throw new ApiException("Drug cache is referenced by a user item");
        drugCacheRepository.delete(drug);
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
