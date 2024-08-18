package net.i_no_am.clickcrystals;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class ModDownloader implements ClientModInitializer {

	public static final String REPO = "https://github.com/clickcrystals-development/ClickCrystals";
	public static final String INFO_LINK = "https://itzispyder.github.io/clickcrystals/info";
	public static final Logger LOGGER = LoggerFactory.getLogger("ClickCrystals Downloader");

	@Override
	public void onInitializeClient() {
		Info info = requestInfo();
		downloadAsset(info);
	}

	public static Info requestInfo() {
		try {
			URL url = URI.create(INFO_LINK).toURL();
			InputStream is = url.openStream();
			String json = new String(is.readAllBytes());

			is.close();

			Gson gson = new Gson();
			return gson.fromJson(json, Info.class);
		}
		catch (Exception ex) {
			LOGGER.error("An unexpected error occurred while trying to download mod information from {}: {}", INFO_LINK, ex);
			ex.printStackTrace();
			return new Info();
		}
	}

	public static void downloadAsset(Info info) {
		try {
			URL url = URI.create(info.getFile()).toURL();
			FabricLoader loader = FabricLoader.getInstance();
			File gameDir = loader.getGameDir().toFile();
			Optional<ModContainer> clickcrystals = loader.getModContainer("clickcrystals");

			LOGGER.info("Checking for environment...");
			if (clickcrystals.isPresent()) {
				String version = clickcrystals.get().getMetadata().getVersion().getFriendlyString();
				if (version.contains(info.latest)) {
					LOGGER.info("ClickCrystals is UP-TO-DATE!");
					return;
				}
			}

			LOGGER.info("Preparing for install...");

			File file = new File("%s/mods/%s".formatted(gameDir, info.getAsset()));

			if (!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			if (!file.exists())
				file.createNewFile();

			InputStream is = url.openStream();
			FileOutputStream fos = new FileOutputStream(file);
			byte[] data = is.readAllBytes();
			double size = Math.floor(data.length / 10000.0) / 100.0;

			LOGGER.info("Downloading ClickCrystals {} ({} MB)", info.latest, size);

			fos.write(data);
			fos.flush();
			fos.close();
			is.close();

			LOGGER.info("ClickCrystals Installation Completed Successfully!");
			LOGGER.info("Restart Your Game, ClickCrystals Will Work In Your Next Game Launch");
			System.exit(-1);
		}
		catch (Exception ex) {
			LOGGER.error("An unexpected error occurred while trying to install ClickCrystals: {}", ex.getMessage());
			ex.printStackTrace();
		}
	}

	public static class Info {
		public String latest = "unknown";
		public final Map<String, String> versionMappings = new HashMap<>();

		public String getGameVersion() {
			FabricLoader loader = FabricLoader.getInstance();
			Optional<ModContainer> minecraft = loader.getModContainer("minecraft");

			if (minecraft.isEmpty())
				throw new IllegalArgumentException("Minecraft is not installed!");

			String version = minecraft.get().getMetadata().getVersion().getFriendlyString();
			return versionMappings.getOrDefault(version, "null");
		}

		public String getAsset() {
			return "ClickCrystals-%s-%s.jar".formatted(getGameVersion(), latest);
		}

		public String getFile() {
			return "%s/releases/download/v%s/%s".formatted(REPO, latest, getAsset());
		}
	}
}