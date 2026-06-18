package com.mythicmarket.cardmarketshipping.service;

import com.mythicmarket.cardmarketshipping.config.PrinterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterService {

    private final PrinterConfig printerConfig;

    public void print(byte[] zpl) {
        if (printerConfig.isLocalMode()) {
            printLocal(zpl);
        } else {
            printTcp(zpl);
        }
    }

    private void printLocal(byte[] zpl) {
        String name = printerConfig.getName();
        PrintService printService = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Printer not found: '%s'".formatted(name)));
        try {
            DocPrintJob job = printService.createPrintJob();
            job.print(new SimpleDoc(zpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
            log.info("ZPL sent to local printer '{}'", name);
        } catch (PrintException e) {
            log.error("Failed to print to local printer '{}'", name, e);
            throw new RuntimeException("Failed to print to local printer '%s'".formatted(name), e);
        }
    }

    private void printTcp(byte[] zpl) {
        String host = printerConfig.getHost();
        int port = printerConfig.getPort();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), printerConfig.getConnectTimeoutMs());
            OutputStream out = socket.getOutputStream();
            out.write(zpl);
            out.flush();
            log.info("ZPL sent to printer {}:{}", host, port);
        } catch (IOException e) {
            log.error("Failed to send ZPL to printer {}:{}", host, port, e);
            throw new RuntimeException("Printer unreachable at %s:%d".formatted(host, port), e);
        }
    }

}
