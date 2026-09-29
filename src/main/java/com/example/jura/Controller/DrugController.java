package com.example.jura.Controller;

import com.example.jura.Service.DrugSearchService;
import com.example.jura.Service.SfdaSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugSearchService drugSearchService;
    private final SfdaSyncService sfdaSyncService;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam(defaultValue = "1") Integer page) {
        return ResponseEntity.status(200).body(drugSearchService.search(q, page));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync() {
        return ResponseEntity.status(200).body(sfdaSyncService.syncAllDrugs());
    }
}
