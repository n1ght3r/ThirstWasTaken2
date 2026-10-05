package com.thirstwastaken2.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.gametest.platform.LoadConditions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * The Mod Items switches: turning one off takes that item's recipes away on the next data load.
 *
 * <p>Reloading data in the middle of a test run would reload it under every other test as well, so
 * these read the recipe files the game loaded and ask the loader's own condition codec what it would
 * decide under the changed config. That covers the condition's registration, its id and field, the
 * NeoForge build's translation, and the config it reads; the loading itself is the loader's.
 */
public final class ModItemsGameTest {
    /** A switch, and the data files that should follow it. */
    private record Switch(String name, BiConsumer<ThirstConfig, Boolean> set, List<String> files) { }

    private static final List<Switch> SWITCHES = List.of(
            new Switch("enableBowls", (config, on) -> config.enableBowls = on, List.of(
                    "recipe/clay_bowl", "recipe/terracotta_bowl_from_smelting", "recipe/terracotta_water_bowl",
                    "recipe/purify_water_bowl_0_smelting", "recipe/purify_water_bowl_2_campfire",
                    "advancement/recipes/misc/purify_water_bowl")),
            new Switch("enableWaterskin", (config, on) -> config.enableWaterskin = on, List.of("recipe/waterskin")),
            new Switch("enableCopperCanteen", (config, on) -> config.enableCopperCanteen = on,
                    List.of("recipe/copper_canteen")),
            new Switch("enableIronFlask", (config, on) -> config.enableIronFlask = on, List.of(
                    "recipe/iron_flask", "recipe/purify_water_iron_flask_1_0_smelting",
                    "advancement/recipes/misc/purify_water_iron_flask")),
            new Switch("enableCopperHangingPot", (config, on) -> config.enableCopperHangingPot = on,
                    List.of("recipe/copper_hanging_pot")),
            new Switch("enableIronHangingPot", (config, on) -> config.enableIronHangingPot = on,
                    List.of("recipe/iron_hanging_pot")),
            new Switch("enableCopperDistiller", (config, on) -> config.enableCopperDistiller = on, List.of(
                    "recipe/copper_distiller", "recipe/copper_pipe", "recipe/distiller_boiler", "recipe/cooling_tub",
                    "recipe/brick_firebox", "advancement/recipes/misc/copper_distiller")));

    /** Recipes for vanilla containers, which no switch touches. */
    private static final List<String> UNSWITCHED = List.of(
            "recipe/purify_water_bottle_0_smelting", "recipe/purify_water_bucket_1_smoking");

    @GameTest
    public void recipesLoadWhileTheirItemIsOn(GameTestHelper helper) {
        for (Switch toggle : SWITCHES) {
            for (String file : toggle.files()) {
                JsonObject json = read(helper, file);
                TestFixtures.check(helper, LoadConditions.present(json), file + " should carry a load condition");
                TestFixtures.check(helper, LoadConditions.hold(json), file + " should load with " + toggle.name() + " on");
            }
        }
        helper.succeed();
    }

    @GameTest
    public void aSwitchedOffItemLosesOnlyItsOwnRecipes(GameTestHelper helper) {
        for (Switch off : SWITCHES) {
            TestFixtures.withConfig(config -> off.set().accept(config, false), () -> {
                for (Switch toggle : SWITCHES) {
                    boolean expected = toggle != off;
                    for (String file : toggle.files()) {
                        TestFixtures.check(helper, LoadConditions.hold(read(helper, file)) == expected,
                                file + (expected ? " should still load" : " should not load") + " with " + off.name() + " off");
                    }
                }
            });
        }
        helper.succeed();
    }

    @GameTest
    public void vanillaContainersArePurifiedWhateverTheSwitches(GameTestHelper helper) {
        TestFixtures.withConfig(config -> SWITCHES.forEach(toggle -> toggle.set().accept(config, false)), () -> {
            for (String file : UNSWITCHED) {
                TestFixtures.check(helper, LoadConditions.hold(read(helper, file)),
                        file + " should load with every Mod Items switch off");
            }
        });
        helper.succeed();
    }

    /** A data file of the mod's own, as the server loaded it. */
    private static JsonObject read(GameTestHelper helper, String path) {
        // 1.21 renamed the data directories from plural to singular; the paths here are the newer ones.
        //? if >=1.21 {
        String file = path;
        //?} else {
        /*String file = path.replaceFirst("^(recipe|advancement)/", "$1s/");
        *///?}
        Optional<Resource> resource = helper.getLevel().getServer().getResourceManager()
                .getResource(ThirstWasTaken2.id(file + ".json"));
        TestFixtures.check(helper, resource.isPresent(), path + ".json is missing from the mod's data");
        try (Reader reader = resource.orElseThrow().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
