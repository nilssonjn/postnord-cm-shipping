package com.mythicmarket.cardmarketshipping.service.payload;

import java.util.List;

public record EdiInstruction(
        String messageDate,
        String updateIndicator,
        boolean testIndicator,
        List<Shipment> shipment
) {
    public record Shipment(
            Parties parties,
            Service service,
            List<GoodsItem> goodsItem
    ) {
    }

    public record Parties(Consignor consignor, Consignee consignee) {
    }

    public record Consignor(
            String issuerCode,
            PartyIdentification partyIdentification,
            PartyDetails party
    ) {
    }

    public record Consignee(PartyDetails party) {
    }

    public record PartyIdentification(String partyId) {
    }

    public record PartyDetails(NameIdentification nameIdentification, Address address) {
    }

    public record NameIdentification(String name) {
    }

    public record Address(
            List<String> streets,
            String city,
            String postalCode,
            String countryCode
    ) {
    }

    public record Service(String basicServiceCode) {
    }

    public record GoodsItem(String packageTypeCode, List<Item> items) {
    }

    public record ItemIdentification(String itemId) {
    }

    public record GrossWeight(double value, String unit) {
    }

    public record Item(ItemIdentification itemIdentification, GrossWeight grossWeight) {
    }
}
