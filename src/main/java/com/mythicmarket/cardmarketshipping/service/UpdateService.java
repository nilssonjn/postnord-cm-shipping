package com.mythicmarket.cardmarketshipping.service;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class UpdateService implements ApplicationRunner {

    private static final String ASSET_NAME = "CardmarketShipping-windows.zip";

    private final RestClient restClient;

    // BuildProperties is only present when the JAR was built with the build-info Maven goal.
    // It is absent in IDE runs, so injection is optional.
    @Autowired(required = false)
    @Nullable
    private BuildProperties buildProperties;

    @Value("${app.github-repo}")
    private String githubRepo;

    @Value("${app.update-check-enabled:true}")
    private boolean updateCheckEnabled;

    public UpdateService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!updateCheckEnabled) {
            return;
        }
        if (buildProperties == null) {
            log.debug("BuildProperties not available — update check skipped");
            return;
        }
        try {
            checkForUpdate();
        } catch (Exception e) {
            log.warn("Update check failed: {}", e.getMessage());
        }
    }

    private void checkForUpdate() throws IOException {
        assert buildProperties != null;
        String currentVersion = buildProperties.getVersion();

        Map<String, Object> release = fetchLatestRelease();
        if (release == null) {
            log.info("Nothing to update");
            return;
        }

        String tagName = (String) release.get("tag_name");
        String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

        if (!isNewer(latestVersion, currentVersion)) {
            log.info("Nothing to update");
            return;
        }

        log.info("New version {} available. Update now? [Y/n]: ", latestVersion);
        Scanner scanner = new Scanner(System.in);
        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (!answer.isEmpty() && !answer.equalsIgnoreCase("y")) {
            log.info("Skipping update");
            return;
        }

        log.info("Downloading...");

        Path appDir = resolveAppDir();
        if (appDir == null) {
            log.warn("Cannot determine app directory — update skipped (running in dev mode?)");
            return;
        }

        String downloadUrl = findAssetUrl(release);
        if (downloadUrl == null) {
            log.warn("No download asset found in release {}", latestVersion);
            return;
        }

        Path newJar = downloadAndExtractJar(downloadUrl, appDir);
        writeAndLaunchUpdater(appDir, newJar);

        log.info("Restarting to apply update...");
        System.exit(0);
    }

    private @Nullable Map<String, Object> fetchLatestRelease() {
        String url = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
        return restClient.get()
                .uri(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "cardmarket-shipping-updater")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private @Nullable String findAssetUrl(Map<String, Object> release) {
        var assets = (List<Map<String, Object>>) release.get("assets");
        if (assets == null) return null;
        return assets.stream()
                .filter(a -> ASSET_NAME.equals(a.get("name")))
                .map(a -> (String) a.get("browser_download_url"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the jpackage app directory by walking up from java.home.
     * In a packaged app-image the layout is:
     *   CardmarketShipping/
     *     runtime/   <- java.home
     *     app/       <- the JAR lives here
     * Returns null when running outside a jpackage image (e.g. from the IDE).
     */
    private @Nullable Path resolveAppDir() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        Path candidate = javaHome.getParent().resolve("app");
        return Files.exists(candidate.resolve("cardmarket-shipping.jar")) ? candidate : null;
    }

    private Path downloadAndExtractJar(String downloadUrl, Path appDir) throws IOException {
        // Create the temp file inside appDir (controlled directory) rather than the
        // system temp directory, which may be world-writable on some platforms.
        Path tempZip = Files.createTempFile(appDir, "cardmarket-shipping-update-", ".zip");
        try {
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection conn = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(120_000);
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempZip, StandardCopyOption.REPLACE_EXISTING);
            }

            Path newJar = appDir.resolve("cardmarket-shipping.jar.new");
            extractJarFromZip(tempZip, newJar);
            return newJar;
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }

    private void extractJarFromZip(Path zipFile, Path destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith("app/cardmarket-shipping.jar")) {
                    Files.copy(zis, destination, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new IOException("cardmarket-shipping.jar not found inside update ZIP");
    }

    private void writeAndLaunchUpdater(Path appDir, Path newJar) throws IOException {
        Path appRoot = appDir.getParent();
        Path currentJar = appDir.resolve("cardmarket-shipping.jar");
        Path exePath = appRoot.resolve("CardmarketShipping.exe");
        Path batPath = appRoot.resolve("update.bat");

        String bat = "@echo off\r\n"
                + "timeout /t 2 >nul\r\n"
                + "move /y \"" + newJar.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"\r\n"
                + "start \"\" \"" + exePath.toAbsolutePath() + "\"\r\n"
                + "del \"%~f0\"\r\n";

        Files.writeString(batPath, bat);

        // The Process is intentionally not closed — closing it would send SIGTERM
        // and kill the updater before it can replace the JAR.
        @SuppressWarnings("resource")
        Process _ = new ProcessBuilder("cmd.exe", "/c", "start", "/min", "", batPath.toString())
                .start();
    }

    private boolean isNewer(String latest, String current) {
        int[] l = parseVersion(latest);
        int[] c = parseVersion(current);
        for (int i = 0; i < 3; i++) {
            if (l[i] > c[i]) return true;
            if (l[i] < c[i]) return false;
        }
        return false;
    }

    private int[] parseVersion(String v) {
        String[] parts = v.split("\\.", 3);
        int[] nums = new int[3];
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                nums[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException _) {
                // non-numeric version segment treated as 0
            }
        }
        return nums;
    }
}
