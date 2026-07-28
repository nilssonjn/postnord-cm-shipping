package com.mythicmarket.cardmarketshipping.service;

import com.mythicmarket.cardmarketshipping.config.PostNordConfig;
import com.mythicmarket.cardmarketshipping.dto.LabelRequest;
import com.mythicmarket.cardmarketshipping.service.payload.EdiInstruction;
import com.mythicmarket.cardmarketshipping.service.payload.EdiInstruction.*;
import com.mythicmarket.cardmarketshipping.util.CountryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostNordPayloadBuilder {

    private final PostNordConfig config;

    public EdiInstruction build(LabelRequest labelRequest, PostNordConfig.ServiceEntry serviceEntry) {
        String isoCode = CountryUtil.toIsoCode(labelRequest.countryName());
        double weightKg = labelRequest.weightGrams() / 1000.0;

        // SENDER
        Consignor consignor = new Consignor(
                config.getIssuerCode(),
                new PartyIdentification(config.getCustomerNumber(), "160"),
                new PartyDetails(
                        new NameIdentification(config.getSender().getName()),
                        null,
                        new Address(
                                List.of(config.getSender().getStreet()),
                                config.getSender().getCity(),
                                config.getSender().getZipCode(),
                                config.getSender().getCountryCode()
                        )
                ));
        // RECIPIENT
        Consignee consignee = new Consignee(
                new PartyDetails(
                        new NameIdentification(labelRequest.buyerName()),
                        List.of(new Contact(labelRequest.buyerName())),
                        new Address(
                                List.of(labelRequest.street()),
                                labelRequest.city(),
                                labelRequest.postalCode(),
                                isoCode)
                )
        );

        List<String> addons = serviceEntry.getAddon() != null && !serviceEntry.getAddon().isBlank()
                ? List.of(serviceEntry.getAddon())
                : null;

        Shipment shipment = new Shipment(
                new Parties(consignor, consignee),
                new Service(serviceEntry.getCode(), addons),
                List.of(new GoodsItem(config.getDefaultPackageType(), List.of(
                        new Item(new ItemIdentification("0"), new GrossWeight(weightKg, "KGM"))
                )))
        );

        PostNordConfig.Application configApp = config.getApplication();
        Application application = (configApp != null && configApp.getApplicationId() != null)
                ? new Application(configApp.getApplicationId(), configApp.getName(), configApp.getVersion())
                : null;

        return new EdiInstruction(
                OffsetDateTime.now().toString(),
                "Instruction",
                labelRequest.orderId() + "_" + System.currentTimeMillis(),
                "Original", // "Original" = new booking; Change/Cancellation are seperate code paths, not a config switch
                application,
                config.isTestMode(), // set to true while testing - validates without creating a real booking
                List.of(shipment)
        );
    }
}
