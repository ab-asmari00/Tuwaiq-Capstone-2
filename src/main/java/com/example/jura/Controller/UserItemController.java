package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Api.SupplementRequest;
import com.example.jura.Model.UserItem;
import com.example.jura.Service.UserItemService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/user-item")
@RequiredArgsConstructor
public class UserItemController {

    private final UserItemService userItemService;

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllUserItems() {
        List<UserItem> items = userItemService.getAllUserItems();
        if (items.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("User items list is empty"));
        }
        return ResponseEntity.status(200).body(items);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getUserItemById(@PathVariable Integer id) {
        UserItem item = userItemService.getUserItemById(id);
        if (item == null) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        return ResponseEntity.status(200).body(item);
    }

    @GetMapping("/get-active-by-user/{userId}")
    public ResponseEntity<?> getActiveUserItems(@PathVariable Integer userId) {
        List<UserItem> items = userItemService.getActiveUserItems(userId);
        if (items == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Patient not found"));
        }
        return ResponseEntity.status(200).body(items);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addUserItem(@RequestBody @Valid UserItem item, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getFieldError().getDefaultMessage()));
        }

        int result = userItemService.addUserItem(item);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("User is not a patient"));
        }
        if (result != 0) {
            return itemError(result);
        }
        return ResponseEntity.status(201).body(new ApiResponse("User item added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUserItem(@PathVariable Integer id, @RequestBody @Valid UserItem item, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getFieldError().getDefaultMessage()));
        }

        int result = userItemService.updateUserItem(id, item);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("User ID cannot be changed"));
        }
        if (result != 0) {
            return itemError(result);
        }
        return ResponseEntity.status(200).body(new ApiResponse("User item updated successfully"));
    }

    @PostMapping("/add-supplement")
    public ResponseEntity<?> addSupplement(@RequestBody @Valid SupplementRequest request, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        }
        int result = userItemService.addSupplement(request);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("User is not a patient"));
        }
        if (result != 0) {
            return itemError(result);
        }
        return ResponseEntity.status(201).body(new ApiResponse("Supplement and its ingredients added successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUserItem(@PathVariable Integer id) {
        if (!userItemService.deleteUserItem(id)) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("User item deleted successfully"));
    }

    private ResponseEntity<?> itemError(int result) {
        if (result == 3) {
            return ResponseEntity.status(400).body(new ApiResponse("Drug cache ID is required for a drug"));
        }
        if (result == 4) {
            return ResponseEntity.status(404).body(new ApiResponse("Drug cache ID not found"));
        }
        if (result == 5) {
            return ResponseEntity.status(400).body(new ApiResponse("Supplement name is required"));
        }
        if (result == 6) {
            return ResponseEntity.status(400).body(new ApiResponse("A supplement cannot have a drug cache ID"));
        }
        if (result == 7) {
            return ResponseEntity.status(409).body(new ApiResponse("Cached drug has no trade name"));
        }
        if (result == 9) {
            return ResponseEntity.status(400).body(new ApiResponse("Use add-supplement and provide ingredients"));
        }
        if (result == 10) {
            return ResponseEntity.status(400).body(new ApiResponse("Item type cannot be changed"));
        }
        if (result == 11) {
            return ResponseEntity.status(400).body(new ApiResponse("At least one valid ingredient is required"));
        }
        if (result == 12) {
            return ResponseEntity.status(409).body(new ApiResponse("Duplicate ingredient in the request"));
        }
        return ResponseEntity.status(400).body(new ApiResponse("Type must be DRUG or SUPPLEMENT"));
    }
}
