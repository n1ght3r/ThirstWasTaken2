package com.thirstwastaken2.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.platform.Clientbound;
import com.thirstwastaken2.platform.Loader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * What data packs say items restore, read from {@code data/<namespace>/thirstwastaken2/drinks/*.json}.
 *
 * <p>A file lists any number of items:
 *
 * <pre>{@code
 * {
 *   "values": {
 *     "mymod:lemonade": { "thirst": 6, "quenched": 8 },
 *     "mymod:ice_cube": { "thirst": 1 }
 *   }
 * }
 * }</pre>
 *
 * <p>{@code quenched} defaults to 0, and both are clamped to {@code 0..ThirstData.MAX}. An entry that
 * restores nothing ({@code 0} and {@code 0}) takes the item out of the tag and keyword fallbacks, which
 * is how a pack takes a value away; only the config's {@code itemBlacklist} beats every other source.
 * An item nobody registered is skipped with one warning per file, never an error: the mod that adds it
 * may simply not be installed. A pack higher in the list replaces a file of the same path outright,
 * as it does a recipe; two different files naming one item resolve by file id, the later one winning.
 *
 * <p>The server parses on every data reload and hands the result to each client through
 * {@link DrinkValuesPayload}, on join and after {@code /reload}, because the tooltip resolves on the
 * client. The client never parses; in singleplayer it shares this class with the integrated server,
 * so what it receives is what is already here.
 */
public final class DataPackDrinks {
    /** Under {@code data/<namespace>/}. */
    public static final String DIRECTORY = ThirstWasTaken2.MOD_ID + "/drinks";
    /** The reload listener's id, which is what a mod orders its own listener against. */
    public static final Identifier RELOAD_ID = ThirstWasTaken2.id("drinks");

    private static final FileToIdConverter FILES = FileToIdConverter.json(DIRECTORY);

    /** Swapped whole, never edited, so a reader never sees half a reload. */
    private static volatile Map<Item, int[]> values = Map.of();
    private static Clientbound<DrinkValuesPayload> channel;

    private DataPackDrinks() { }

    /** Declares {@link DrinkValuesPayload}, on both sides, during {@code initialize}. */
    public static void registerPayload() {
        channel = Loader.clientboundPayload(DrinkValuesPayload.ID, DrinkValuesPayload::write, DrinkValuesPayload::read,
                DataPackDrinks::receive);
    }

    /** @return {thirst, quenched} as a data pack gave it, or {@code null} when no pack names the item. */
    public static int[] get(Item item) {
        return values.get(item);
    }

    /** How many items the data packs name. */
    public static int size() {
        return values.size();
    }

    /** The server's data reload: parses every file and replaces what was there. */
    public static void reload(ResourceManager manager) {
        Map<Identifier, Resource> files = FILES.listMatchingResources(manager);
        List<Identifier> ids = new ArrayList<>(files.keySet());
        ids.sort(Comparator.comparing(Identifier::toString));

        Map<Item, int[]> parsed = new IdentityHashMap<>();
        Map<Item, Identifier> namedBy = new IdentityHashMap<>();
        for (Identifier file : ids) {
            Map<Item, int[]> entries;
            try (Reader reader = files.get(file).openAsReader()) {
                entries = read(file, JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException e) {
                // A broken file is skipped whole, so a typo never leaves half of it applied.
                ThirstWasTaken2.LOGGER.warn("Skipping thirst values in {}: {}", file, e.getMessage());
                continue;
            }
            entries.forEach((item, amounts) -> {
                Identifier earlier = namedBy.put(item, file);
                if (earlier != null) {
                    ThirstWasTaken2.LOGGER.warn("{} is given thirst values by both {} and {}; {} wins",
                            BuiltInRegistries.ITEM.getKey(item), earlier, file, file);
                }
                parsed.put(item, amounts);
            });
        }
        replace(parsed);
        ThirstWasTaken2.LOGGER.info("Loaded thirst values for {} items from data packs", parsed.size());
    }

    /** Sends the current values to one player; called on join and after every reload. */
    public static void sync(ServerPlayer player) {
        if (channel != null) channel.send(player, new DrinkValuesPayload(values));
    }

    /** The client's side of {@link #sync}. */
    public static void receive(DrinkValuesPayload payload) {
        if (payload.values() == values) return;
        replace(payload.values());
    }

    private static void replace(Map<Item, int[]> next) {
        Map<Item, int[]> previous = values;
        values = Collections.unmodifiableMap(next);
        if (!sameValues(previous, next)) ThirstApi.clearCache();
    }

    private static Map<Item, int[]> read(Identifier file, JsonElement json) {
        JsonObject entries = json.getAsJsonObject().getAsJsonObject("values");
        if (entries == null) throw new JsonParseException("no \"values\" object");

        Map<Item, int[]> parsed = new IdentityHashMap<>();
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) throw new JsonParseException("\"" + entry.getKey() + "\" is not an item id");
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item == Items.AIR) {
                missing.add(entry.getKey());
                continue;
            }
            JsonObject value = entry.getValue().getAsJsonObject();
            parsed.put(item, new int[]{amount(value, "thirst"), amount(value, "quenched")});
        }
        if (!missing.isEmpty()) {
            ThirstWasTaken2.LOGGER.warn("Skipping {} unknown item(s) in {}: {}", missing.size(), file, missing);
        }
        return parsed;
    }

    private static int amount(JsonObject value, String key) {
        JsonElement element = value.get(key);
        if (element == null) {
            if (key.equals("thirst")) throw new JsonParseException("an entry has no \"thirst\"");
            return 0;
        }
        return Math.max(0, Math.min(ThirstData.MAX, element.getAsInt()));
    }

    private static boolean sameValues(Map<Item, int[]> a, Map<Item, int[]> b) {
        if (a.size() != b.size()) return false;
        for (Map.Entry<Item, int[]> entry : a.entrySet()) {
            if (!Arrays.equals(entry.getValue(), b.get(entry.getKey()))) return false;
        }
        return true;
    }
}
