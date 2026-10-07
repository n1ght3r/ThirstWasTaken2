package com.thirstwastaken2.client.config;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.client.platform.ClientVanilla;
import com.thirstwastaken2.compat.AppleSkin;
import com.thirstwastaken2.config.QuenchedOverlay;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * One page of the config screen: its icon in the sidebar, its settings, and any rows that are not a
 * setting. Every scalar in {@link ThirstConfig} belongs to exactly one page. A page that covers several
 * topics is split into {@link ConfigSection} tabs, one per topic; a new setting goes in the tab of its
 * topic, and length alone is never a reason to split or merge one. The per-item values are
 * edited item by item on Item Values ({@link ItemValueRows}); the keyword patterns stay in the file,
 * which Item Values opens. Reset puts back exactly the page's entries, so the item values are never
 * reset from the footer, only one row at a time.
 */
enum ConfigCategory {
    THIRST("thirst", ThirstWasTaken2.id("textures/item/waterskin_3.png"),
            ConfigSection.of("thirst.drain", List.of(
                    ConfigEntry.percent("thirst_depletion_modifier", 0, 1000,
                            config -> config.thirstDepletionModifier, (config, value) -> config.thirstDepletionModifier = value),
                    ConfigEntry.toggle("thirst_depletion_in_peaceful",
                            config -> config.thirstDepletionInPeaceful, (config, value) -> config.thirstDepletionInPeaceful = value),
                    ConfigEntry.toggle("prevent_sprinting_when_thirsty",
                            config -> config.preventSprintingWhenThirsty, (config, value) -> config.preventSprintingWhenThirsty = value),
                    ConfigEntry.toggle("dehydration_halts_health_regen",
                            config -> config.dehydrationHaltsHealthRegen, (config, value) -> config.dehydrationHaltsHealthRegen = value),
                    ConfigEntry.number("food_heal_min_thirst_percent", 0, 100, ConfigEntry::wholePercent,
                            config -> config.foodHealMinThirstPercent, (config, value) -> config.foodHealMinThirstPercent = value),
                    ConfigEntry.percent("quenched_health_regen", 0, 100,
                            config -> config.quenchedHealthRegen, (config, value) -> config.quenchedHealthRegen = value),
                    ConfigEntry.number("quenched_heal_min_food_percent", 0, 100, ConfigEntry::wholePercent,
                            config -> config.quenchedHealMinFoodPercent, (config, value) -> config.quenchedHealMinFoodPercent = value),
                    // Only Cold Sweat measures the temperature this reads, so without it the switch is left off the page.
                    ConfigEntry.toggle("cold_sweat_climate",
                            config -> config.coldSweatClimate, (config, value) -> config.coldSweatClimate = value)
                            .requires("cold_sweat"))),
            // Only Serene Seasons keeps the calendar these read, so without it the tab is left off the page.
            ConfigSection.of("thirst.seasons", List.of(
                    ConfigEntry.toggle("serene_seasons_climate",
                            config -> config.sereneSeasonsClimate, (config, value) -> config.sereneSeasonsClimate = value)
                            .requires("sereneseasons"),
                    seasonDrain("season_drain_spring",
                            config -> config.seasonDrainSpring, (config, value) -> config.seasonDrainSpring = value),
                    seasonDrain("season_drain_summer",
                            config -> config.seasonDrainSummer, (config, value) -> config.seasonDrainSummer = value),
                    seasonDrain("season_drain_autumn",
                            config -> config.seasonDrainAutumn, (config, value) -> config.seasonDrainAutumn = value),
                    seasonDrain("season_drain_winter",
                            config -> config.seasonDrainWinter, (config, value) -> config.seasonDrainWinter = value)))),

