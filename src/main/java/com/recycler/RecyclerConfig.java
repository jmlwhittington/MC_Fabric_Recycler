package com.example.mymod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecyclerConfig {

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

    private RecyclerConfig() {
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                saveDefaultConfig();
                return;
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

            int configuredTime = json.get("processing_time_ticks").getAsInt();

            if (configuredTime < MIN_PROCESSING_TIME_TICKS) {
                throw new JsonParseException(
                        "processing_time_ticks must be at least 1"
                );
            }

            processingTimeTicks = configuredTime;

        } catch (IOException | JsonParseException | NumberFormatException e) {
            processingTimeTicks = DEFAULT_PROCESSING_TIME_TICKS;

            System.err.println(
                    "[Recycler] Failed to load config from "
                            + CONFIG_PATH
                            + ". Using default processing time of "
                            + DEFAULT_PROCESSING_TIME_TICKS
                            + " ticks."
            );

            e.printStackTrace();
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
