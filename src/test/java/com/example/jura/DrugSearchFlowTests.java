package com.example.jura;

import com.example.jura.Api.DrugSearchResponse;
import com.example.jura.Model.DrugCache;
import com.example.jura.Repository.DrugCacheRepository;
import com.example.jura.Service.DrugSearchService;
import com.example.jura.Service.SfdaSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class DrugSearchFlowTests {

    @Test
    void syncsAllPagesAndUpdatesExistingRows() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DrugCacheRepository repository = mock(DrugCacheRepository.class);
        Map<String, DrugCache> saved = new HashMap<>();
        when(repository.findDrugCacheBySfdaRegNo(any())).thenAnswer(call -> saved.get(call.getArgument(0)));
        when(repository.saveAll(any())).thenAnswer(call -> {
            List<DrugCache> rows = new ArrayList<>();
            for (DrugCache drug : (Iterable<DrugCache>) call.getArgument(0)) {
                saved.put(drug.getSfdaRegNo(), drug);
                rows.add(drug);
            }
            return rows;
        });

        String first = "{\"data\":{\"success\":true,\"result\":{\"pageCount\":2,\"results\":[{\"registerNumber\":\"100\",\"tradeName\":\"PANADOL\",\"tradeNameAr\":\"بَانَادُول\",\"scientificName\":\"PARACETAMOL\"}]}}}";
        String second = "{\"data\":{\"success\":true,\"result\":{\"pageCount\":2,\"results\":[{\"registerNumber\":\"200\",\"tradeName\":\"OTHER DRUG\",\"tradeNameAr\":\"دواء آخر\",\"scientificName\":\"OTHER INGREDIENT\"}]}}}";
        for (int run = 0; run < 2; run++) {
            server.expect(once(), requestTo("https://oldsfda.sfda.gov.sa/GetDrugs.php"))
                    .andExpect(method(POST))
                    .andExpect(content().string(containsString("page=1")))
                    .andRespond(withSuccess(first, MediaType.APPLICATION_JSON));
            server.expect(once(), requestTo("https://oldsfda.sfda.gov.sa/GetDrugs.php"))
                    .andExpect(method(POST))
                    .andExpect(content().string(containsString("page=2")))
                    .andRespond(withSuccess(second, MediaType.APPLICATION_JSON));
        }

        SfdaSyncService service = new SfdaSyncService(builder, repository);
        SfdaSyncService.SyncResult firstSync = service.syncAllDrugs();
        SfdaSyncService.SyncResult secondSync = service.syncAllDrugs();

        assertEquals(2, firstSync.pages());
        assertEquals(2, firstSync.added());
        assertEquals(0, firstSync.updated());
        assertEquals(0, secondSync.added());
        assertEquals(2, secondSync.updated());
        assertEquals(2, saved.size());
        assertEquals("بانادول", saved.get("100").getSearchNameAr());
        assertEquals("PARACETAMOL", saved.get("100").getScientificNameRaw());
        server.verify();
    }

    @Test
    void arabicSearchUsesNormalizedNameAndReturnsOnlySelectedFields() {
        DrugCacheRepository repository = mock(DrugCacheRepository.class);
        DrugSearchService service = new DrugSearchService(repository);
        DrugCache drug = new DrugCache();
        drug.setId(5);
        drug.setSfdaRegNo("100");
        drug.setTradeNameAr("بانادول");
        drug.setTradeNameEn("PANADOL");
        drug.setScientificNameRaw("PARACETAMOL");
        when(repository.findBySearchNameArContaining(eq("بانادول"), any()))
                .thenReturn(new PageImpl<>(List.of(drug), PageRequest.of(0, 20), 1));

        DrugSearchResponse result = service.search("بَانَادُول", 1);

        assertEquals(1, result.getTotalResults());
        assertEquals("100", result.getDrugs().get(0).getSfdaRegNo());
        assertEquals("PARACETAMOL", result.getDrugs().get(0).getScientificName());
        verify(repository).findBySearchNameArContaining(eq("بانادول"), any());
    }
}
