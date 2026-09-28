package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.ItemIngredient;
import com.example.jura.Service.ItemIngredientService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/item-ingredient")
@RequiredArgsConstructor
public class ItemIngredientController {

    private final ItemIngredientService itemIngredientService;

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllItemIngredients() {
        List<ItemIngredient> ingredients = itemIngredientService.getAllItemIngredients();
        if (ingredients.isEmpty()) {
            return ResponseEntity.status(200).body(new ApiResponse("Ingredients list is empty"));
        }
        return ResponseEntity.status(200).body(ingredients);
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<?> getItemIngredientById(@PathVariable Integer id) {
        ItemIngredient ingredient = itemIngredientService.getItemIngredientById(id);
        if (ingredient == null) {
            return ResponseEntity.status(404).body(new ApiResponse("Ingredient ID not found"));
        }
        return ResponseEntity.status(200).body(ingredient);
    }

    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getIngredientsByItemId(@PathVariable Integer itemId) {
        List<ItemIngredient> ingredients = itemIngredientService.getIngredientsByItemId(itemId);
        if (ingredients == null) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        return ResponseEntity.status(200).body(ingredients);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItemIngredient(@RequestBody @Valid ItemIngredient ingredient, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        }
        int result = itemIngredientService.addItemIngredient(ingredient);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Drug ingredients must come from SFDA"));
        }
        if (result == 3) {
            return ResponseEntity.status(409).body(new ApiResponse("Ingredient already exists for this item"));
        }
        return ResponseEntity.status(201).body(new ApiResponse("Ingredient added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateItemIngredient(@PathVariable Integer id, @RequestBody @Valid ItemIngredient ingredient, Errors errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.status(400).body(new ApiResponse(errors.getAllErrors().get(0).getDefaultMessage()));
        }
        int result = itemIngredientService.updateItemIngredient(id, ingredient);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Ingredient ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("Item ID cannot be changed"));
        }
        if (result == 3) {
            return ResponseEntity.status(409).body(new ApiResponse("Drug ingredients must come from SFDA"));
        }
        if (result == 4) {
            return ResponseEntity.status(409).body(new ApiResponse("Ingredient already exists for this item"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Ingredient updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteItemIngredient(@PathVariable Integer id) {
        int result = itemIngredientService.deleteItemIngredient(id);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("Ingredient ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(409).body(new ApiResponse("Drug ingredients must come from SFDA"));
        }
        if (result == 3) {
            return ResponseEntity.status(409).body(new ApiResponse("A supplement must keep at least one ingredient"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Ingredient deleted successfully"));
    }

    @PostMapping("/sync-drug/{itemId}")
    public ResponseEntity<?> syncDrugIngredients(@PathVariable Integer itemId) {
        int result = itemIngredientService.syncDrugIngredients(itemId);
        if (result == 1) {
            return ResponseEntity.status(404).body(new ApiResponse("User item ID not found"));
        }
        if (result == 2) {
            return ResponseEntity.status(400).body(new ApiResponse("Item is not a drug"));
        }
        if (result == 3) {
            return ResponseEntity.status(404).body(new ApiResponse("Drug cache ID not found"));
        }
        if (result == 4) {
            return ResponseEntity.status(409).body(new ApiResponse("SFDA ingredient data is unavailable or cannot be stored reliably"));
        }
        return ResponseEntity.status(200).body(new ApiResponse("Drug ingredients synced successfully"));
    }
}
