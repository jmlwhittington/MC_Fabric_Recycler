package com.recycler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RecyclerConfig {

	private static final Logger LOGGER =
        LoggerFactory.getLogger("Recycler");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final int DEFAULT_PROCESSING_TIME_TICKS = 100;
    private static final int MIN_PROCESSING_TIME_TICKS = 1;

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("recycler")
            .resolve("config.json");

    private static int processingTimeTicks = DEFAULT_PROCESSING_TIME_TICKS;
	private static final int MAX_PROCESSING_TIME_TICKS = Short.MAX_VALUE;

    private RecyclerConfig() {
    }

	public static boolean load() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());

			if (!Files.exists(CONFIG_PATH)) {
				saveDefaultConfig();
				return true;
			}

			JsonObject json = GSON.fromJson(
					Files.readString(CONFIG_PATH),
					JsonObject.class
			);

			if (json == null || !json.has("processing_time_ticks")
					|| !json.get("processing_time_ticks").isJsonPrimitive()
					|| !json.getAsJsonPrimitive("processing_time_ticks").isNumber()) {
				throw new JsonParseException(
						"Missing or invalid processing_time_ticks"
				);
			}

			BigDecimal configuredValue = json
					.getAsJsonPrimitive("processing_time_ticks")
					.getAsBigDecimal();

			if (configuredValue.stripTrailingZeros().scale() > 0
					|| configuredValue.compareTo(
							BigDecimal.valueOf(MIN_PROCESSING_TIME_TICKS)
					) < 0
					|| configuredValue.compareTo(
							BigDecimal.valueOf(MAX_PROCESSING_TIME_TICKS)
					) > 0) {
				throw new JsonParseException(
						"processing_time_ticks must be a whole number between "
								+ MIN_PROCESSING_TIME_TICKS
								+ " and "
								+ MAX_PROCESSING_TIME_TICKS
				);
			}

			processingTimeTicks = configuredValue.intValueExact();

			return true;

		} catch (IOException | JsonParseException | NumberFormatException e) {
			processingTimeTicks = DEFAULT_PROCESSING_TIME_TICKS;

			LOGGER.error(
					"Failed to load config from {}. Using default processing time of {} ticks.",
					CONFIG_PATH,
					DEFAULT_PROCESSING_TIME_TICKS,
					e
			);

			return false;
		}
	}

    private static void saveDefaultConfig() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty(
                "processing_time_ticks",
                DEFAULT_PROCESSING_TIME_TICKS
        );

        Files.writeString(CONFIG_PATH, GSON.toJson(json));
        processingTimeTicks = DEFAULT_PROCESSING_TIME_TICKS;
    }

    public static int getProcessingTimeTicks() {
        return processingTimeTicks;
    }
}
