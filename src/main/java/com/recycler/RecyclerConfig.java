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
    private static final int MAX_PROCESSING_TIME_TICKS = Short.MAX_VALUE;

    private static final boolean DEFAULT_ENABLE_STAIR_SLAB_CRAFTING = true;

    private static final boolean DEFAULT_ENABLE_GLASS_SHARD_DROPS = true;
    private static final float DEFAULT_GLASS_SHARD_DROP_CHANCE = 1.0f;

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("recycler")
            .resolve("config.json");

    private static int processingTimeTicks = DEFAULT_PROCESSING_TIME_TICKS;

    private static boolean enableStairSlabCrafting =
            DEFAULT_ENABLE_STAIR_SLAB_CRAFTING;

    private static boolean enableGlassShardDrops =
            DEFAULT_ENABLE_GLASS_SHARD_DROPS;

    private static float glassShardDropChance =
            DEFAULT_GLASS_SHARD_DROP_CHANCE;

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

            boolean configuredStairSlabCrafting =
                    DEFAULT_ENABLE_STAIR_SLAB_CRAFTING;

            if (json.has("enable_stair_slab_crafting")) {
                if (!json.get("enable_stair_slab_crafting").isJsonPrimitive()
                        || !json.getAsJsonPrimitive("enable_stair_slab_crafting").isBoolean()) {
                    throw new JsonParseException(
                            "Invalid enable_stair_slab_crafting"
                    );
                }

                configuredStairSlabCrafting =
                        json.get("enable_stair_slab_crafting").getAsBoolean();
            }

			boolean configuredGlassShardDrops =
					DEFAULT_ENABLE_GLASS_SHARD_DROPS;

			float configuredGlassShardChance =
					DEFAULT_GLASS_SHARD_DROP_CHANCE;

			if (json.has("enable_glass_shard_drops")) {
				if (!json.get("enable_glass_shard_drops").isJsonPrimitive()
						|| !json.getAsJsonPrimitive("enable_glass_shard_drops").isBoolean()) {
					throw new JsonParseException(
							"Invalid enable_glass_shard_drops"
					);
				}

				configuredGlassShardDrops =
						json.get("enable_glass_shard_drops").getAsBoolean();
			}

			if (json.has("glass_shard_drop_chance")) {
				if (!json.get("glass_shard_drop_chance").isJsonPrimitive()
						|| !json.getAsJsonPrimitive("glass_shard_drop_chance").isNumber()) {
					throw new JsonParseException(
							"Invalid glass_shard_drop_chance"
					);
				}

				BigDecimal configuredChance = json
						.getAsJsonPrimitive("glass_shard_drop_chance")
						.getAsBigDecimal();

				if (configuredChance.compareTo(BigDecimal.ZERO) < 0
						|| configuredChance.compareTo(BigDecimal.ONE) > 0) {
					throw new JsonParseException(
							"glass_shard_drop_chance must be between 0.0 and 1.0"
					);
				}

				configuredGlassShardChance = configuredChance.floatValue();
			}

            processingTimeTicks = configuredValue.intValueExact();
            enableStairSlabCrafting = configuredStairSlabCrafting;
            enableGlassShardDrops = configuredGlassShardDrops;
            glassShardDropChance = configuredGlassShardChance;

            return true;

        } catch (IOException | JsonParseException | NumberFormatException e) {
            processingTimeTicks = DEFAULT_PROCESSING_TIME_TICKS;
            enableStairSlabCrafting = DEFAULT_ENABLE_STAIR_SLAB_CRAFTING;
            enableGlassShardDrops = DEFAULT_ENABLE_GLASS_SHARD_DROPS;
            glassShardDropChance = DEFAULT_GLASS_SHARD_DROP_CHANCE;

            LOGGER.error(
                    "Failed to load config from {}. Using default configuration.",
                    CONFIG_PATH,
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

        json.addProperty(
                "enable_stair_slab_crafting",
                DEFAULT_ENABLE_STAIR_SLAB_CRAFTING
        );

        json.addProperty(
                "enable_glass_shard_drops",
                DEFAULT_ENABLE_GLASS_SHARD_DROPS
        );

        json.addProperty(
                "glass_shard_drop_chance",
                DEFAULT_GLASS_SHARD_DROP_CHANCE
        );

        Files.writeString(CONFIG_PATH, GSON.toJson(json));

        processingTimeTicks = DEFAULT_PROCESSING_TIME_TICKS;
        enableStairSlabCrafting = DEFAULT_ENABLE_STAIR_SLAB_CRAFTING;
        enableGlassShardDrops = DEFAULT_ENABLE_GLASS_SHARD_DROPS;
        glassShardDropChance = DEFAULT_GLASS_SHARD_DROP_CHANCE;
    }

    public static int getProcessingTimeTicks() {
        return processingTimeTicks;
    }

    public static boolean isStairSlabCraftingEnabled() {
        return enableStairSlabCrafting;
    }

    public static boolean areGlassShardDropsEnabled() {
        return enableGlassShardDrops;
    }

    public static float getGlassShardDropChance() {
        return glassShardDropChance;
    }
}
