package com.example.jura.Service;

import com.example.jura.Api.ApiException;
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
        return userItemRepository.findById(id).orElseThrow(() -> new ApiException("User item ID not found"));
    }

    public List<UserItem> getActiveUserItems(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException("Patient not found"));
        if (!"PATIENT".equals(user.getRole())) {
            throw new ApiException("Patient not found");
        }
        return userItemRepository.findByUserIdAndActiveTrue(userId);
    }

    @Transactional
    public void addUserItem(UserItem userItem) {
        if ("SUPPLEMENT".equals(userItem.getType())) {
            throw new ApiException("Use add-supplement to include the ingredient list");
        }
        saveNewItem(userItem);
    }

    @Transactional
    public void addSupplement(SupplementRequest request) {
        itemIngredientService.validateSupplementIngredients(request.getIngredients());
        UserItem item = new UserItem(null, request.getUserId(), "SUPPLEMENT", request.getDisplayName(),
                null, request.getDosageText(), true);
        saveNewItem(item);
        itemIngredientService.saveSupplementIngredients(item.getId(), request.getIngredients());
    }

    private void saveNewItem(UserItem userItem) {
        User user = userRepository.findById(userItem.getUserId()).orElseThrow(() -> new ApiException("User ID not found"));
        if (!"PATIENT".equals(user.getRole())) {
            throw new ApiException("User is not a patient");
        }

        prepareItem(userItem);

        userItem.setId(null);
        userItem.setActive(true);
        userItemRepository.save(userItem);
        if ("DRUG".equals(userItem.getType())) {
            itemIngredientService.syncDrugIngredientsIfAvailable(userItem.getId());
        }
    }

    @Transactional
    public void updateUserItem(Integer id, UserItem userItem) {
        UserItem oldItem = userItemRepository.findById(id).orElseThrow(() -> new ApiException("User item ID not found"));
        if (!oldItem.getUserId().equals(userItem.getUserId())) {
            throw new ApiException("User ID cannot be changed");
        }
        if (!oldItem.getType().equals(userItem.getType())) {
            throw new ApiException("Item type cannot be changed");
        }

        prepareItem(userItem);

        boolean drugChanged = !Objects.equals(oldItem.getDrugCacheId(), userItem.getDrugCacheId());
        oldItem.setDisplayName(userItem.getDisplayName());
        oldItem.setDrugCacheId(userItem.getDrugCacheId());
        oldItem.setDosageText(userItem.getDosageText());
        if (userItem.getActive() != null) {
            oldItem.setActive(userItem.getActive());
        }
        userItemRepository.save(oldItem);
        if ("DRUG".equals(oldItem.getType()) && drugChanged) {
            itemIngredientService.syncDrugIngredientsIfAvailable(oldItem.getId());
        }
    }

    @Transactional
    public void deleteUserItem(Integer id) {
        UserItem userItem = userItemRepository.findById(id).orElseThrow(() -> new ApiException("User item ID not found"));
        doseScheduleService.deleteDoseSchedulesByItem(id);
        interactionResultRepository.deleteByItemAIdOrItemBId(id, id);
        itemIngredientService.deleteIngredientsByItemId(id);
        userItemRepository.delete(userItem);
    }

    private void prepareItem(UserItem userItem) {
        if ("DRUG".equals(userItem.getType())) {
            if (userItem.getDrugCacheId() == null) {
                throw new ApiException("Drug cache ID is required for a drug");
            }
            DrugCache drug = drugCacheRepository.findById(userItem.getDrugCacheId()).orElseThrow(() -> new ApiException("Drug cache ID not found"));

            String name = drug.getTradeNameAr();
            if (name == null || name.isBlank()) {
                name = drug.getTradeNameEn();
            }
            if (name == null || name.isBlank()) {
                throw new ApiException("Cached drug has no trade name");
            }
            userItem.setDisplayName(name.trim());
            return;
        }

        if ("SUPPLEMENT".equals(userItem.getType())) {
            if (userItem.getDrugCacheId() != null) {
                throw new ApiException("A supplement cannot have a drug cache ID");
            }
            if (userItem.getDisplayName() == null || userItem.getDisplayName().isBlank()) {
                throw new ApiException("Supplement name is required");
            }
            userItem.setDisplayName(userItem.getDisplayName().trim());
            return;
        }
        throw new ApiException("Type must be DRUG or SUPPLEMENT");
    }
}
