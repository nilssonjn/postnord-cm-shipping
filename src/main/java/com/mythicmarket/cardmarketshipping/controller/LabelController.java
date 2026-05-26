package com.mythicmarket.cardmarketshipping.controller;

import com.mythicmarket.cardmarketshipping.dto.LabelRequest;
import com.mythicmarket.cardmarketshipping.service.PostNordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelController {

    private final PostNordService postNordService;

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generate(@Valid @RequestBody LabelRequest request) {
        byte[] pdf = postNordService.generateLabel(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"label-" + request.orderId() + ".pdf\"")
                .body(pdf);
    }

}
