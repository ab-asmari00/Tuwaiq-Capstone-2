package com.example.jura.Controller;

import com.example.jura.Api.ApiResponse;
import com.example.jura.Model.ItemIngredient;
import com.example.jura.Service.ItemIngredientService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.status(200).body(ingredient);
    }

    @GetMapping("/get-by-item/{itemId}")
    public ResponseEntity<?> getIngredientsByItemId(@PathVariable Integer itemId) {
        List<ItemIngredient> ingredients = itemIngredientService.getIngredientsByItemId(itemId);
        return ResponseEntity.status(200).body(ingredients);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItemIngredient(@RequestBody @Valid ItemIngredient ingredient) {
        itemIngredientService.addItemIngredient(ingredient);
        return ResponseEntity.status(201).body(new ApiResponse("Ingredient added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateItemIngredient(@PathVariable Integer id, @RequestBody @Valid ItemIngredient ingredient) {
        itemIngredientService.updateItemIngredient(id, ingredient);
        return ResponseEntity.status(200).body(new ApiResponse("Ingredient updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteItemIngredient(@PathVariable Integer id) {
        itemIngredientService.deleteItemIngredient(id);
        return ResponseEntity.status(200).body(new ApiResponse("Ingredient deleted successfully"));
    }

    @PostMapping("/sync-drug/{itemId}")
    public ResponseEntity<?> syncDrugIngredients(@PathVariable Integer itemId) {
        itemIngredientService.syncDrugIngredients(itemId);
        return ResponseEntity.status(200).body(new ApiResponse("Drug ingredients synced successfully"));
    }
}
