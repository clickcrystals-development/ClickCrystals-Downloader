package net.i_no_am.clickcrystals;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModDownloader implements PreLaunchEntrypoint {

    public static final String README_URL = "https://raw.githubusercontent.com/clickcrystals-development/ClickCrystals/main/README.md";
    public static final String REPO = "https://github.com/clickcrystals-development/ClickCrystals";
    public static final Logger LOGGER = LoggerFactory.getLogger("ClickCrystals Downloader");

    @Override
    public void onPreLaunch() {
        Optional<Chart> chart = getChart();
        chart.ifPresent(ModDownloader::downloadAsset);
    }

    public static Optional<Chart> getChart() {
        try {
            URL url = URI.create(README_URL).toURL();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String mcVersion = getGameVersion();

            LOGGER.info("Looking for Minecraft version: {}", mcVersion);

            boolean inTable = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("| What You Have")) {
                    inTable = true;
                    reader.readLine();
                    continue;
                }

                if (inTable) {
                    if (line.isEmpty() || !line.trim().startsWith("|")) break;
                    LOGGER.debug("Processing table row: {}", line.trim());
                    // Parse the table row - split by | and clean up
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) { // Should have at least 3 parts due to leading/trailing |
                        String versionCell = parts[1].trim(); // First column (version)
                        String downloadCell = parts[2].trim(); // Second column (download link)

                        LOGGER.debug("Version cell: '{}', Download cell: '{}'", versionCell, downloadCell);

                        if (versionCell.equals(mcVersion)) {
                            LOGGER.info("Found matching version: {}", mcVersion);

                            // First try to extract URL from Markdown link format [text](url)
                            Pattern linkPattern = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
                            Matcher matcher = linkPattern.matcher(downloadCell);

                            if (matcher.find()) {
                                String githubUrl = matcher.group(2);
                                LOGGER.info("Found GitHub URL for Minecraft {}: {}", mcVersion, githubUrl);

                                String versionTag = getVersion(githubUrl);
                                if (versionTag != null) {
                                    LOGGER.info("Extracted version tag: {}", versionTag);
                                    return Optional.of(new Chart(mcVersion, versionTag));
                                }
                            }
                            if (downloadCell.toLowerCase().contains("not supported") || downloadCell.toLowerCase().contains("cry") || downloadCell.toLowerCase().contains("how is that even possible")) {
                                LOGGER.warn("Minecraft version {} is not supported by ClickCrystals.", mcVersion);
                                return Optional.empty();
                            }
                            LOGGER.warn("Could not parse download URL from: '{}'", downloadCell);
                        }

                    }
                }
            }

            reader.close();
            LOGGER.warn("Could not find matching Minecraft version ({}) in README.", mcVersion);
        } catch (Exception ex) {
            LOGGER.error("Failed to fetch or parse README.md: {}", ex.getMessage());
            ex.printStackTrace();
        }
        return Optional.empty();
    }

    private static String getVersion(String githubUrl) {
        Pattern versionPattern = Pattern.compile(".*/releases/tag/v([0-9]+\\.[0-9]+\\.[0-9]+)");
        Matcher matcher = versionPattern.matcher(githubUrl);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void downloadAsset(Chart chart) {
        try {
            String downloadUrl = getFile(chart.url());
            String fileName = getAsset(chart.url());

            LOGGER.info("Constructed download URL: {}", downloadUrl);

            URL url = URI.create(downloadUrl).toURL();
            FabricLoader loader = FabricLoader.getInstance();
            File gameDir = loader.getGameDir().toFile();
            Optional<ModContainer> clickcrystals = loader.getModContainer("clickcrystals");

            LOGGER.info("Checking for environment...");
            if (clickcrystals.isPresent()) {
                String version = clickcrystals.get().getMetadata().getVersion().getFriendlyString();
                if (chart.url().contains(version)) {
                    LOGGER.info("ClickCrystals is UP-TO-DATE!");
                    return;
                }
            }

            LOGGER.info("Preparing for install...");
            File file = new File("%s/mods/%s".formatted(gameDir, fileName));

            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            if (!file.exists()) file.createNewFile();

            InputStream is = url.openStream();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] data = is.readAllBytes();
            double size = Math.floor(data.length / 10000.0) / 100.0;

            LOGGER.info("Downloading ClickCrystals ({}) MB", size);
            fos.write(data);
            fos.flush();
            fos.close();
            is.close();

            LOGGER.info("ClickCrystals Installation Completed Successfully!");
            LOGGER.info("Restart Your Game, ClickCrystals Will Work In Your Next Game Launch");
            System.exit(-1);
        } catch (Exception ex) {
            LOGGER.error("An error occurred while installing ClickCrystals: {}", ex.getMessage());
        }
    }

    public static String getAsset(String latest) {
        return "ClickCrystals-%s-%s.jar".formatted(getGameVersion(), latest);
    }

    public static String getFile(String latest) {
        return "%s/releases/download/v%s/%s".formatted(REPO, latest, getAsset(latest));
    }

    private static String getGameVersion() {
        Optional<ModContainer> minecraft = FabricLoader.getInstance().getModContainer("minecraft");
        return minecraft.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElseThrow(() -> new IllegalArgumentException("Minecraft is not installed!"));
    }

    public record Chart(String version, String url) {
    }
}