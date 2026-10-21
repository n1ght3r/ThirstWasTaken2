package com.thirstwastaken2.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.platform.Loader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the active {@link ThirstConfig} lives and how it reaches the file: reading, carrying renamed
 * keys over, writing, and the snapshot the config screen restores on Cancel. {@link ThirstConfig}'s
 * static methods hand over to this; nothing else calls it.
 *
 * <p>The instance is swapped wholesale on {@link #load()}; {@link #generation()} increments on every
 * swap so derived caches (compiled patterns, per-item lookups) can invalidate themselves cheaply.
 */
final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Loader.configDir().resolve("thirstwastaken2.json");
    private static volatile ThirstConfig instance;
    private static volatile int generation;

    private ConfigStore() { }

    static ThirstConfig get() {
        ThirstConfig config = instance;
        return config != null ? config : load();
    }

    static Path path() {
        return PATH;
    }

    static int generation() {
        return generation;
    }

    static synchronized ThirstConfig load() {
        ThirstConfig loaded = null;
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                JsonElement json = JsonParser.parseReader(reader);
                if (json.isJsonObject()) loaded = read(json.getAsJsonObject());
            } catch (Exception exception) {
                ThirstWasTaken2.LOGGER.error("Could not read {}", PATH, exception);
            }
        }
        ThirstConfig config = loaded == null ? new ThirstConfig() : loaded;
        config.sanitize();
        instance = config;
        generation++;
        save();
        return config;
    }

    static ThirstConfig read(JsonObject json) {
        migrate(json);
        ThirstConfig config = GSON.fromJson(json, ThirstConfig.class);
        if (config == null) config = new ThirstConfig();
        config.sanitize();
        return config;
    }

    /**
     * Carries a setting saved under an old name over to its new one, once, on the parsed file before
     * Gson maps it: the old value is taken only when the new key is absent, so a file holding both
     * trusts the new one, and the old key is dropped so the next save writes the new name alone. The
     * purification rework renamed the three water settings from purity to quality.
     */
    private static void migrate(JsonObject json) {
        rename(json, "defaultPurity", "defaultQuality");
        rename(json, "rainwaterPurity", "rainwaterQuality");
        rename(json, "dripstonePurity", "dripstoneQuality");
    }

    private static void rename(JsonObject json, String legacy, String current) {
        JsonElement value = json.remove(legacy);
        if (value != null && !json.has(current)) json.add(current, value);
    }

    static synchronized void save() {
        ThirstConfig config = instance;
        if (config == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            ThirstWasTaken2.LOGGER.error("Could not write {}", PATH, exception);
        }
    }

    static synchronized void commit() {
        ThirstConfig config = instance;
        if (config == null) return;
        config.sanitize();
        generation++;
        save();
    }

    static synchronized ThirstConfig snapshot() {
        ThirstConfig copy = GSON.fromJson(GSON.toJson(get()), ThirstConfig.class);
        copy.sanitize();
        return copy;
    }

    static synchronized void restore(ThirstConfig snapshot) {
        snapshot.sanitize();
        instance = snapshot;
        generation++;
    }
}
