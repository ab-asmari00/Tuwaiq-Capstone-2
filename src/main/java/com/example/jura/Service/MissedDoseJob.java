package com.example.jura.Service;

import com.example.jura.Model.User;
import com.example.jura.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissedDoseJob {
    private final UserRepository userRepository;
    private final DoseOverviewService overviewService;
    @Value("${jura.doses.auto-missed-enabled:true}")
    private boolean enabled = true;

    @Scheduled(fixedDelayString = "${jura.doses.scan-interval-ms:60000}", initialDelayString = "${jura.doses.scan-interval-ms:60000}")
    public void markMissedDoses() {
        if (!enabled) return;
        for (User patient : userRepository.findByRole("PATIENT")) {
            try {
                overviewService.refreshMissed(patient.getId());
            } catch (RuntimeException exception) {
                log.warn("Missed-dose scan failed for patient ID {}; will retry next cycle", patient.getId());
            }
        }
    }
}
