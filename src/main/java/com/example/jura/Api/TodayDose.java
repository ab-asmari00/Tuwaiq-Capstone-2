package com.example.jura.Api;

import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodayDose {
    private Integer scheduleId;
    private Integer itemId;
    private String displayName;
    private String dosageText;
    private LocalDateTime dueAt;
    private String status;
    private Integer logId;
    private LocalDateTime takenAt;
    private boolean reminderDue;
}
