package com.example.jura.Service;

import com.example.jura.Model.DrugCache;
import com.example.jura.Repository.DrugCacheRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SfdaSyncService {

    private static final String SFDA_URL = "https://oldsfda.sfda.gov.sa/GetDrugs.php";
    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");

    private final RestClient restClient;
    private final DrugCacheRepository drugCacheRepository;

    public SfdaSyncService(RestClient.Builder restClientBuilder, DrugCacheRepository drugCacheRepository) {
        this.restClient = restClientBuilder.build();
        this.drugCacheRepository = drugCacheRepository;
    }

    public synchronized SyncResult syncAllDrugs() {
        SfdaPage firstPage = fetchPage(1);
        int pageCount = firstPage.pageCount();
        if (pageCount < 1 || pageCount > 1000) {
            throw new IllegalStateException("SFDA returned an invalid page count");
        }

        int added = 0;
        int updated = 0;
        int skipped = 0;
        for (int page = 1; page <= pageCount; page++) {
            SfdaPage sfdaPage = page == 1 ? firstPage : fetchPage(page);
            if (sfdaPage.results().isEmpty()) {
                throw new IllegalStateException("SFDA returned an empty page before the end of the catalog");
            }

            Map<String, DrugCache> drugsToSave = new LinkedHashMap<>();
            for (Map<?, ?> row : sfdaPage.results()) {
                String registrationNumber = cleanString(row.get("registerNumber"));
                if (registrationNumber == null || registrationNumber.length() > 100) {
                    skipped++;
                    continue;
                }

                DrugCache drug = drugsToSave.get(registrationNumber);
                if (drug == null) {
                    drug = drugCacheRepository.findDrugCacheBySfdaRegNo(registrationNumber);
                    if (drug == null) {
                        drug = new DrugCache();
                        drug.setSfdaRegNo(registrationNumber);
                        added++;
                    } else {
                        updated++;
                    }
                    drugsToSave.put(registrationNumber, drug);
                }

                drug.setTradeNameAr(limit(cleanString(row.get("tradeNameAr")), 255));
                drug.setTradeNameEn(limit(cleanString(row.get("tradeName")), 255));
                drug.setSearchNameAr(DrugSearchService.normalizeArabic(drug.getTradeNameAr()));
                drug.setScientificNameRaw(cleanString(row.get("scientificName")));
                drug.setSyncedAt(LocalDateTime.now(RIYADH));
            }

            drugCacheRepository.saveAll(drugsToSave.values());
            skipped += sfdaPage.skippedRows();
            if (page % 25 == 0 || page == pageCount) {
                log.info("SFDA drug sync: page {} of {}", page, pageCount);
            }
        }

        return new SyncResult(pageCount, added, updated, skipped);
    }

    private SfdaPage fetchPage(int page) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("TradeName", "");
        form.add("scientificName", "");
        form.add("Agent", "");
        form.add("ManufacturerName", "");
        form.add("RegNo", "");
        form.add("page", String.valueOf(page));

        Map<?, ?> response = restClient.post()
                .uri(SFDA_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("data") instanceof Map<?, ?> data)
                || Boolean.FALSE.equals(data.get("success"))
                || !(data.get("result") instanceof Map<?, ?> result)
                || !(result.get("pageCount") instanceof Number pageCount)
                || !(result.get("results") instanceof List<?> rawRows)) {
            throw new IllegalStateException("SFDA response did not contain the expected drug list");
        }

        List<Map<?, ?>> rows = new ArrayList<>();
        int skippedRows = 0;
        for (Object rawRow : rawRows) {
            if (rawRow instanceof Map<?, ?> row) {
                rows.add(row);
            } else {
                skippedRows++;
            }
        }
        return new SfdaPage(pageCount.intValue(), rows, skippedRows);
    }

    private static String cleanString(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        String cleaned = text.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String limit(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }

    private record SfdaPage(int pageCount, List<Map<?, ?>> results, int skippedRows) {
    }

    public record SyncResult(int pages, int added, int updated, int skipped) {
    }
}
