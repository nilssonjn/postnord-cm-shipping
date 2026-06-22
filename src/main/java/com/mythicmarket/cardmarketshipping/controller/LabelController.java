package com.mythicmarket.cardmarketshipping.controller;

import com.mythicmarket.cardmarketshipping.config.PrinterConfig;
import com.mythicmarket.cardmarketshipping.dto.LabelRequest;
import com.mythicmarket.cardmarketshipping.service.PostNordService;
import com.mythicmarket.cardmarketshipping.service.PrinterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelController {

    private final PostNordService postNordService;
    private final PrinterConfig printerConfig;
    private final PrinterService printerService;

    @PostMapping(value = "/generate", produces = "application/zpl")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody LabelRequest request) {
        byte[] label = postNordService.generateLabel(request);

        if (printerConfig.isEnabled()) {
            try {
                printerService.print(label);
                return ResponseEntity.ok().build();
            } catch (RuntimeException e) {
                log.error("Printing failed for order '{}' — falling back to download", request.orderId(), e);
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"label-" + request.orderId() + ".zpl\"")
                .body(label);
    }

}
