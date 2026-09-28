package com.example.jura.Service;

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
        return itemIngredientRepository.findById(id).orElse(null);
    }

    public List<ItemIngredient> getIngredientsByItemId(Integer itemId) {
        if (!userItemRepository.existsById(itemId)) {
            return null;
        }
        return itemIngredientRepository.findByItemId(itemId);
    }

    @Transactional
    public int addItemIngredient(ItemIngredient ingredient) {
        UserItem item = userItemRepository.findById(ingredient.getItemId()).orElse(null);
        if (item == null) {
            return 1; // User item ID not found
        }
        if (!"SUPPLEMENT".equals(item.getType())) {
            return 2; // Drug ingredients must come from SFDA
        }
        ingredient.setNameEn(cleanName(ingredient.getNameEn()));
        if (itemIngredientRepository.existsByItemIdAndNameEnIgnoreCase(item.getId(), ingredient.getNameEn())) {
            return 3; // Ingredient already exists for this item
        }
        ingredient.setId(null);
        ingredient.setNameAr(cleanOptionalName(ingredient.getNameAr()));
        itemIngredientRepository.save(ingredient);
        return 0; // Ingredient added successfully
    }

    @Transactional
    public int updateItemIngredient(Integer id, ItemIngredient ingredient) {
        ItemIngredient oldIngredient = itemIngredientRepository.findById(id).orElse(null);
        if (oldIngredient == null) {
            return 1; // Ingredient ID not found
        }
        if (!oldIngredient.getItemId().equals(ingredient.getItemId())) {
            return 2; // Item ID cannot be changed
        }
        UserItem item = userItemRepository.findById(oldIngredient.getItemId()).orElse(null);
        if (item == null || !"SUPPLEMENT".equals(item.getType())) {
            return 3; // Drug ingredients must come from SFDA
        }
        String name = cleanName(ingredient.getNameEn());
        if (itemIngredientRepository.existsByItemIdAndNameEnIgnoreCaseAndIdNot(item.getId(), name, id)) {
            return 4; // Ingredient already exists for this item
        }
        oldIngredient.setNameEn(name);
        oldIngredient.setNameAr(cleanOptionalName(ingredient.getNameAr()));
        itemIngredientRepository.save(oldIngredient);
        return 0; // Ingredient updated successfully
    }

    @Transactional
    public int deleteItemIngredient(Integer id) {
        ItemIngredient ingredient = itemIngredientRepository.findById(id).orElse(null);
        if (ingredient == null) {
            return 1; // Ingredient ID not found
        }
        UserItem item = userItemRepository.findById(ingredient.getItemId()).orElse(null);
        if (item != null && !"SUPPLEMENT".equals(item.getType())) {
            return 2; // Drug ingredients must come from SFDA
        }
        if (item != null && itemIngredientRepository.countByItemId(item.getId()) <= 1) {
            return 3; // A supplement must keep at least one ingredient
        }
        itemIngredientRepository.delete(ingredient);
        return 0; // Ingredient deleted successfully
    }

    public int validateSupplementIngredients(List<SupplementRequest.IngredientInput> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return 11; // At least one ingredient is required
        }
        Set<String> names = new HashSet<>();
        for (SupplementRequest.IngredientInput ingredient : ingredients) {
            if (ingredient == null || ingredient.getNameEn() == null || ingredient.getNameEn().isBlank()
                    || ingredient.getNameEn().length() > 255
                    || (ingredient.getNameAr() != null && ingredient.getNameAr().length() > 255)) {
                return 11; // Ingredient names are missing or invalid
            }
            if (!names.add(cleanName(ingredient.getNameEn()).toLowerCase(Locale.ROOT))) {
                return 12; // Duplicate ingredient in the request
            }
        }
        return 0; // Ingredient list is valid
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
    public int syncDrugIngredients(Integer itemId) {
        UserItem item = userItemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return 1; // User item ID not found
        }
        if (!"DRUG".equals(item.getType())) {
            return 2; // Item is not a drug
        }
        if (item.getDrugCacheId() == null) {
            return 3; // Drug cache ID not found
        }
        DrugCache drug = drugCacheRepository.findById(item.getDrugCacheId()).orElse(null);
        if (drug == null) {
            return 3; // Drug cache ID not found
        }

        itemIngredientRepository.deleteByItemId(itemId);
        String raw = drug.getScientificNameRaw();
        if (raw == null || raw.isBlank()) {
            return 4; // SFDA ingredient data is unavailable
        }

        // SFDA's scientificName field lists combination ingredients separated by commas.
        Map<String, ItemIngredient> rows = new LinkedHashMap<>();
        for (String part : raw.split(",", -1)) {
            String name = cleanName(part);
            if (name.isEmpty() || name.length() > 255) {
                return 4; // SFDA ingredient data cannot be stored reliably
            }
            rows.putIfAbsent(name.toLowerCase(Locale.ROOT), new ItemIngredient(null, itemId, name, null));
        }
        if (rows.isEmpty()) {
            return 4; // SFDA ingredient data is unavailable
        }
        itemIngredientRepository.saveAll(rows.values());
        return 0; // Drug ingredients synced successfully
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
