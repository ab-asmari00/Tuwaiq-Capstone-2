package com.example.jura.Service;

import com.example.jura.Api.DrugSearchItem;
import com.example.jura.Api.DrugSearchResponse;
import com.example.jura.Model.DrugCache;
import com.example.jura.Repository.DrugCacheRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrugSearchService {

    private final DrugCacheRepository drugCacheRepository;

    public boolean isCatalogEmpty() {
        return drugCacheRepository.count() == 0;
    }

    public DrugSearchResponse search(String query, int page) {
        String name = query.trim();
        PageRequest pageable = PageRequest.of(page - 1, 20);
        Page<DrugCache> matches;

        if (name.codePoints().anyMatch(character -> Character.UnicodeScript.of(character) == Character.UnicodeScript.ARABIC)) {
            matches = drugCacheRepository.findBySearchNameArContaining(normalizeArabic(name), pageable);
        } else {
            matches = drugCacheRepository.findByTradeNameEnContainingIgnoreCase(name, pageable);
        }

        List<DrugSearchItem> drugs = matches.getContent().stream()
                .map(DrugSearchItem::from)
                .toList();
        return new DrugSearchResponse(page, matches.getTotalPages(), matches.getTotalElements(), drugs);
    }

    public static String normalizeArabic(String text) {
        if (text == null) {
            return null;
        }
        return text.trim()
                .replaceAll("[\\u064B-\\u065F\\u0670\\u0640]", "")
                .replaceAll("[\\u0622\\u0623\\u0625\\u0671]", "ا")
                .replace('ى', 'ي')
                .replace('ؤ', 'و')
                .replace('ئ', 'ي')
                .replace('ة', 'ه')
                .replaceAll("\\s+", " ");
    }
}
