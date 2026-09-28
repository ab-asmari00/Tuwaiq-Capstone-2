package com.example.jura;

import com.example.jura.Api.GeminiInteractionResponse;
import com.example.jura.Controller.InteractionController;
import com.example.jura.Model.*;
import com.example.jura.Repository.*;
import com.example.jura.Service.GeminiService;
import com.example.jura.Service.InteractionService;
import com.example.jura.Service.UserItemService;
import com.example.jura.Service.DoseScheduleService;
import com.example.jura.Service.ItemIngredientService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InteractionFlowTests {
    private final AiInteractionResultRepository results = mock(AiInteractionResultRepository.class);
    private final UserItemRepository items = mock(UserItemRepository.class);
    private final ItemIngredientRepository ingredients = mock(ItemIngredientRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final GeminiService gemini = mock(GeminiService.class);
    private final InteractionService service = new InteractionService(results, items, ingredients, users, gemini);

    private void prepare() {
        UserItem selected = new UserItem(10, 1, "SUPPLEMENT", "Iron", null, null, true);
        UserItem other = new UserItem(11, 1, "SUPPLEMENT", "Calcium", null, null, true);
        User patient = new User(1, "Private name", "private@example.com", "private-password", "PATIENT", "Reported condition");
        when(items.findById(10)).thenReturn(Optional.of(selected));
        when(items.findByUserIdAndActiveTrue(1)).thenReturn(List.of(selected, other));
        when(users.findUserById(1)).thenReturn(patient);
        when(ingredients.findByItemId(10)).thenReturn(List.of(new ItemIngredient(1, 10, "Iron", null)));
        when(ingredients.findByItemId(11)).thenReturn(List.of(new ItemIngredient(2, 11, "Calcium", null),
                new ItemIngredient(3, 11, "Vitamin D", null)));
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.getModelName()).thenReturn("gemini-3.8-flash");
        when(results.saveAll(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void oneRequestContainsAllIngredientsAndConditionsButNoPatientCredentials() {
        prepare();
        when(gemini.checkInteractions(any())).thenAnswer(call -> {
            Map<String, Object> input = call.getArgument(0);
            String json = new JsonMapper().writeValueAsString(input);
            assertTrue(json.contains("Iron"));
            assertTrue(json.contains("Calcium"));
            assertTrue(json.contains("Vitamin D"));
            assertTrue(json.contains("Reported condition"));
            assertFalse(json.contains("private@example.com"));
            assertFalse(json.contains("private-password"));
            assertFalse(json.contains("Private name"));
            return List.of(new GeminiInteractionResponse.PairResult(11, "MODERATE", "تقييم تجريبي", "راجع الصيدلي"));
        });
        var outcome = service.checkInteractions(10);
        assertEquals(0, outcome.code());
        assertEquals(1, outcome.response().getResults().size());
        AiInteractionResult saved = outcome.response().getResults().get(0);
        assertEquals("MODERATE", saved.getResultStatus());
        assertEquals(10, saved.getItemAId());
        assertEquals(11, saved.getItemBId());
        assertEquals("[]", saved.getSourceLinksJson());
        assertNotNull(saved.getCheckedAt());
        verify(gemini, times(1)).checkInteractions(any());
    }

    @Test
    void missingIngredientsCreateUnknownWithoutCallingAiOrRequiringKey() {
        prepare();
        when(ingredients.findByItemId(10)).thenReturn(List.of());
        when(gemini.isConfigured()).thenReturn(false);
        var outcome = service.checkInteractions(10);
        assertEquals(0, outcome.code());
        assertEquals("UNKNOWN", outcome.response().getResults().get(0).getResultStatus());
        assertEquals("INPUT_VALIDATION", outcome.response().getResults().get(0).getModelName());
        verify(gemini, never()).checkInteractions(any());
    }

    @Test
    void unexpectedDuplicateAndOmittedPairIdsCannotProduceSafetyFindings() {
        prepare();
        var finding = new GeminiInteractionResponse.PairResult(11, "NONE_IDENTIFIED", "تقييم", "إرشاد");
        when(gemini.checkInteractions(any())).thenReturn(List.of(finding, finding));
        assertEquals("UNKNOWN", service.checkInteractions(10).response().getResults().get(0).getResultStatus());
        when(gemini.checkInteractions(any())).thenReturn(List.of(new GeminiInteractionResponse.PairResult(99, "NONE_IDENTIFIED", "تقييم", "إرشاد")));
        assertEquals("UNKNOWN", service.checkInteractions(10).response().getResults().get(0).getResultStatus());
        when(gemini.checkInteractions(any())).thenReturn(List.of());
        assertEquals("UNKNOWN", service.checkInteractions(10).response().getResults().get(0).getResultStatus());
    }

    @Test
    void missingConfigurationAndApiFailureSaveNoResults() {
        prepare();
        when(gemini.isConfigured()).thenReturn(false);
        assertEquals(4, service.checkInteractions(10).code());
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.checkInteractions(any())).thenThrow(new RestClientException("timeout"));
        assertEquals(5, service.checkInteractions(10).code());
        verify(results, never()).saveAll(any());
    }

    @Test
    void missingInactiveAndNonPatientItemsAreRejectedAndAloneIsNotSafe() {
        assertEquals(1, service.checkInteractions(404).code());
        prepare();
        UserItem selected = items.findById(10).orElseThrow();
        selected.setActive(false);
        assertEquals(2, service.checkInteractions(10).code());
        selected.setActive(true);
        users.findUserById(1).setRole("DOCTOR");
        assertEquals(3, service.checkInteractions(10).code());
        users.findUserById(1).setRole("PATIENT");
        when(items.findByUserIdAndActiveTrue(1)).thenReturn(List.of(selected));
        var outcome = service.checkInteractions(10);
        assertEquals(0, outcome.code());
        assertTrue(outcome.response().getResults().isEmpty());
        verify(gemini, never()).checkInteractions(any());
        verify(results, never()).saveAll(any());
    }

    @Test
    void controllerMapsNotFoundConfigurationAndProviderErrors() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new InteractionController(service)).build();
        mvc.perform(post("/api/v1/interaction/check/404")).andExpect(status().isNotFound());
        prepare();
        when(gemini.isConfigured()).thenReturn(false);
        mvc.perform(post("/api/v1/interaction/check/10")).andExpect(status().isServiceUnavailable());
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.checkInteractions(any())).thenThrow(new RestClientException("timeout"));
        mvc.perform(post("/api/v1/interaction/check/10")).andExpect(status().isBadGateway());
    }

    private void assertGeminiOutput(String text, String finishReason, int expectedCount) {
        RestClient.Builder httpBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(httpBuilder).build();
        RestClient.Builder configuredBuilder = mock(RestClient.Builder.class);
        when(configuredBuilder.requestFactory(any())).thenReturn(httpBuilder);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            GeminiService provider = new GeminiService(configuredBuilder, factory.getValidator(), "test-key", "gemini-3.8-flash");
            String response = new JsonMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                    "finishReason", finishReason, "content", Map.of("parts", List.of(Map.of("text", text)))))));
            server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                    .andExpect(header("x-goog-api-key", "test-key"))
                    .andExpect(jsonPath("$.generationConfig.responseFormat.text.mimeType").value("APPLICATION_JSON"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
            assertEquals(expectedCount, provider.checkInteractions(Map.of("selectedItem", "test")).size());
            server.verify();
        }
    }

    @Test
    void realRestClientUsesStructuredJsonAndParsesValidatedOutput() {
        assertGeminiOutput("{\"results\":[{\"otherItemId\":11,\"resultStatus\":\"MODERATE\",\"explanationAr\":\"تفسير\",\"adviceAr\":\"نصيحة\"}]}", "STOP", 1);
    }

    @Test
    void blockedMalformedTruncatedAndUnsupportedStatusOutputsAreNotAccepted() {
        assertGeminiOutput("not JSON", "STOP", 0);
        assertGeminiOutput("{\"results\":[]}", "SAFETY", 0);
        assertGeminiOutput("{\"results\":[]}", "MAX_TOKENS", 0);
        assertGeminiOutput("{\"results\":[{\"otherItemId\":11,\"resultStatus\":\"SAFE\",\"explanationAr\":\"تفسير\",\"adviceAr\":\"نصيحة\"}]}", "STOP", 0);
        assertGeminiOutput("{\"results\":[{\"otherItemId\":11,\"resultStatus\":\"HIGH\",\"explanationAr\":\"\",\"adviceAr\":\"نصيحة\"}]}", "STOP", 0);
    }

    @Test
    void providerErrorMessageIsReadableWithoutExposingTheApiKeyOrOtherFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            GeminiService provider = new GeminiService(RestClient.builder(), factory.getValidator(), "secret-test-key", "gemini-3.8-flash");
            String body = new JsonMapper().writeValueAsString(Map.of("error", Map.of(
                    "message", "API key secret-test-key is invalid\nCheck configuration",
                    "details", List.of("must-not-be-logged"))));
            var exception = new RestClientResponseException("Bad request", 400, "Bad Request", null,
                    body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            String message = provider.describeError(exception);
            assertTrue(message.contains("[REDACTED]"));
            assertFalse(message.contains("secret-test-key"));
            assertFalse(message.contains("must-not-be-logged"));
            assertFalse(message.contains("\n"));
        }
    }

    @Test
    void nonJsonProviderErrorDoesNotDumpItsBody() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            GeminiService provider = new GeminiService(RestClient.builder(), factory.getValidator(), "test-key", "gemini-3.8-flash");
            var exception = new RestClientResponseException("Bad request", 400, "Bad Request", null,
                    "<html>private-content</html>".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            assertEquals("Provider returned no readable error message", provider.describeError(exception));
        }
    }

    @Test
    void itemDeletionRemovesBothDirectionsOfSavedInteractionResultsFirst() {
        UserItem item = new UserItem();
        when(items.findById(10)).thenReturn(Optional.of(item));
        ItemIngredientService ingredientService = mock(ItemIngredientService.class);
        UserItemService itemService = new UserItemService(items, users, mock(DrugCacheRepository.class), ingredientService, results, mock(DoseScheduleService.class));
        assertTrue(itemService.deleteUserItem(10));
        var order = inOrder(results, ingredientService, items);
        order.verify(results).deleteByItemAIdOrItemBId(10, 10);
        order.verify(ingredientService).deleteIngredientsByItemId(10);
        order.verify(items).delete(item);
    }
}
