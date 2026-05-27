package com.mythicmarket.cardmarketshipping.service;

import com.mythicmarket.cardmarketshipping.config.PostNordConfig;
import com.mythicmarket.cardmarketshipping.config.PostNordConfig.ServiceEntry;
import com.mythicmarket.cardmarketshipping.dto.LabelRequest;
import com.mythicmarket.cardmarketshipping.dto.PostNordLabelResponse;
import com.mythicmarket.cardmarketshipping.service.payload.EdiInstruction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostNordService {

    private final PostNordConfig config;
    private final PostNordPayloadBuilder postNordPayloadBuilder;
    private final RestClient restClient;

    public byte[] generateLabel(LabelRequest labelRequest) {
        log.info("Generating label for orderId={}, serviceType={}", labelRequest.orderId(), labelRequest.serviceType());

        ServiceEntry entry = config.getServiceCodes().get(labelRequest.serviceType());
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Unknown service type: " + labelRequest.serviceType() +
                            ". Check application.yaml serviceCodes map.");
        }

        EdiInstruction payload = postNordPayloadBuilder.build(labelRequest, entry);

        String url = config.getUrl() + config.getApiKey() + config.getUrlParams();

        PostNordLabelResponse response;
        try {
            response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(PostNordLabelResponse.class);
        } catch (RestClientException e) {
            log.error("PostNord API call failed for orderId={}: {}", labelRequest.orderId(), e.getMessage());
            throw new IllegalStateException("PostNord API call failed - see logs for details.", e);
        }

        String base64Pdf = extractBase64Pdf(response);
        log.info("Label generated successfully for orderId={}", labelRequest.orderId());
        return Base64.getDecoder().decode(base64Pdf);
    }

    private String extractBase64Pdf(PostNordLabelResponse postnordLabelResponse) {
        if (postnordLabelResponse == null || postnordLabelResponse.labelPrintout()
                == null || postnordLabelResponse.labelPrintout().isEmpty()) {
            log.error("Postnord did not return a label. Response body: {}", postnordLabelResponse);
            throw new IllegalArgumentException("Postnord booking failed - see logs for details.");
        }
        return postnordLabelResponse.labelPrintout().getFirst().printout().data();
    }

}
