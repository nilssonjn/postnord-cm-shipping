package com.mythicmarket.cardmarketshipping.dto;

import java.util.List;

public record PostNordLabelResponse(List<LabelPrintoutEntry> labelPrintout) {
    public record LabelPrintoutEntry(Printout printout) {
    }

    public record Printout(String data) {
    }
}
