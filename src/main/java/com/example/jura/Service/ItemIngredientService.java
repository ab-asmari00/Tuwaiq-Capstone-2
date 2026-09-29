package com.example.jura.Service;

import com.example.jura.Api.ApiException;
import com.example.jura.Api.SupplementRequest;
import com.example.jura.Model.DrugCache;
import com.example.jura.Model.ItemIngredient;
import com.example.jura.Model.UserItem;
import com.example.jura.Repository.DrugCacheRepository;
import com.example.jura.Repository.ItemIngredientRepository;
import com.example.jura.Repository.UserItemRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemIngredientService {

    private final ItemIngredientRepository itemIngredientRepository;
    private final UserItemRepository userItemRepository;
    private final DrugCacheRepository drugCacheRepository;

    public List<ItemIngredient> getAllItemIngredients() {
        return itemIngredientRepository.findAll();
    }

    public ItemIngredient getItemIngredientById(Integer id) {
        return itemIngredientRepository.findById(id).orElseThrow(() -> new ApiException("Ingredient ID not found"));
    }

    public List<ItemIngredient> getIngredientsByItemId(Integer itemId) {
        if (!userItemRepository.existsById(itemId)) {
            throw new ApiException("User item ID not found");
        }
        return itemIngredientRepository.findByItemId(itemId);
    }

    @Transactional
    public void addItemIngredient(ItemIngredient ingredient) {
        UserItem item = userItemRepository.findById(ingredient.getItemId()).orElseThrow(() -> new ApiException("User item ID not found"));
        if (!"SUPPLEMENT".equals(item.getType())) {
            throw new ApiException("Drug ingredients must come from SFDA");
        }
        ingredient.setNameEn(cleanName(ingredient.getNameEn()));
        if (itemIngredientRepository.existsByItemIdAndNameEnIgnoreCase(item.getId(), ingredient.getNameEn())) {
            throw new ApiException("Ingredient already exists for this item");
        }
        ingredient.setId(null);
        ingredient.setNameAr(cleanOptionalName(ingredient.getNameAr()));
        itemIngredientRepository.save(ingredient);
    }

    @Transactional
    public void updateItemIngredient(Integer id, ItemIngredient ingredient) {
        ItemIngredient oldIngredient = itemIngredientRepository.findById(id).orElseThrow(() -> new ApiException("Ingredient ID not found"));
        if (!oldIngredient.getItemId().equals(ingredient.getItemId())) {
            throw new ApiException("Item ID cannot be changed");
        }
        UserItem item = userItemRepository.findById(oldIngredient.getItemId()).orElse(null);
        if (item == null || !"SUPPLEMENT".equals(item.getType())) {
            throw new ApiException("Drug ingredients must come from SFDA");
        }
        String name = cleanName(ingredient.getNameEn());
        if (itemIngredientRepository.existsByItemIdAndNameEnIgnoreCaseAndIdNot(item.getId(), name, id)) {
            throw new ApiException("Ingredient already exists for this item");
        }
        oldIngredient.setNameEn(name);
        oldIngredient.setNameAr(cleanOptionalName(ingredient.getNameAr()));
        itemIngredientRepository.save(oldIngredient);
    }

    @Transactional
    public void deleteItemIngredient(Integer id) {
        ItemIngredient ingredient = itemIngredientRepository.findById(id).orElseThrow(() -> new ApiException("Ingredient ID not found"));
        UserItem item = userItemRepository.findById(ingredient.getItemId()).orElse(null);
        if (item != null && !"SUPPLEMENT".equals(item.getType())) {
            throw new ApiException("Drug ingredients must come from SFDA");
        }
        if (item != null && itemIngredientRepository.countByItemId(item.getId()) <= 1) {
            throw new ApiException("A supplement must keep at least one ingredient");
        }
        itemIngredientRepository.delete(ingredient);
    }

    public void validateSupplementIngredients(List<SupplementRequest.IngredientInput> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new ApiException("At least one ingredient is required");
        }
        Set<String> names = new HashSet<>();
        for (SupplementRequest.IngredientInput ingredient : ingredients) {
            if (ingredient == null || ingredient.getNameEn() == null || ingredient.getNameEn().isBlank()
                    || ingredient.getNameEn().length() > 255
                    || (ingredient.getNameAr() != null && ingredient.getNameAr().length() > 255)) {
                throw new ApiException("Ingredient names are missing or invalid");
            }
            if (!names.add(cleanName(ingredient.getNameEn()).toLowerCase(Locale.ROOT))) {
                throw new ApiException("Duplicate ingredient in the request");
            }
        }
    }

    public void saveSupplementIngredients(Integer itemId, List<SupplementRequest.IngredientInput> ingredients) {
        List<ItemIngredient> rows = new ArrayList<>();
        for (SupplementRequest.IngredientInput ingredient : ingredients) {
            rows.add(new ItemIngredient(null, itemId, cleanName(ingredient.getNameEn()),
                    cleanOptionalName(ingredient.getNameAr())));
        }
        itemIngredientRepository.saveAll(rows);
    }

    @Transactional
    public void syncDrugIngredients(Integer itemId) {
        DrugCache drug = getDrugForItem(itemId);
        List<ItemIngredient> rows = parseDrugIngredients(itemId, drug.getScientificNameRaw());
        if (rows.isEmpty()) {
            throw new ApiException("SFDA ingredient data is unavailable or cannot be stored reliably");
        }
        itemIngredientRepository.deleteByItemId(itemId);
        itemIngredientRepository.saveAll(rows);
    }

    @Transactional
    public void syncDrugIngredientsIfAvailable(Integer itemId) {
        DrugCache drug = getDrugForItem(itemId);
        List<ItemIngredient> rows = parseDrugIngredients(itemId, drug.getScientificNameRaw());
        itemIngredientRepository.deleteByItemId(itemId);
        if (!rows.isEmpty()) itemIngredientRepository.saveAll(rows);
        // An empty ingredient list makes an interaction assessment UNKNOWN, not safe.
    }

    private DrugCache getDrugForItem(Integer itemId) {
        UserItem item = userItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException("User item ID not found"));
        if (!"DRUG".equals(item.getType())) throw new ApiException("Item is not a drug");
        if (item.getDrugCacheId() == null) throw new ApiException("Drug cache ID not found");
        return drugCacheRepository.findById(item.getDrugCacheId())
                .orElseThrow(() -> new ApiException("Drug cache ID not found"));
    }

    private List<ItemIngredient> parseDrugIngredients(Integer itemId, String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        Map<String, ItemIngredient> rows = new LinkedHashMap<>();
        for (String part : raw.split(",", -1)) {
            String name = cleanName(part);
            if (name.isEmpty() || name.length() > 255) return List.of();
            rows.putIfAbsent(name.toLowerCase(Locale.ROOT), new ItemIngredient(null, itemId, name, null));
        }
        return new ArrayList<>(rows.values());
    }

    @Transactional
    public void deleteIngredientsByItemId(Integer itemId) {
        itemIngredientRepository.deleteByItemId(itemId);
    }

    private static String cleanName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private static String cleanOptionalName(String name) {
        return name == null || name.isBlank() ? null : cleanName(name);
    }
}