    WATER("water", ThirstWasTaken2.id("textures/item/terracotta_water_bowl_purity_3.png"),
            ConfigSection.of("water.drinking", List.of(
                    ConfigEntry.grade("default_quality",
                            config -> config.defaultQuality, (config, value) -> config.defaultQuality = value),
                    // One serving of plain water, the same from every container; two values of one array,
                    // set in place like the quenched shares.
                    ConfigEntry.number("plain_water_thirst", 0, ThirstData.MAX, ConfigEntry::points,
                            config -> config.plainWaterValue[0], (config, value) -> config.plainWaterValue[0] = value),
                    ConfigEntry.number("plain_water_quenched", 0, ThirstData.MAX, ConfigEntry::points,
                            config -> config.plainWaterValue[1], (config, value) -> config.plainWaterValue[1] = value),
                    ConfigEntry.number("plain_water_drink_ticks", ThirstConfig.MIN_DRINK_TICKS,
                            ThirstConfig.MAX_DRINK_TICKS, ConfigEntry::ticks,
                            config -> config.plainWaterDrinkTicks, (config, value) -> config.plainWaterDrinkTicks = value),
                    ConfigEntry.toggle("can_drink_by_hand",
                            config -> config.canDrinkByHand, (config, value) -> config.canDrinkByHand = value))),
            ConfigSection.of("water.quenched", List.of(
                    quenchedPercent("quenched_percent_dirty", 0),
                    quenchedPercent("quenched_percent_murky", 1),
                    quenchedPercent("quenched_percent_clean", 2),
                    quenchedPercent("quenched_percent_pure", 3))),
            ConfigSection.of("water.sea_water", List.of(
                    ConfigEntry.toggle("enable_sea_water",
                            config -> config.enableSeaWater, (config, value) -> config.enableSeaWater = value),
                    ConfigEntry.number("sea_water_nausea_seconds", 0, ThirstConfig.MAX_EFFECT_SECONDS, ConfigEntry::seconds,
                            config -> config.seaWaterNauseaSeconds, (config, value) -> config.seaWaterNauseaSeconds = value),
                    ConfigEntry.number("sea_water_parched_seconds", 0, ThirstConfig.MAX_EFFECT_SECONDS, ConfigEntry::seconds,
                            config -> config.seaWaterParchedSeconds, (config, value) -> config.seaWaterParchedSeconds = value))),
            ConfigSection.of("water.collected", List.of(
                    ConfigEntry.toggle("enable_rain_collection",
                            config -> config.enableRainCollection, (config, value) -> config.enableRainCollection = value),
                    ConfigEntry.grade("rainwater_quality",
                            config -> config.rainwaterQuality, (config, value) -> config.rainwaterQuality = value),
                    ConfigEntry.grade("dripstone_quality",
                            config -> config.dripstoneQuality, (config, value) -> config.dripstoneQuality = value)))),

    // What bad water does to the drinker is a subject of its own: how effects add up, then one tab per
    // difficulty since each has its own table. The tables are edited line by line (SicknessRows) and
    // reset one grade at a time from its heading, or all at once with the General tab by the footer's
    // Reset while this page is open.
    SICKNESS("sickness", Identifier.withDefaultNamespace("textures/item/spider_eye.png"),
            ConfigSection.of("sickness.general", List.of(
                    ConfigEntry.toggle("extend_sickness_effects",
                            config -> config.extendSicknessEffects, (config, value) -> config.extendSicknessEffects = value))),
            sicknessTable("peaceful"),
            sicknessTable("easy"),
            sicknessTable("normal"),
            sicknessTable("hard")),

    APPLESKIN("appleskin", Identifier.withDefaultNamespace("textures/item/apple.png"), ConfigSection.whole(List.of(
            ConfigEntry.choice("appleskin_quenched_overlay", QuenchedOverlay.values(),
                    config -> config.appleskinQuenchedOverlay, (config, value) -> config.appleskinQuenchedOverlay = value),
            ConfigEntry.toggle("appleskin_tooltip_droplets",
                    config -> config.appleskinTooltipDroplets, (config, value) -> config.appleskinTooltipDroplets = value)))) {
        @Override
        void addLeadingRows(List<ConfigRow> rows) {
            rows.add(ConfigRow.preview());
            // Both settings only show anything alongside AppleSkin, so the page says so when it is
            // missing rather than offering switches that appear to do nothing.
            if (!AppleSkin.isLoaded()) rows.add(ConfigRow.note(Component.translatable("thirstwastaken2.config.appleskin_missing")));
        }
    },

