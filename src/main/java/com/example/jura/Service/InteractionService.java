package com.example.jura.Service;

import com.example.jura.Api.GeminiInteractionResponse;
import com.example.jura.Api.InteractionCheckResponse;
import com.example.jura.Model.*;
import com.example.jura.Repository.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final AiInteractionResultRepository resultRepository;
    private final UserItemRepository itemRepository;
    private final ItemIngredientRepository ingredientRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    // No database transaction is kept open during the external AI request.
    public CheckOutcome checkInteractions(Integer itemId) {
        UserItem selected = itemRepository.findById(itemId).orElse(null);
        if (selected == null) {
            return new CheckOutcome(1, null); // Item ID not found
        }
        if (!Boolean.TRUE.equals(selected.getActive())) {
            return new CheckOutcome(2, null); // Selected item must be active
        }
        User patient = userRepository.findUserById(selected.getUserId());
        if (patient == null || !"PATIENT".equals(patient.getRole())) {
            return new CheckOutcome(3, null); // Item must belong to an existing patient
        }
        List<UserItem> otherItems = itemRepository.findByUserIdAndActiveTrue(selected.getUserId()).stream()
                .filter(item -> !item.getId().equals(itemId)).toList();
        if (otherItems.isEmpty()) {
            return new CheckOutcome(0, new InteractionCheckResponse(
                    "No other active items to compare. No interaction assessment was performed.", List.of())); // Nothing to compare
        }

        List<ItemIngredient> selectedIngredients = ingredientRepository.findByItemId(itemId);
        Map<Integer, List<ItemIngredient>> ingredients = new HashMap<>();
        List<UserItem> checkable = new ArrayList<>();
        for (UserItem other : otherItems) {
            List<ItemIngredient> rows = ingredientRepository.findByItemId(other.getId());
            ingredients.put(other.getId(), rows);
            if (hasIngredients(selectedIngredients) && hasIngredients(rows)) {
                checkable.add(other);
            }
        }

        Map<Integer, GeminiInteractionResponse.PairResult> findings = new HashMap<>();
        if (!checkable.isEmpty()) {
            if (!geminiService.isConfigured()) {
                return new CheckOutcome(4, null); // Gemini is not configured
            }
            Map<String, Object> input = Map.of(
                    "selectedItem", itemInput(selected, selectedIngredients),
                    "otherItems", checkable.stream().map(item -> itemInput(item, ingredients.get(item.getId()))).toList(),
                    "medicalConditions", patient.getMedicalConditions() == null ? "" : patient.getMedicalConditions());
            try {
                List<GeminiInteractionResponse.PairResult> replies = geminiService.checkInteractions(input);
                Set<Integer> expectedIds = new HashSet<>();
                for (UserItem item : checkable) expectedIds.add(item.getId());
                for (GeminiInteractionResponse.PairResult reply : replies) {
                    if (!expectedIds.contains(reply.getOtherItemId())
                            || findings.putIfAbsent(reply.getOtherItemId(), reply) != null) {
                        findings.clear(); // Unexpected or duplicate IDs invalidate this response
                        break;
                    }
                }
            } catch (RestClientResponseException exception) {
                log.warn("Gemini interaction request returned HTTP {}: {}",
                        exception.getStatusCode().value(), geminiService.describeError(exception));
                return new CheckOutcome(5, null); // Provider rejected the request; no results saved
            } catch (RestClientException exception) {
                log.warn("Gemini interaction request failed (connection or timeout)");
                return new CheckOutcome(5, null); // Provider unavailable; no results saved
            }
        }

        List<AiInteractionResult> results = new ArrayList<>();
        LocalDateTime checkedAt = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));
        for (UserItem other : otherItems) {
            AiInteractionResult result = new AiInteractionResult();
            result.setItemAId(itemId);
            result.setItemBId(other.getId());
            result.setCheckedAt(checkedAt);
            result.setSourceLinksJson("[]"); // No verified external references are retrieved by this prototype
            boolean complete = hasIngredients(selectedIngredients) && hasIngredients(ingredients.get(other.getId()));
            GeminiInteractionResponse.PairResult finding = findings.get(other.getId());
            result.setModelName(complete ? geminiService.getModelName() : "INPUT_VALIDATION");
            if (finding == null) {
                result.setResultStatus("UNKNOWN");
                result.setExplanationAr(complete
                        ? "لم نحصل على تقييم صالح من نموذج الذكاء الاصطناعي لهذا الزوج."
                        : "بيانات المكونات مفقودة أو غير مكتملة لأحد العنصرين؛ لا يمكن تقييم التداخل.");
                result.setAdviceAr("لا تعتبر هذه النتيجة تأكيداً للسلامة. تحقق من المكونات واستشر الصيدلي عند الحاجة.");
            } else {
                result.setResultStatus(finding.getResultStatus());
                result.setExplanationAr(finding.getExplanationAr());
                result.setAdviceAr(finding.getAdviceAr());
            }
            results.add(result);
        }
        return new CheckOutcome(0, new InteractionCheckResponse(
                "Prototype AI assessment. NONE_IDENTIFIED does not guarantee safety. Results reflect the data at check time.",
                resultRepository.saveAll(results))); // Results checked and saved
    }

    private boolean hasIngredients(List<ItemIngredient> ingredients) {
        return ingredients != null && !ingredients.isEmpty() && ingredients.stream()
                .allMatch(ingredient -> ingredient.getNameEn() != null && !ingredient.getNameEn().isBlank());
    }

    private Map<String, Object> itemInput(UserItem item, List<ItemIngredient> ingredients) {
        return Map.of("otherItemId", item.getId(), "type", item.getType(),
                "ingredients", ingredients.stream().map(ItemIngredient::getNameEn).toList(),
                "dosageText", item.getDosageText() == null ? "" : item.getDosageText());
    }

    public int addResult(AiInteractionResult result) {
        int code = validatePair(result);
        if (code != 0) return code; // Invalid pair
        result.setId(null);
        prepareManualResult(result);
        resultRepository.save(result);
        return 0; // Manual course result added
    }

    public int updateResult(Integer id, AiInteractionResult result) {
        AiInteractionResult old = resultRepository.findById(id).orElse(null);
        if (old == null) return 1; // Result ID not found
        if (!Objects.equals(old.getItemAId(), result.getItemAId()) || !Objects.equals(old.getItemBId(), result.getItemBId()))
            return 4; // Pair IDs cannot be changed
        int code = validatePair(result);
        if (code != 0) return code; // Invalid pair
        old.setResultStatus(result.getResultStatus());
        old.setExplanationAr(result.getExplanationAr());
        old.setAdviceAr(result.getAdviceAr());
        prepareManualResult(old);
        resultRepository.save(old);
        return 0; // Result manually updated and labelled MANUAL
    }

    private int validatePair(AiInteractionResult result) {
        UserItem first = itemRepository.findById(result.getItemAId()).orElse(null);
        UserItem second = itemRepository.findById(result.getItemBId()).orElse(null);
        if (first == null || second == null) return 2; // Item ID not found
        if (first.getId().equals(second.getId()) || !Objects.equals(first.getUserId(), second.getUserId()))
            return 3; // Two different items belonging to the same patient are required
        User patient = userRepository.findUserById(first.getUserId());
        if (patient == null || !"PATIENT".equals(patient.getRole())) return 3; // Existing patient required
        return 0; // Pair is valid
    }

    private void prepareManualResult(AiInteractionResult result) {
        result.setModelName("MANUAL");
        result.setSourceLinksJson("[]");
        result.setCheckedAt(LocalDateTime.now(ZoneId.of("Asia/Riyadh")));
    }

    public List<AiInteractionResult> getAllResults() {
        return resultRepository.findAll();
    }

    public AiInteractionResult getResultById(Integer id) {
        return resultRepository.findById(id).orElse(null);
    }

    public List<AiInteractionResult> getResultsByItem(Integer itemId) {
        if (!itemRepository.existsById(itemId)) {
            return null;
        }
        return resultRepository.findByItemAIdOrItemBIdOrderByCheckedAtDesc(itemId, itemId);
    }

    public boolean deleteResult(Integer id) {
        AiInteractionResult result = resultRepository.findById(id).orElse(null);
        if (result == null) return false;
        resultRepository.delete(result);
        return true;
    }

    public record CheckOutcome(int code, InteractionCheckResponse response) { }
}
