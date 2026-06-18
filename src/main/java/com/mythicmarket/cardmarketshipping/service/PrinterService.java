package com.mythicmarket.cardmarketshipping.service;

import com.mythicmarket.cardmarketshipping.config.PrinterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterService {

    private final PrinterConfig printerConfig;

    public void print(byte[] zpl) {
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
