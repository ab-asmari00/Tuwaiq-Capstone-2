package com.example.jura;

import com.example.jura.Api.SupplementRequest;
import com.example.jura.Controller.UserItemController;
import com.example.jura.Model.DrugCache;
import com.example.jura.Model.ItemIngredient;
import com.example.jura.Model.User;
import com.example.jura.Model.UserItem;
import com.example.jura.Repository.DrugCacheRepository;
import com.example.jura.Repository.AiInteractionResultRepository;
import com.example.jura.Repository.ItemIngredientRepository;
import com.example.jura.Repository.UserItemRepository;
import com.example.jura.Repository.UserRepository;
import com.example.jura.Service.ItemIngredientService;
import com.example.jura.Service.UserItemService;
import com.example.jura.Service.DoseScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemIngredientFlowTests {

    private final ItemIngredientRepository ingredients = mock(ItemIngredientRepository.class);
    private final UserItemRepository items = mock(UserItemRepository.class);
    private final DrugCacheRepository drugs = mock(DrugCacheRepository.class);
    private final ItemIngredientService ingredientService = new ItemIngredientService(ingredients, items, drugs);

    @Test
    void supplementCreationSavesMultipleIngredientsAndRejectsDuplicatesBeforeSaving() {
        UserRepository users = mock(UserRepository.class);
        User patient = new User();
        patient.setRole("PATIENT");
        when(users.findUserById(1)).thenReturn(patient);
        when(items.save(any())).thenAnswer(call -> {
            UserItem item = call.getArgument(0);
            item.setId(10);
            return item;
        });
        UserItemService service = new UserItemService(items, users, drugs, ingredientService, mock(AiInteractionResultRepository.class), mock(DoseScheduleService.class));
        SupplementRequest request = new SupplementRequest(1, "Calcium + Vitamin D", null, List.of(
                new SupplementRequest.IngredientInput("Calcium", "كالسيوم"),
                new SupplementRequest.IngredientInput(" calcium ", null)));

        assertEquals(12, service.addSupplement(request));
        verify(items, never()).save(any());
        verify(ingredients, never()).saveAll(any());

        request.setIngredients(List.of(new SupplementRequest.IngredientInput("Calcium", "كالسيوم"),
                new SupplementRequest.IngredientInput("Vitamin D", "فيتامين د")));
        assertEquals(0, service.addSupplement(request));
        verify(ingredients).saveAll(argThat(rows -> {
            int count = 0;
            for (ItemIngredient row : rows) {
                if (!Integer.valueOf(10).equals(row.getItemId())) return false;
                count++;
            }
            return count == 2;
        }));
    }

    @Test
    void drugIngredientsComeFromSfdaAndCannotBeManuallyAdded() {
        UserItem item = new UserItem();
        item.setId(7);
        item.setType("DRUG");
        item.setDrugCacheId(8);
        when(items.findById(7)).thenReturn(Optional.of(item));
        DrugCache drug = new DrugCache();
        drug.setScientificNameRaw("PARACETAMOL, PSEUDOEPHEDRINE HYDROCHLORIDE,paracetamol");
        when(drugs.findById(8)).thenReturn(Optional.of(drug));

        assertEquals(0, ingredientService.syncDrugIngredients(7));
        verify(ingredients).deleteByItemId(7);
        verify(ingredients).saveAll(argThat(rows -> {
            int count = 0;
            for (ItemIngredient row : rows) {
                if (!Integer.valueOf(7).equals(row.getItemId())) return false;
                count++;
            }
            return count == 2;
        }));
        assertEquals(2, ingredientService.addItemIngredient(new ItemIngredient(null, 7, "Invented", null)));
        verify(ingredients, never()).save(any());
    }

    @Test
    void missingSfdaIngredientDataDoesNotInventIngredients() {
        UserItem item = new UserItem();
        item.setType("DRUG");
        item.setDrugCacheId(8);
        when(items.findById(7)).thenReturn(Optional.of(item));
        when(drugs.findById(8)).thenReturn(Optional.of(new DrugCache()));
        assertEquals(4, ingredientService.syncDrugIngredients(7));
        DrugCache malformed = new DrugCache();
        malformed.setScientificNameRaw("PARACETAMOL,");
        when(drugs.findById(8)).thenReturn(Optional.of(malformed));
        assertEquals(4, ingredientService.syncDrugIngredients(7));
        verify(ingredients, never()).saveAll(any());
    }

    @Test
    void supplementPayloadRequiresIngredientList() throws Exception {
        UserItemService service = mock(UserItemService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UserItemController(service)).build();
        mvc.perform(post("/api/v1/user-item/add-supplement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"displayName\":\"Calcium Plus\",\"ingredients\":[]}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).addSupplement(any());
        mvc.perform(post("/api/v1/user-item/add-supplement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"displayName\":\"Calcium Plus\",\"ingredients\":[{\"nameEn\":\"Calcium\"},{\"nameEn\":\"Vitamin D\"}]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deletingUserItemRemovesItsIngredients() {
        UserItem item = new UserItem();
        when(items.findById(7)).thenReturn(Optional.of(item));
        UserItemService service = new UserItemService(items, mock(UserRepository.class), drugs, ingredientService, mock(AiInteractionResultRepository.class), mock(DoseScheduleService.class));
        service.deleteUserItem(7);
        var order = inOrder(ingredients, items);
        order.verify(ingredients).deleteByItemId(7);
        order.verify(items).delete(item);
    }

    @Test
    void ingredientCannotMoveToAnotherItemAndLastSupplementIngredientCannotBeDeleted() {
        UserItem item = new UserItem();
        item.setId(7);
        item.setType("SUPPLEMENT");
        ItemIngredient saved = new ItemIngredient(4, 7, "Calcium", null);
        when(ingredients.findById(4)).thenReturn(Optional.of(saved));
        when(items.findById(7)).thenReturn(Optional.of(item));
        assertEquals(2, ingredientService.updateItemIngredient(4, new ItemIngredient(null, 8, "Vitamin D", null)));
        when(ingredients.countByItemId(7)).thenReturn(1L);
        assertEquals(3, ingredientService.deleteItemIngredient(4));
        verify(ingredients, never()).delete(any());

        assertEquals(0, ingredientService.updateItemIngredient(4, new ItemIngredient(null, 7, " Calcium carbonate ", "كالسيوم")));
        assertEquals("Calcium carbonate", saved.getNameEn());
        verify(ingredients).save(saved);
        when(ingredients.countByItemId(7)).thenReturn(2L);
        assertEquals(0, ingredientService.deleteItemIngredient(4));
        verify(ingredients).delete(saved);
    }
}
