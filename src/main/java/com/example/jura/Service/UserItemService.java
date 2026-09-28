package com.example.jura.Service;

import com.example.jura.Api.SupplementRequest;
import com.example.jura.Model.DrugCache;
import com.example.jura.Model.User;
import com.example.jura.Model.UserItem;
import com.example.jura.Repository.AiInteractionResultRepository;
import com.example.jura.Repository.DrugCacheRepository;
import com.example.jura.Repository.UserItemRepository;
import com.example.jura.Repository.UserRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserItemService {

    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final DrugCacheRepository drugCacheRepository;
    private final ItemIngredientService itemIngredientService;
    private final AiInteractionResultRepository interactionResultRepository;
    private final DoseScheduleService doseScheduleService;

    public List<UserItem> getAllUserItems() {
        return userItemRepository.findAll();
    }

    public UserItem getUserItemById(Integer id) {
        return userItemRepository.findById(id).orElse(null);
    }

    public List<UserItem> getActiveUserItems(Integer userId) {
        User user = userRepository.findUserById(userId);
        if (user == null || !"PATIENT".equals(user.getRole())) {
            return null;
        }
        return userItemRepository.findByUserIdAndActiveTrue(userId);
    }

    @Transactional
    public int addUserItem(UserItem userItem) {
        if ("SUPPLEMENT".equals(userItem.getType())) {
            return 9; // Use add-supplement to include the ingredient list
        }
        return saveNewItem(userItem);
    }

    @Transactional
    public int addSupplement(SupplementRequest request) {
        int result = itemIngredientService.validateSupplementIngredients(request.getIngredients());
        if (result != 0) {
            return result; // Ingredient list is invalid
        }
        UserItem item = new UserItem(null, request.getUserId(), "SUPPLEMENT", request.getDisplayName(),
                null, request.getDosageText(), true);
        result = saveNewItem(item);
        if (result != 0) {
            return result; // Patient or supplement details are invalid
        }
        itemIngredientService.saveSupplementIngredients(item.getId(), request.getIngredients());
        return 0; // Supplement and its ingredients added successfully
    }

    private int saveNewItem(UserItem userItem) {
        User user = userRepository.findUserById(userItem.getUserId());
        if (user == null) {
            return 1; // User ID not found
        }
        if (!"PATIENT".equals(user.getRole())) {
            return 2; // User is not a patient
        }

        int result = prepareItem(userItem);
        if (result != 0) {
            return result; // Invalid drug or supplement details
        }

        userItem.setId(null);
        userItem.setActive(true);
        userItemRepository.save(userItem);
        if ("DRUG".equals(userItem.getType())) {
            itemIngredientService.syncDrugIngredients(userItem.getId());
        }
        return 0; // User item added successfully
    }

    @Transactional
    public int updateUserItem(Integer id, UserItem userItem) {
        UserItem oldItem = userItemRepository.findById(id).orElse(null);
        if (oldItem == null) {
            return 1; // User item ID not found
        }
        if (!oldItem.getUserId().equals(userItem.getUserId())) {
            return 2; // User ID cannot be changed
        }
        if (!oldItem.getType().equals(userItem.getType())) {
            return 10; // Item type cannot be changed
        }

        int result = prepareItem(userItem);
        if (result != 0) {
            return result; // Invalid drug or supplement details
        }

        boolean drugChanged = !Objects.equals(oldItem.getDrugCacheId(), userItem.getDrugCacheId());
        oldItem.setDisplayName(userItem.getDisplayName());
        oldItem.setDrugCacheId(userItem.getDrugCacheId());
        oldItem.setDosageText(userItem.getDosageText());
        if (userItem.getActive() != null) {
            oldItem.setActive(userItem.getActive());
        }
        userItemRepository.save(oldItem);
        if ("DRUG".equals(oldItem.getType()) && drugChanged) {
            itemIngredientService.syncDrugIngredients(oldItem.getId());
        }
        return 0; // User item updated successfully
    }

    @Transactional
    public boolean deleteUserItem(Integer id) {
        UserItem userItem = userItemRepository.findById(id).orElse(null);
        if (userItem == null) {
            return false;
        }
        doseScheduleService.deleteDoseSchedulesByItem(id);
        interactionResultRepository.deleteByItemAIdOrItemBId(id, id);
        itemIngredientService.deleteIngredientsByItemId(id);
        userItemRepository.delete(userItem);
        return true;
    }

    private int prepareItem(UserItem userItem) {
        if ("DRUG".equals(userItem.getType())) {
            if (userItem.getDrugCacheId() == null) {
                return 3; // Drug cache ID is required for a drug
            }
            DrugCache drug = drugCacheRepository.findById(userItem.getDrugCacheId()).orElse(null);
            if (drug == null) {
                return 4; // Drug cache ID not found
            }

            String name = drug.getTradeNameAr();
            if (name == null || name.isBlank()) {
                name = drug.getTradeNameEn();
            }
            if (name == null || name.isBlank()) {
                return 7; // Cached drug has no trade name
            }
            userItem.setDisplayName(name.trim());
            return 0; // Drug details are valid
        }

        if ("SUPPLEMENT".equals(userItem.getType())) {
            if (userItem.getDrugCacheId() != null) {
                return 6; // A supplement cannot have a drug cache ID
            }
            if (userItem.getDisplayName() == null || userItem.getDisplayName().isBlank()) {
                return 5; // Supplement name is required
            }
            userItem.setDisplayName(userItem.getDisplayName().trim());
            return 0; // Supplement details are valid
        }
        return 8; // Type must be DRUG or SUPPLEMENT
    }
}
