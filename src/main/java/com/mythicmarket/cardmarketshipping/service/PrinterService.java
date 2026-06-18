package com.mythicmarket.cardmarketshipping.service;

import com.mythicmarket.cardmarketshipping.config.PrinterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterService {

    private final PrinterConfig printerConfig;

    public void print(byte[] zpl) {
        try (Socket socket = new Socket(printerConfig.getHost(), printerConfig.getPort());
             OutputStream out = socket.getOutputStream()) {
            out.write(zpl);
            out.flush();
            log.info("ZPL sent to printer {}:{}", printerConfig.getHost(), printerConfig.getPort());
        } catch (IOException e) {
            log.error("Failed to send ZPL to printer {}:{}", printerConfig.getHost(), printerConfig.getPort(), e);
            throw new RuntimeException("Printer unreachable", e);
        }
    }

}
