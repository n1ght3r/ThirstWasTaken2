package com.thirstwastaken2.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thirstwastaken2.config.ThirstConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The three water settings renamed from Purity to Quality: a file written before the rename keeps its
 * values, one written after is read as it is, and a file holding both trusts the new name. Saving
 * afterwards writes only the new names.
 */
public final class ConfigMigrationGameTest {
    @GameTest
    public void legacyKeysAreReadIntoTheNewOnes(GameTestHelper helper) {
        JsonObject json = parse("{\"defaultPurity\": 1, \"rainwaterPurity\": 0, \"dripstonePurity\": 2}");
        ThirstConfig config = ThirstConfig.read(json);
        TestFixtures.check(helper, config.defaultQuality == 1 && config.rainwaterQuality == 0
                        && config.dripstoneQuality == 2,
                "the legacy values should carry over, got " + config.defaultQuality + ", "
                        + config.rainwaterQuality + ", " + config.dripstoneQuality);
        TestFixtures.check(helper, !json.has("defaultPurity") && !json.has("rainwaterPurity")
                && !json.has("dripstonePurity"), "the legacy keys should be dropped, left " + json);
        helper.succeed();
    }

    @GameTest
    public void newKeysAreReadAsTheyAre(GameTestHelper helper) {
        ThirstConfig config = ThirstConfig.read(parse("{\"defaultQuality\": 0, \"rainwaterQuality\": 3}"));
        TestFixtures.check(helper, config.defaultQuality == 0 && config.rainwaterQuality == 3,
                "the new keys should be read, got " + config.defaultQuality + " and " + config.rainwaterQuality);
        // Missing keys keep their defaults.
        TestFixtures.check(helper, config.dripstoneQuality == new ThirstConfig().dripstoneQuality,
                "a missing key should keep its default, got " + config.dripstoneQuality);
        helper.succeed();
    }

    @GameTest
    public void theNewKeyWinsWhenBothArePresent(GameTestHelper helper) {
        JsonObject json = parse("{\"defaultPurity\": 1, \"defaultQuality\": 3}");
        ThirstConfig config = ThirstConfig.read(json);
        TestFixtures.check(helper, config.defaultQuality == 3,
                "with both keys the new one should win, got " + config.defaultQuality);
        TestFixtures.check(helper, !json.has("defaultPurity"), "the legacy key should still be dropped");
        helper.succeed();
    }

    @GameTest
    public void savingWritesOnlyTheNewKeys(GameTestHelper helper) {
        ThirstConfig config = ThirstConfig.read(parse("{\"dripstonePurity\": 1}"));
        String saved = new Gson().toJson(config);
        TestFixtures.check(helper, !saved.contains("Purity\""), "a saved config should name no legacy key");
        TestFixtures.check(helper, saved.contains("\"dripstoneQuality\":1"),
                "the custom value should be saved under the new key");
        helper.succeed();
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
