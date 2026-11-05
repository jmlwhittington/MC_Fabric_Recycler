
package com.recycler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class RecoveryRule {

    private static final Path CONFIG_DIRECTORY =
            FabricLoader.getInstance().getConfigDir().resolve("recycler");

    private static final Path CONFIG_FILE =
            CONFIG_DIRECTORY.resolve("recovery_rules.json");

    private static Map<Identifier, List<OutputRule>> itemRules =
            Map.of();

    private static Map<Identifier, List<OutputRule>> tagRules =
            Map.of();

    private RecoveryRule() {
    }

    private record OutputRule(Item item, int count, double chance) {
    }

    public static void initialize() {
        try {
            Files.createDirectories(CONFIG_DIRECTORY);

            if (!Files.exists(CONFIG_FILE)) {
                String defaultConfig = """
                        {
                          "items": {},
                          "tags": {}
                        }
                        """;

                Files.writeString(CONFIG_FILE, defaultConfig);

                RecyclerMod.LOGGER.info(
                        "Created recovery rules configuration at {}",
                        CONFIG_FILE
                );
            }

            reload();

        } catch (IOException exception) {
            RecyclerMod.LOGGER.error(
                    "Could not create the recovery rules configuration.",
                    exception
            );
        }
    }

    public static boolean reload() {
        try {
            String json = Files.readString(CONFIG_FILE);
            JsonElement rootElement = JsonParser.parseString(json);

            if (!rootElement.isJsonObject()) {
                throw new IllegalArgumentException(
                        "The configuration root must be a JSON object."
                );
            }

            JsonObject root = rootElement.getAsJsonObject();

            Map<Identifier, List<OutputRule>> newItemRules =
                    parseRules(root, "items", true);

            Map<Identifier, List<OutputRule>> newTagRules =
                    parseRules(root, "tags", false);

            itemRules = Map.copyOf(newItemRules);
            tagRules = Map.copyOf(newTagRules);

            RecyclerMod.LOGGER.info("Recovery rules loaded successfully.");

            return true;

        } catch (Exception exception) {
            RecyclerMod.LOGGER.error(
                    "Failed to load recovery rules. Keeping the previous valid configuration.",
                    exception
            );

            return false;
        }
    }

    private static Map<Identifier, List<OutputRule>> parseRules(
            JsonObject root,
            String sectionName,
            boolean validateItemKeys
    ) {
        Map<Identifier, List<OutputRule>> result =
                new LinkedHashMap<>();

        if (!root.has(sectionName)) {
            return result;
        }

        JsonElement sectionElement = root.get(sectionName);

        if (!sectionElement.isJsonObject()) {
            throw new IllegalArgumentException(
                    "'" + sectionName + "' must be a JSON object."
            );
        }

        JsonObject section = sectionElement.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
            Identifier id = parseIdentifier(entry.getKey());

            if (validateItemKeys
                    && BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown item in '" + sectionName + "': " + id
                );
            }

            if (!entry.getValue().isJsonArray()) {
                throw new IllegalArgumentException(
                        "Rules for " + id + " must be a JSON array."
                );
            }

            JsonArray outputs = entry.getValue().getAsJsonArray();
            List<OutputRule> parsedOutputs = new ArrayList<>();

            for (JsonElement outputElement : outputs) {
                if (!outputElement.isJsonObject()) {
                    throw new IllegalArgumentException(
                            "An output rule for " + id + " must be an object."
                    );
                }

                JsonObject output = outputElement.getAsJsonObject();

                if (!output.has("item")) {
                    throw new IllegalArgumentException(
                            "An output rule for " + id + " is missing 'item'."
                    );
                }

                Identifier outputId =
                        parseIdentifier(output.get("item").getAsString());

                Item outputItem = BuiltInRegistries.ITEM
                        .getOptional(outputId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown output item: " + outputId
                        ));

                int count = output.has("count")
                        ? output.get("count").getAsInt()
                        : 1;

                double chance = output.has("chance")
                        ? output.get("chance").getAsDouble()
                        : 100.0;

                if (count <= 0) {
                    throw new IllegalArgumentException(
                            "Output count must be greater than zero."
                    );
                }

                if (!Double.isFinite(chance)
                        || chance < 0.0
                        || chance > 100.0) {
                    throw new IllegalArgumentException(
                            "Chance must be between 0 and 100."
                    );
                }

                parsedOutputs.add(
                        new OutputRule(outputItem, count, chance)
                );
            }

            result.put(id, List.copyOf(parsedOutputs));
        }

        return result;
    }

    private static Identifier parseIdentifier(String value) {
        Identifier id = Identifier.tryParse(value);

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid identifier: " + value
            );
        }

        return id;
    }

    public static Optional<List<ItemStack>> getRecoveryResult(
            ItemStack input
    ) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }

        Identifier inputId =
                BuiltInRegistries.ITEM.getKey(input.getItem());

        List<OutputRule> exactRules = itemRules.get(inputId);

        if (exactRules != null) {
            return Optional.of(rollOutputs(exactRules));
        }

        for (Map.Entry<Identifier, List<OutputRule>> entry
                : tagRules.entrySet()) {

            TagKey<Item> tag = TagKey.create(
                    Registries.ITEM,
                    entry.getKey()
            );

            if (input.is(tag)) {
                return Optional.of(rollOutputs(entry.getValue()));
            }
        }

        return Optional.empty();
    }

    private static List<ItemStack> rollOutputs(
            List<OutputRule> rules
    ) {
        List<ItemStack> results = new ArrayList<>();

        for (OutputRule rule : rules) {
            double roll =
                    ThreadLocalRandom.current().nextDouble(100.0);

            if (roll < rule.chance()) {
                results.add(
                        new ItemStack(rule.item(), rule.count())
                );
            }
        }

        return results;
    }
}