    // The item list is long by nature, so it gets a tab of its own and the switches stay one click away.
    ITEMS("items", Identifier.withDefaultNamespace("textures/item/honey_bottle.png"),
            new ConfigSection("items.list", List.of(), ItemValueRows::addPage),
            new ConfigSection("items.matching", List.of(
                    ConfigEntry.toggle("enable_drink_tag_matching",
                            config -> config.enableDrinkTagMatching, (config, value) -> config.enableDrinkTagMatching = value),
                    ConfigEntry.toggle("enable_keyword_matching",
                            config -> config.enableKeywordMatching, (config, value) -> config.enableKeywordMatching = value)),
                    (rows, refresh) -> rows.add(ConfigRow.action(Component.translatable("thirstwastaken2.config.open_file"),
                            Component.translatable("thirstwastaken2.config.open_file.tooltip"),
                            Component.translatable("thirstwastaken2.config.open_file.button"),
                            () -> ClientVanilla.openPath(ThirstConfig.path()))))),

    MOD_ITEMS("mod_items", Identifier.withDefaultNamespace("textures/block/crafting_table_front.png"),
            ConfigSection.of("mod_items.items", List.of(
                    ConfigEntry.toggle("enable_bowls",
                            config -> config.enableBowls, (config, value) -> config.enableBowls = value),
                    ConfigEntry.toggle("enable_waterskin",
                            config -> config.enableWaterskin, (config, value) -> config.enableWaterskin = value),
                    ConfigEntry.toggle("enable_copper_canteen",
                            config -> config.enableCopperCanteen, (config, value) -> config.enableCopperCanteen = value),
                    ConfigEntry.toggle("enable_iron_flask",
                            config -> config.enableIronFlask, (config, value) -> config.enableIronFlask = value))),
            ConfigSection.of("mod_items.blocks", List.of(
                    ConfigEntry.toggle("enable_copper_hanging_pot",
                            config -> config.enableCopperHangingPot, (config, value) -> config.enableCopperHangingPot = value),
                    ConfigEntry.toggle("enable_iron_hanging_pot",
                            config -> config.enableIronHangingPot, (config, value) -> config.enableIronHangingPot = value),
                    ConfigEntry.toggle("enable_copper_distiller",
                            config -> config.enableCopperDistiller, (config, value) -> config.enableCopperDistiller = value)))) {
        @Override
        void addLeadingRows(List<ConfigRow> rows) {
            // Recipes are only read as data loads, so a switch here does nothing until the next load.
            rows.add(ConfigRow.note(Component.translatable("thirstwastaken2.config.mod_items_reload")));
        }
    },

    CONTAINERS("containers", ThirstWasTaken2.id("textures/item/iron_flask.png"),
            ConfigSection.of("containers.capacity", List.of(
                    capacity("waterskin_capacity",
                            config -> config.waterskinCapacity, (config, value) -> config.waterskinCapacity = value),
                    capacity("copper_canteen_capacity",
                            config -> config.copperCanteenCapacity, (config, value) -> config.copperCanteenCapacity = value),
                    capacity("iron_flask_capacity",
                            config -> config.ironFlaskCapacity, (config, value) -> config.ironFlaskCapacity = value))),
            // How many filled bowls share a slot, which is a different thing from how much one
            // holds: each is still a single serving.
            ConfigSection.of("containers.stacks", List.of(
                    ConfigEntry.number("terracotta_water_bowl_stack_size", 1, ThirstConfig.MAX_CONTAINER, ConfigEntry::points,
                            config -> config.terracottaWaterBowlStackSize,
                            (config, value) -> config.terracottaWaterBowlStackSize = value))),
            ConfigSection.of("containers.boiling", List.of(
                    ConfigEntry.toggle("enable_boiling_in_hand",
                            config -> config.enableBoilingInHand, (config, value) -> config.enableBoilingInHand = value),
                    ConfigEntry.toggle("enable_furnace_boiling",
                            config -> config.enableFurnaceBoiling, (config, value) -> config.enableFurnaceBoiling = value),
                    ConfigEntry.number("copper_canteen_boil_seconds", 1, ThirstConfig.MAX_BOIL_SECONDS, ConfigEntry::seconds,
                            config -> config.copperCanteenBoilSeconds, (config, value) -> config.copperCanteenBoilSeconds = value),
                    ConfigEntry.number("iron_flask_boil_seconds", 1, ThirstConfig.MAX_BOIL_SECONDS, ConfigEntry::seconds,
                            config -> config.ironFlaskBoilSeconds, (config, value) -> config.ironFlaskBoilSeconds = value))),
            ConfigSection.of("containers.hanging_pots", List.of(
                    capacity("copper_hanging_pot_capacity",
                            config -> config.copperHangingPotCapacity, (config, value) -> config.copperHangingPotCapacity = value),
                    capacity("iron_hanging_pot_capacity",
                            config -> config.ironHangingPotCapacity, (config, value) -> config.ironHangingPotCapacity = value),
                    ConfigEntry.number("copper_hanging_pot_boil_seconds", 1, ThirstConfig.MAX_BOIL_SECONDS, ConfigEntry::seconds,
                            config -> config.copperHangingPotBoilSeconds,
                            (config, value) -> config.copperHangingPotBoilSeconds = value),
                    ConfigEntry.number("iron_hanging_pot_boil_seconds", 1, ThirstConfig.MAX_BOIL_SECONDS, ConfigEntry::seconds,
                            config -> config.ironHangingPotBoilSeconds,
                            (config, value) -> config.ironHangingPotBoilSeconds = value))),
            ConfigSection.of("containers.distiller", List.of(
                    ConfigEntry.number("distiller_serving_seconds", 1, ThirstConfig.MAX_BOIL_SECONDS, ConfigEntry::seconds,
                            config -> config.distillerServingSeconds,
                            (config, value) -> config.distillerServingSeconds = value),
                    capacity("distiller_tank_servings",
                            config -> config.distillerTankServings,
                            (config, value) -> config.distillerTankServings = value))));

