package com.example.jura.Api;

import com.example.jura.Model.DrugCache;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DrugSearchItem {

    private Integer id;
    private String sfdaRegNo;
    private String tradeNameAr;
    private String tradeNameEn;
    private String scientificName;

    public static DrugSearchItem from(DrugCache drug) {
        return new DrugSearchItem(drug.getId(), drug.getSfdaRegNo(),
                drug.getTradeNameAr(), drug.getTradeNameEn(), drug.getScientificNameRaw());
    }
}
