package net.i_no_am.clickcrystals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Environment(EnvType.CLIENT)
public class ModDownloader implements ClientModInitializer {
	private static final Logger LOGGER = Logger.getLogger(ModDownloader.class.getName());

	@Override
	public void onInitializeClient() {
		String version = "ClickCrystals-" + getGameVersion() + "-" + getLatestClickCrystalsVersion();
		File file = Paths.get(FabricLoader.getInstance().getGameDir().toString(), "mods", version + ".jar").toFile();
		boolean wasBefore = file.exists();

		try (BufferedInputStream bis = new BufferedInputStream(new URL("https://github.com/clickcrystals-development/ClickCrystals/releases/download/v" + getLatestClickCrystalsVersion() + "/" + version + ".jar").openStream());
			 FileOutputStream fos = new FileOutputStream(file)) {

			System.out.println("Downloading: " + version);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = bis.read(buffer, 0, 1024)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}

			if (!wasBefore) {
				System.out.println("ClickCrystals has been installed. Please restart the game.");
				System.exit(-1);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error during ClickCrystals installation", e);
		}
	}

	private static String getLatestClickCrystalsVersion() {
		String version = "";
		try {
			URL url = new URL("https://itzispyder.github.io/clickcrystals/info.html");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder content = new StringBuilder();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();

			JsonObject jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
			version = jsonObject.get("latest").getAsString();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error fetching the latest ClickCrystals version", e);
		}
		return version;
	}

	private static String getGameVersion() {
		FabricLoader loader = FabricLoader.getInstance();
		ModContainer container = loader.getModContainer("minecraft").orElse(null);
		String gameVersion = null;
		if (container != null) {
			gameVersion = container.getMetadata().getVersion().getFriendlyString();
		}
		return switch (Objects.requireNonNull(gameVersion)) {
			case "1.20.1" -> "1.20.2";
			case "1.20.3" -> "1.20.4";
			case "1.20.5" -> "1.20.6";
			default -> gameVersion;
		};
	}
}