    private final String key;
    private final Identifier icon;
    private final List<ConfigSection> sections;
    private final List<ConfigEntry<?>> entries;

    ConfigCategory(String key, Identifier icon, ConfigSection... sections) {
        this.key = key;
        this.icon = icon;
        // A tab whose every setting needs a missing mod is left off, as its settings are.
        this.sections = Arrays.stream(sections).filter(section -> !section.isEmpty()).toList();
        List<ConfigEntry<?>> all = new ArrayList<>();
        for (ConfigSection section : this.sections) all.addAll(section.entries());
        this.entries = List.copyOf(all);
    }

    /** Servings a container holds, 1 to {@link ThirstConfig#MAX_CONTAINER}. */
    private static ConfigEntry<Integer> capacity(String key, Function<ThirstConfig, Integer> getter,
                                                 BiConsumer<ThirstConfig, Integer> setter) {
        return ConfigEntry.number(key, 1, ThirstConfig.MAX_CONTAINER, ConfigEntry::servings, getter, setter);
    }

    /** A season's factor on the drain, shown on the Seasons tab only while Serene Seasons is installed. */
    private static ConfigEntry<Double> seasonDrain(String key, Function<ThirstConfig, Double> getter,
                                                   BiConsumer<ThirstConfig, Double> setter) {
        return ConfigEntry.percent(key, (int) Math.round(ThirstConfig.MIN_SEASON_DRAIN * 100),
                (int) Math.round(ThirstConfig.MAX_SEASON_DRAIN * 100), getter, setter).requires("sereneseasons");
    }

    /** One difficulty's tab of the sickness tables. */
    private static ConfigSection sicknessTable(String difficulty) {
        return new ConfigSection("sickness." + difficulty, List.of(), SicknessRows.page(difficulty));
    }

    /**
     * One grade's share of a drink's quenched. The value is an element of an array, so it is set in place.
     * The label names the grade in its tooltip colour.
     */
    private static ConfigEntry<Integer> quenchedPercent(String key, int grade) {
        return ConfigEntry.number(key, 0, 100, ConfigEntry::wholePercent,
                config -> config.quenchedPercent[grade], (config, value) -> config.quenchedPercent[grade] = value)
                .labelled(ConfigEntry.gradeName(grade));
    }

    Component title() {
        return Component.translatable("thirstwastaken2.config.section." + key);
    }

    Component description() {
        return Component.translatable("thirstwastaken2.config.section." + key + ".tooltip");
    }

    /** A 16x16 texture drawn whole, beside the page's name in the sidebar. */
    Identifier icon() {
        return icon;
    }

    /** Every setting on the page, across its sections, for the search and the footer's Reset. */
    List<ConfigEntry<?>> entries() {
        return entries;
    }

    /** The page's tabs, in order; one section means the page shows no tabs. */
    List<ConfigSection> sections() {
        return sections;
    }

    /** Rows placed between the page heading, or its tabs, and its settings, on every tab. */
    void addLeadingRows(List<ConfigRow> rows) { }
}
