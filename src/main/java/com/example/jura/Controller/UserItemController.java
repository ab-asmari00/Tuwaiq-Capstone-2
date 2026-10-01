package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Api.SupplementRequest;
import com.example.jura.Model.UserItem;
import com.example.jura.Service.UserItemService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.status(200).body(item);
    }

    @GetMapping("/get-active-by-user/{userId}")
    public ResponseEntity<?> getActiveUserItems(@PathVariable Integer userId) {
        List<UserItem> items = userItemService.getActiveUserItems(userId);
        if (items.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("User items list is empty"));
        }
        return ResponseEntity.status(200).body(items);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addUserItem(@RequestBody @Valid UserItem item) {
        userItemService.addUserItem(item);
        return ResponseEntity.status(201).body(new ApiResponse("User item added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUserItem(@PathVariable Integer id, @RequestBody @Valid UserItem item) {
        userItemService.updateUserItem(id, item);
        return ResponseEntity.status(200).body(new ApiResponse("User item updated successfully"));
    }

    @PostMapping("/add-supplement")
    public ResponseEntity<?> addSupplement(@RequestBody @Valid SupplementRequest request) {
        userItemService.addSupplement(request);
        return ResponseEntity.status(201).body(new ApiResponse("Supplement and its ingredients added successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUserItem(@PathVariable Integer id) {
        userItemService.deleteUserItem(id);
        return ResponseEntity.status(200).body(new ApiResponse("User item deleted successfully"));
    }

}
