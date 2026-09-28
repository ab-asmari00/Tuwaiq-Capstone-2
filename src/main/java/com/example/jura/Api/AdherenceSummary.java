package com.example.jura.Api;
import lombok.*;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdherenceSummary {
    private long taken;
    private long skipped;
    private long missed;
    private long totalRecorded;
    private Double takenPercentage;
}
