
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RecoveryRule {

	private static final Logger LOGGER =
        LoggerFactory.getLogger(RecoveryRule.class);

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

	public record PotentialRuleOutput(ItemStack stack) {}

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
            }

            reload();

        } catch (IOException exception) {
		    LOGGER.error(
					"Failed to initialize Recycler recovery-rule configuration at '{}': {}",
					CONFIG_DIRECTORY,
					exception.getMessage(),
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
                    parseItemRules(root);

			Map<Identifier, List<OutputRule>> newTagRules =
					parseTagRules(root);

            itemRules = Collections.unmodifiableMap(
                    new LinkedHashMap<>(newItemRules)
            );

            tagRules = Collections.unmodifiableMap(
                    new LinkedHashMap<>(newTagRules)
            );

            return true;

        } catch (Exception exception) {
			LOGGER.error(
					"Failed to load Recycler recovery rules from '{}': {}",
					CONFIG_FILE,
					exception.getMessage(),
					exception
			);

			return false;
        }
    }

    private static Map<Identifier, List<OutputRule>> parseItemRules(
            JsonObject root
    ) {
        Map<Identifier, List<OutputRule>> result =
                new LinkedHashMap<>();

        if (!root.has("items")) {
            return result;
        }

        JsonElement sectionElement = root.get("items");

        if (!sectionElement.isJsonObject()) {
            throw new IllegalArgumentException(
                    "'items' must be a JSON object."
            );
        }

        JsonObject section = sectionElement.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
            Identifier inputId = parseIdentifier(entry.getKey());

            getRegisteredItem(inputId);

            List<OutputRule> outputs =
                    parseOutputRules(entry.getValue(), inputId.toString());

            result.put(inputId, outputs);
        }

        return result;
    }

	private static Map<Identifier, List<OutputRule>> parseTagRules(
			JsonObject root
	) {
		Map<Identifier, List<OutputRule>> result =
				new LinkedHashMap<>();

		if (!root.has("tags")) {
			return result;
		}

		JsonElement sectionElement = root.get("tags");

		if (!sectionElement.isJsonObject()) {
			throw new IllegalArgumentException(
					"'tags' must be a JSON object."
			);
		}

		JsonObject section = sectionElement.getAsJsonObject();

		for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
			Identifier tagId = parseIdentifier(entry.getKey());

			List<OutputRule> outputs =
					parseOutputRules(entry.getValue(), tagId.toString());

			result.put(tagId, outputs);
		}

		return result;
	}

    private static List<OutputRule> parseOutputRules(
            JsonElement element,
            String description
    ) {
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException(
                    "Rules for " + description + " must be a JSON array."
            );
        }

        JsonArray outputs = element.getAsJsonArray();
        List<OutputRule> parsedOutputs = new ArrayList<>();

        for (JsonElement outputElement : outputs) {
            if (!outputElement.isJsonObject()) {
                throw new IllegalArgumentException(
                        "An output rule for " + description + " must be an object."
                );
            }

            JsonObject output = outputElement.getAsJsonObject();

            if (!output.has("item")) {
                throw new IllegalArgumentException(
                        "An output rule for " + description + " is missing 'item'."
                );
            }

			String outputIdentifier = output.get("item").getAsString();

			Item outputItem;

			try {
				Identifier outputId = parseIdentifier(outputIdentifier);
				outputItem = getRegisteredItem(outputId);

			} catch (RuntimeException exception) {
				throw new IllegalArgumentException(
						"Invalid output item '" + outputIdentifier
								+ "' in rules for " + description
								+ ": " + exception.getMessage(),
						exception
				);
			}

			int count = 1;

			if (output.has("count")) {
				JsonElement countElement = output.get("count");

				if (!countElement.isJsonPrimitive()
						|| !countElement.getAsJsonPrimitive().isNumber()) {
					throw new IllegalArgumentException(
							"Output count must be a whole number."
					);
				}

				try {
					count = countElement.getAsBigDecimal().intValueExact();
				} catch (RuntimeException exception) {
					throw new IllegalArgumentException(
							"Output count must be a whole number within the integer range.",
							exception
					);
				}
			}

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

        return List.copyOf(parsedOutputs);
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

    private static Item getRegisteredItem(Identifier id) {
        return BuiltInRegistries.ITEM
                .getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown item: " + id
                ));
    }

	public static boolean hasApplicableRule(ItemStack input) {
		if (input == null || input.isEmpty()) {
			return false;
		}

		Identifier inputId =
				BuiltInRegistries.ITEM.getKey(input.getItem());

		if (itemRules.containsKey(inputId)) {
			return true;
		}

		for (Identifier tagId : tagRules.keySet()) {
			TagKey<Item> tag = TagKey.create(
					Registries.ITEM,
					tagId
			);

			if (input.is(tag)) {
				return true;
			}
		}

		return false;
	}

	public static Optional<List<PotentialRuleOutput>> getPotentialRecoveryResult(
			ItemStack input
	) {
		if (input == null || input.isEmpty()) {
			return Optional.empty();
		}

		Identifier inputId =
				BuiltInRegistries.ITEM.getKey(input.getItem());

		List<OutputRule> exactRules = itemRules.get(inputId);

		if (exactRules != null) {
			return Optional.of(convertPotentialOutputs(exactRules));
		}

		for (Map.Entry<Identifier, List<OutputRule>> entry
				: tagRules.entrySet()) {

			TagKey<Item> tag = TagKey.create(
					Registries.ITEM,
					entry.getKey()
			);

			if (input.is(tag)) {
				return Optional.of(
						convertPotentialOutputs(entry.getValue())
				);
			}
		}

		return Optional.empty();
	}

	private static List<PotentialRuleOutput> convertPotentialOutputs(
			List<OutputRule> rules
	) {
		List<PotentialRuleOutput> results = new ArrayList<>();

		for (OutputRule rule : rules) {
			results.add(
					new PotentialRuleOutput(
							new ItemStack(rule.item(), rule.count())
					)
			);
		}

		return results;
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
	
	public static String getConfigurationFingerprint() {
		StringBuilder canonical = new StringBuilder();

		appendFingerprintSection(
				canonical,
				"items",
				itemRules,
				true
		);

		appendFingerprintSection(
				canonical,
				"tags",
				tagRules,
				false
		);

		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
					.digest(
							canonical.toString().getBytes(StandardCharsets.UTF_8)
					);

			return HexFormat.of().formatHex(hash);

		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(
					"SHA-256 is unavailable.",
					exception
			);
		}
	}

	private static void appendFingerprintSection(
			StringBuilder builder,
			String sectionName,
			Map<Identifier, List<OutputRule>> rules,
			boolean sortEntries
	) {
		builder.append(sectionName).append('\n');

		List<Map.Entry<Identifier, List<OutputRule>>> entries =
				new ArrayList<>(rules.entrySet());

		if (sortEntries) {
			entries.sort(
					Comparator.comparing(
							entry -> entry.getKey().toString()
					)
			);
		}

		for (Map.Entry<Identifier, List<OutputRule>> entry : entries) {
			builder.append(entry.getKey()).append('\n');

			for (OutputRule rule : entry.getValue()) {
				builder.append(
						BuiltInRegistries.ITEM.getKey(rule.item())
				).append('|')
						.append(rule.count()).append('|')
						.append(Double.toString(rule.chance()))
						.append('\n');
			}
		}
	}
}
