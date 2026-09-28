package com.example.jura.Api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DrugSearchResponse {

    private int page;
    private int totalPages;
    private long totalResults;
    private List<DrugSearchItem> drugs;
}
