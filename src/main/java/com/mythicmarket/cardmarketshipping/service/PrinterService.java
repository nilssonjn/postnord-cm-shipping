package com.mythicmarket.cardmarketshipping.service;

import com.mythicmarket.cardmarketshipping.config.PrinterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.*;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterService {

    private final PrinterConfig printerConfig;

    public void print(byte[] zpl) {
        String name = printerConfig.getName();
        PrintService printService = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Printer not found: '%s'".formatted(name)));
        try {
            DocPrintJob job = printService.createPrintJob();
            job.print(new SimpleDoc(zpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
            log.info("ZPL sent to printer '{}'", name);
        } catch (PrintException e) {
            log.error("Failed to print to printer '{}'", name, e);
            throw new RuntimeException("Failed to print to printer '%s'".formatted(name), e);
        }
    }

}
