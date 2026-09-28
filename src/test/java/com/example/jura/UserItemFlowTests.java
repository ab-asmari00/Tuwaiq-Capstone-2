package com.example.jura;

import com.example.jura.Controller.UserItemController;
import com.example.jura.Api.SupplementRequest;
import com.example.jura.Model.DrugCache;
import com.example.jura.Model.User;
import com.example.jura.Model.UserItem;
import com.example.jura.Repository.DrugCacheRepository;
import com.example.jura.Repository.AiInteractionResultRepository;
import com.example.jura.Repository.UserItemRepository;
import com.example.jura.Repository.UserRepository;
import com.example.jura.Service.UserItemService;
import com.example.jura.Service.DoseScheduleService;
import com.example.jura.Service.ItemIngredientService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserItemFlowTests {

    private final UserItemRepository itemRepository = mock(UserItemRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DrugCacheRepository drugRepository = mock(DrugCacheRepository.class);
    private final ItemIngredientService ingredientService = mock(ItemIngredientService.class);
    private final UserItemService service = new UserItemService(itemRepository, userRepository, drugRepository, ingredientService, mock(AiInteractionResultRepository.class), mock(DoseScheduleService.class));

    @Test
    void drugRequestCanOmitGeneratedNameAndActiveFlag() throws Exception {
        UserItemService mockedService = mock(UserItemService.class);
        when(mockedService.addUserItem(any())).thenReturn(0);
        MockMvcBuilders.standaloneSetup(new UserItemController(mockedService)).build()
                .perform(post("/api/v1/user-item/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"type\":\"DRUG\",\"drugCacheId\":8}"))
                .andExpect(status().isCreated());
        verify(mockedService).addUserItem(any());
    }

    @Test
    void drugNameComesFromCacheAndNewItemIsActive() {
        User patient = new User();
        patient.setRole("PATIENT");
        when(userRepository.findUserById(1)).thenReturn(patient);
        DrugCache drug = new DrugCache();
        drug.setTradeNameAr("بانادول ذائب");
        drug.setTradeNameEn("PANADOL SOLUBLE");
        when(drugRepository.findById(8)).thenReturn(Optional.of(drug));
        UserItem item = new UserItem();
        item.setId(99);
        item.setUserId(1);
        item.setType("DRUG");
        item.setDrugCacheId(8);
        item.setDisplayName("Client supplied name");
        item.setActive(false);
        when(itemRepository.save(item)).thenAnswer(call -> {
            assertNull(item.getId());
            item.setId(5);
            return item;
        });

        assertEquals(0, service.addUserItem(item));
        assertEquals("بانادول ذائب", item.getDisplayName());
        assertTrue(item.getActive());
        assertEquals(5, item.getId());
        verify(itemRepository).save(item);
        verify(ingredientService).syncDrugIngredients(5);
    }

    @Test
    void supplementRequiresNameAndCannotReferenceDrugCache() {
        User patient = new User();
        patient.setRole("PATIENT");
        when(userRepository.findUserById(1)).thenReturn(patient);
        UserItem item = new UserItem();
        item.setUserId(1);
        item.setType("SUPPLEMENT");
        assertEquals(9, service.addUserItem(item));

        item.setDisplayName(" Creatine ");
        item.setDrugCacheId(8);
        assertEquals(9, service.addUserItem(item));
        verify(itemRepository, never()).save(any());

        SupplementRequest request = new SupplementRequest(1, " Creatine ", null,
                List.of(new SupplementRequest.IngredientInput("Creatine", null)));
        assertEquals(0, service.addSupplement(request));
        verify(itemRepository).save(argThat(saved -> "Creatine".equals(saved.getDisplayName()) && saved.getActive()));
        verify(ingredientService).saveSupplementIngredients(any(), eq(request.getIngredients()));
    }

    @Test
    void doctorCannotAddPatientItemsAndMissingDrugIsRejected() {
        User doctor = new User();
        doctor.setRole("DOCTOR");
        when(userRepository.findUserById(2)).thenReturn(doctor);
        UserItem item = new UserItem();
        item.setUserId(2);
        item.setType("DRUG");
        assertEquals(2, service.addUserItem(item));

        User patient = new User();
        patient.setRole("PATIENT");
        when(userRepository.findUserById(1)).thenReturn(patient);
        item.setUserId(1);
        assertEquals(3, service.addUserItem(item));
        item.setDrugCacheId(404);
        when(drugRepository.findById(404)).thenReturn(Optional.empty());
        assertEquals(4, service.addUserItem(item));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateCannotReassignOwnerAndCanDeactivateItem() {
        UserItem saved = new UserItem();
        saved.setUserId(1);
        saved.setType("SUPPLEMENT");
        saved.setDisplayName("Creatine");
        saved.setActive(true);
        when(itemRepository.findById(7)).thenReturn(Optional.of(saved));
        UserItem update = new UserItem();
        update.setUserId(2);
        update.setType("SUPPLEMENT");
        update.setDisplayName("Creatine");
        update.setActive(false);
        assertEquals(2, service.updateUserItem(7, update));
        assertTrue(saved.getActive());

        update.setUserId(1);
        update.setDosageText("5 g daily");
        assertEquals(0, service.updateUserItem(7, update));
        assertFalse(saved.getActive());
        assertEquals("5 g daily", saved.getDosageText());

        User patient = new User();
        patient.setRole("PATIENT");
        when(userRepository.findUserById(1)).thenReturn(patient);
        when(itemRepository.findByUserIdAndActiveTrue(1)).thenReturn(List.of());
        assertTrue(service.getActiveUserItems(1).isEmpty());
        verify(itemRepository).findByUserIdAndActiveTrue(1);
    }

    @Test
    void changingSelectedDrugRefreshesIngredientsButChangingTypeIsRejected() {
        UserItem saved = new UserItem(7, 1, "DRUG", "Old drug", 8, null, true);
        when(itemRepository.findById(7)).thenReturn(Optional.of(saved));
        UserItem update = new UserItem(null, 1, "SUPPLEMENT", "Calcium", null, null, true);
        assertEquals(10, service.updateUserItem(7, update));
        DrugCache drug = new DrugCache();
        drug.setTradeNameEn("New drug");
        when(drugRepository.findById(9)).thenReturn(Optional.of(drug));
        update.setType("DRUG");
        update.setDrugCacheId(9);
        assertEquals(0, service.updateUserItem(7, update));
        verify(ingredientService).syncDrugIngredients(7);
    }
}
