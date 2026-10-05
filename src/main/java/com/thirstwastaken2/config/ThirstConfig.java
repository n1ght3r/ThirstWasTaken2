package com.thirstwastaken2.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Loader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loader-independent replacement for the original Forge config specs.
 *
 * <p>The instance is swapped wholesale on {@link #load()}; {@link #generation()} increments on every
 * swap so derived caches (compiled patterns, per-item lookups) can invalidate themselves cheaply.
 */
public final class ThirstConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Loader.configDir().resolve("thirstwastaken2.json");
    private static volatile ThirstConfig INSTANCE;
    private static volatile int generation;
    /** The longest sea water's Nausea or Parched may be set to last, and the config screen's slider end. */
    public static final int MAX_EFFECT_SECONDS = 300;
    /** The longest a serving may be set to take to boil, in a pot or in hand. */
    public static final int MAX_BOIL_SECONDS = 60;
    /** The most servings a distiller tank may hold: ten buckets. */
    public static final int MAX_DISTILLER_TANK = 30;
    /** The range of a season's drain factor, which is also the config screen's slider range. */
    public static final double MIN_SEASON_DRAIN = 0.25;
    public static final double MAX_SEASON_DRAIN = 4.0;

    // ---- thirst depletion -------------------------------------------------
    public double thirstDepletionModifier = 1.2;
    public boolean thirstDepletionInPeaceful = false;
    public boolean preventSprintingWhenThirsty = true;
    public boolean canDrinkByHand = true;
    public boolean dehydrationHaltsHealthRegen = true;
    /**
     * How fast quenched heals, as a share of vanilla's saturation heal, while thirst is full: 0.5 heals
     * half as fast for half the quenched, 0 turns it off. Not in the original, where quenched never healed.
     */
    public double quenchedHealthRegen = 0.5;
    /**
     * The food level, in half shanks, below which quenched stops healing, so a player cannot drink their
     * way past starving. 0 to 20.
     */
    public int quenchedHealMinFood = 10;
    /**
     * With Cold Sweat installed, the drain follows the temperature it measures around the player in
     * place of the biome's. Does nothing without it.
     */
    public boolean coldSweatClimate = true;
    /**
     * With Serene Seasons installed, the drain follows the season: the factors below, and a tropical
     * biome's dry season counting as dry. Does nothing without it.
     */
    public boolean sereneSeasonsClimate = true;
    /** The drain's factor at the middle of each season, blended from one season's middle to the next. */
    public double seasonDrainSpring = 1.0;
    public double seasonDrainSummer = 1.15;
    public double seasonDrainAutumn = 1.0;
    public double seasonDrainWinter = 0.9;

    // ---- AppleSkin (client side, and only while AppleSkin is installed) ----
    public QuenchedOverlay appleskinQuenchedOverlay = QuenchedOverlay.DIAMOND;
    public boolean appleskinTooltipDroplets = true;

    // ---- water purity -----------------------------------------------------
    public int defaultPurity = 2;

    // ---- water balance ----------------------------------------------------
    // The numbers WaterPurity used to hold as constants. Each is read as a drink or a fill happens, never
    // cached, so a change applies at once.
    /** Percent of a drink's quenched that water of each grade gives, Dirty first. */
    public int[] quenchedPercent = {0, 50, 100, 100};
    /** Off, ocean and beach water is graded like any other water instead of being sea water. */
    public boolean enableSeaWater = true;
    public int seaWaterNauseaSeconds = 8;
    public int seaWaterParchedSeconds = 30;
    /** Off, rain fills no hanging pot, and rain in a cauldron is left ungraded, as vanilla leaves it. */
    public boolean enableRainCollection = true;
    public int rainwaterPurity = 2;
    public int dripstonePurity = 3;

    // ---- containers -------------------------------------------------------
    /** Off, the copper canteen and the iron flask no longer boil their water over a campfire. */
    public boolean enableBoilingInHand = true;
    /** 1 to {@code WaterskinItem.MAX_CAPACITY}, which bounds the saved servings and the flask's furnace recipes. */
    public int copperCanteenCapacity = 4;
    public int ironFlaskCapacity = 6;
    public int copperCanteenBoilSeconds = 3;
    public int ironFlaskBoilSeconds = 4;
    public int copperHangingPotBoilSeconds = 4;
    public int ironHangingPotBoilSeconds = 6;
    /** Seconds the copper distiller takes to distil a serving, while its fire burns. */
    public int distillerServingSeconds = 8;
    /** Servings each of the copper distiller's two tanks holds; at least a bucket's three. */
    public int distillerTankServings = 9;

    // ---- water sickness ---------------------------------------------------
    // This replaced quenchWhenDebuffed, nauseaChance, poisonChance and nauseaSeconds in the sickness
    // rework, and later sicknessPreset, whose Realistic and Classic chances were fixed in code. The
    // config has no migration: dropped keys are ignored, and every fresh drink quenches.
    /**
     * What a drink of fresh water gives: difficulty, then grade, then the effects, each rolling on its
     * own. See {@link SicknessEffect}.
     */
    public Map<String, Map<String, List<SicknessEffect>>> sicknessEffects = SicknessEffect.defaults();
    /**
     * On, a drink that gives an effect the player already has adds the line's time to what is left, up to
     * twice the line's time, so drinking bad water while ill makes it last longer. Off, vanilla's rule:
     * the longer of the two is kept.
     */
    public boolean extendSicknessEffects = true;

    // ---- item values ------------------------------------------------------
    /**
     * Items their own mod tags {@code c:drinks} restore {@link #drinkTagValue} when neither list names
     * them. On by default, unlike keyword matching, because the tag is the mod's word rather than a guess.
     */
    public boolean enableDrinkTagMatching = true;
    public int[] drinkTagValue = {6, 8};
    public boolean enableKeywordMatching = false;
    public String keywordBlacklist = "dried|candied|leaf|leaves|gummy|crate|jam|sauce|bucket|seed|cookie|pie|bush|sapling|bean|curry|cake|candy";
    public String drinkKeywords = "drink|juice|tea|soda|coffee|wine|beer|cider|yogurt|milkshake|smoothie";
    public String soupKeywords = "soup|stew|porridge";
    public String fruitKeywords = "fruit|berry|berries|grape|orange|peach|pear|coconut|lemon|melon|cherry|apple";
    public int[] keywordDrinkValue = {10, 14};
    public int[] keywordSoupValue = {4, 5};
    public int[] keywordFruitValue = {2, 3};
    public Set<String> itemBlacklist = new LinkedHashSet<>();
    public Map<String, int[]> drinks = defaultDrinks();
    public Map<String, int[]> foods = defaultFoods();

    // ---- the mod's own items ----------------------------------------------
    // For a pack that brings its own. Off takes the item's recipes and its creative tab entry away; the
    // item stays registered, because registries must match between server and client and a world may
    // still hold one, and a stack that already exists keeps working. See isItemEnabled.
    /** The clay bowl, the terracotta bowl and the water bowl, which make no sense one without the others. */
    public boolean enableBowls = true;
    public boolean enableWaterskin = true;
    public boolean enableCopperCanteen = true;
    public boolean enableIronFlask = true;
    public boolean enableCopperHangingPot = true;
    public boolean enableIronHangingPot = true;
    /** The copper distiller and the four parts it is crafted from, which are good for nothing else. */
    public boolean enableCopperDistiller = true;
    /**
     * The item id of the salt the copper distiller leaves behind from sea water, for a pack whose mods
     * add more than one salt. Empty takes the first item in the {@code thirstwastaken2:distiller_salt}
     * tag, which names other mods' salt only; with none of them the distiller makes no salt. An id that
     * names no item is ignored. See {@code block/DistillerSalt}.
     */
    public String distillerSaltItem = "";

    private transient Pattern keywordBlacklistPattern;
    private transient Pattern drinkKeywordPattern;
    private transient Pattern soupKeywordPattern;
    private transient Pattern fruitKeywordPattern;

    public static ThirstConfig get() {
        ThirstConfig config = INSTANCE;
        return config != null ? config : load();
    }

    /** The config file on disk. */
    public static Path path() {
        return PATH;
    }

    /** Bumped whenever {@link #load()} replaces the active instance. */
    public static int generation() {
        return generation;
    }

    public static synchronized ThirstConfig load() {
        ThirstConfig loaded = null;
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                loaded = GSON.fromJson(reader, ThirstConfig.class);
            } catch (Exception exception) {
                ThirstWasTaken2.LOGGER.error("Could not read {}", PATH, exception);
            }
        }
        ThirstConfig config = loaded == null ? new ThirstConfig() : loaded;
        config.sanitize();
        INSTANCE = config;
        generation++;
        save();
        return config;
    }

    public static synchronized void save() {
        ThirstConfig config = INSTANCE;
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

    /** Re-validates and persists after an in-place edit (used by the config screen). */
    public static synchronized void commit() {
        ThirstConfig config = INSTANCE;
        if (config == null) return;
        config.sanitize();
        generation++;
        save();
    }

    /** A detached copy of the active config, for a screen that may throw its edits away. */
    public static synchronized ThirstConfig snapshot() {
        ThirstConfig copy = GSON.fromJson(GSON.toJson(get()), ThirstConfig.class);
        copy.sanitize();
        return copy;
    }

    /**
     * Makes a {@link #snapshot()} the active config again, without saving: edits are only written by
     * {@link #commit()}, so the file on disk still holds what the snapshot does.
     */
    public static synchronized void restore(ThirstConfig snapshot) {
        snapshot.sanitize();
        INSTANCE = snapshot;
        generation++;
    }

    /**
     * Whether the mod's own item {@code id} may be crafted and is listed in the creative tab. True for
     * any id that is not one of the mod's items. Read by the {@code thirstwastaken2:item_enabled}
     * recipe condition as recipes load, so a change takes effect on the next data reload.
     */
    public boolean isItemEnabled(String id) {
        return switch (id) {
            case "thirstwastaken2:clay_bowl", "thirstwastaken2:terracotta_bowl",
                 "thirstwastaken2:terracotta_water_bowl" -> enableBowls;
            case "thirstwastaken2:waterskin" -> enableWaterskin;
            case "thirstwastaken2:copper_canteen" -> enableCopperCanteen;
            case "thirstwastaken2:iron_flask" -> enableIronFlask;
            case "thirstwastaken2:copper_hanging_pot" -> enableCopperHangingPot;
            case "thirstwastaken2:iron_hanging_pot" -> enableIronHangingPot;
            case "thirstwastaken2:copper_distiller", "thirstwastaken2:copper_pipe", "thirstwastaken2:distiller_boiler",
                 "thirstwastaken2:cooling_tub", "thirstwastaken2:brick_firebox" -> enableCopperDistiller;
            default -> true;
        };
    }

    public Pattern keywordBlacklistPattern() { return keywordBlacklistPattern; }
    public Pattern drinkKeywordPattern() { return drinkKeywordPattern; }
    public Pattern soupKeywordPattern() { return soupKeywordPattern; }
    public Pattern fruitKeywordPattern() { return fruitKeywordPattern; }

    private void sanitize() {
        if (drinks == null) drinks = defaultDrinks();
        // Existing config files predate the waterskin, so merge its required built-in value once.
        drinks.putIfAbsent("thirstwastaken2:waterskin", new int[]{4, 5});
        // And the canteen and flask, which came later: a drink from them is the waterskin's drink.
        drinks.putIfAbsent("thirstwastaken2:copper_canteen", new int[]{4, 5});
        drinks.putIfAbsent("thirstwastaken2:iron_flask", new int[]{4, 5});
        // Same for milk and honey, added later still. A player who does not want them can set both
        // values to zero or list the item in itemBlacklist; only a missing key is filled in.
        drinks.putIfAbsent("minecraft:milk_bucket", new int[]{6, 8});
        drinks.putIfAbsent("minecraft:honey_bottle", new int[]{4, 6});
        // And for the Farmer's Delight drinks and meals the first lists missed.
        drinks.putIfAbsent("farmersdelight:milk_bottle", new int[]{6, 8});
        drinks.putIfAbsent("farmersdelight:hot_cocoa", new int[]{8, 13});
        if (foods == null) foods = defaultFoods();
        foods.putIfAbsent("farmersdelight:bone_broth", new int[]{5, 7});
        foods.putIfAbsent("farmersdelight:onion_soup", new int[]{4, 5});
        foods.putIfAbsent("farmersdelight:glow_berry_custard", new int[]{2, 3});
        foods.putIfAbsent("farmersdelight:tomato", new int[]{2, 3});
        // And for Kaleidoscope Cookery, added after both.
        kaleidoscopeCookeryDrinks(drinks);
        kaleidoscopeCookeryFoods(foods);
        // And for Brewin' and Chewin', added after that.
        brewinAndChewinDrinks(drinks);
        brewinAndChewinFoods(foods);
        // And for Cold Sweat's waterskin, added after that.
        coldSweatDrinks(drinks);
        // And for Cultural Delights, added after that.
        culturalDelightsDrinks(drinks);
        culturalDelightsFoods(foods);
        // And for Fruits Delight, added after that.
        fruitsDelightDrinks(drinks);
        fruitsDelightFoods(foods);
        // And for Ocean's Delight, added after that.
        oceansDelightFoods(foods);
        // And for Expanded Delight and Rustic Delight, added after that.
        expandedDelightDrinks(drinks);
        expandedDelightFoods(foods);
        rusticDelightDrinks(drinks);
        rusticDelightFoods(foods);
        // And for Farm & Charm and Candlelight, added after that.
        farmAndCharmDrinks(drinks);
        farmAndCharmFoods(foods);
        candlelightFoods(foods);
        // And for HerbalBrews and Beachparty, added after that.
        herbalBrewsDrinks(drinks);
        beachpartyDrinks(drinks);
        beachpartyFoods(foods);
        // And for Hearth and Harvest, added after that.
        hearthAndHarvestDrinks(drinks);
        hearthAndHarvestFoods(foods);
        // And for No Man's Land, added after that.
        noMansLandDrinks(drinks);
        noMansLandFoods(foods);
        // And for Vinery, added after that.
        vineryDrinks(drinks);
        vineryFoods(foods);
        // And for Croptopia, added after that.
        croptopiaDrinks(drinks);
        croptopiaFoods(foods);
        // And for Miner's Delight, added after that.
        minersDelightDrinks(drinks);
        minersDelightFoods(foods);
        clampValues(drinks);
        clampValues(foods);
        if (itemBlacklist == null) itemBlacklist = new LinkedHashSet<>();
        itemBlacklist.remove(null);
        sicknessEffects = sanitizeSickness(sicknessEffects);
        if (drinkTagValue == null || drinkTagValue.length != 2) drinkTagValue = new int[]{6, 8};
        if (keywordDrinkValue == null || keywordDrinkValue.length != 2) keywordDrinkValue = new int[]{10, 14};
        if (keywordSoupValue == null || keywordSoupValue.length != 2) keywordSoupValue = new int[]{4, 5};
        if (keywordFruitValue == null || keywordFruitValue.length != 2) keywordFruitValue = new int[]{2, 3};
        defaultPurity = clamp(defaultPurity, 0, 3);
        if (quenchedPercent == null || quenchedPercent.length != 4) quenchedPercent = new int[]{0, 50, 100, 100};
        for (int grade = 0; grade < quenchedPercent.length; grade++) {
            quenchedPercent[grade] = clamp(quenchedPercent[grade], 0, 100);
        }
        seaWaterNauseaSeconds = clamp(seaWaterNauseaSeconds, 0, MAX_EFFECT_SECONDS);
        seaWaterParchedSeconds = clamp(seaWaterParchedSeconds, 0, MAX_EFFECT_SECONDS);
        rainwaterPurity = clamp(rainwaterPurity, 0, 3);
        dripstonePurity = clamp(dripstonePurity, 0, 3);
        copperCanteenCapacity = clamp(copperCanteenCapacity, 1, WaterskinItem.MAX_CAPACITY);
        ironFlaskCapacity = clamp(ironFlaskCapacity, 1, WaterskinItem.MAX_CAPACITY);
        copperCanteenBoilSeconds = clamp(copperCanteenBoilSeconds, 1, MAX_BOIL_SECONDS);
        ironFlaskBoilSeconds = clamp(ironFlaskBoilSeconds, 1, MAX_BOIL_SECONDS);
        copperHangingPotBoilSeconds = clamp(copperHangingPotBoilSeconds, 1, MAX_BOIL_SECONDS);
        ironHangingPotBoilSeconds = clamp(ironHangingPotBoilSeconds, 1, MAX_BOIL_SECONDS);
        distillerServingSeconds = clamp(distillerServingSeconds, 1, MAX_BOIL_SECONDS);
        distillerTankServings = clamp(distillerTankServings, 3, MAX_DISTILLER_TANK);
        distillerSaltItem = distillerSaltItem == null ? "" : distillerSaltItem.trim();
        // Gson reads a name it does not know, including a hand typo, as null.
        if (appleskinQuenchedOverlay == null) appleskinQuenchedOverlay = QuenchedOverlay.DIAMOND;
        thirstDepletionModifier = clamp(thirstDepletionModifier, 0.0, 10.0);
        quenchedHealthRegen = clamp(quenchedHealthRegen, 0.0, 1.0);
        quenchedHealMinFood = clamp(quenchedHealMinFood, 0, 20);
        seasonDrainSpring = clamp(seasonDrainSpring, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);
        seasonDrainSummer = clamp(seasonDrainSummer, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);
        seasonDrainAutumn = clamp(seasonDrainAutumn, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);
        seasonDrainWinter = clamp(seasonDrainWinter, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);

        keywordBlacklistPattern = compile(keywordBlacklist);
        drinkKeywordPattern = compile(drinkKeywords);
        soupKeywordPattern = compile(soupKeywords);
        fruitKeywordPattern = compile(fruitKeywords);
    }

    private static Pattern compile(String pattern) {
        if (pattern == null || pattern.isBlank()) return null;
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        } catch (Exception exception) {
            ThirstWasTaken2.LOGGER.error("Invalid keyword pattern '{}', ignoring it", pattern, exception);
            return null;
        }
    }

    /**
     * Keeps every item value a pair inside the bar, as a data pack's already is: a hand-edited entry of
     * the wrong length is dropped, and one out of range is brought back into {@code 0..ThirstData.MAX}.
     * The config screen edits these maps too and never offers more than the bar holds.
     */
    private static void clampValues(Map<String, int[]> values) {
        values.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null || entry.getValue().length != 2);
        for (Map.Entry<String, int[]> entry : values.entrySet()) {
            int[] value = entry.getValue();
            value[0] = clamp(value[0], 0, ThirstData.MAX);
            value[1] = clamp(value[1], 0, ThirstData.MAX);
        }
    }

    /**
     * Rebuilds the sickness tables in their fixed order: a difficulty or grade missing from the
     * file gets its default lines, while one present and empty stays empty, since that is how a player
     * takes every effect off it. A line with no effect id is dropped and the numbers are clamped. An id
     * nothing registers is kept, for a mod that may be installed later, and skipped when drinking.
     */
    private static Map<String, Map<String, List<SicknessEffect>>> sanitizeSickness(
            Map<String, Map<String, List<SicknessEffect>>> tables) {
        Map<String, Map<String, List<SicknessEffect>>> clean = new LinkedHashMap<>();
        for (String difficulty : SicknessEffect.DIFFICULTIES) {
            Map<String, List<SicknessEffect>> grades = tables == null ? null : tables.get(difficulty);
            Map<String, List<SicknessEffect>> cleanGrades = new LinkedHashMap<>();
            for (String grade : SicknessEffect.GRADES) {
                List<SicknessEffect> lines = grades == null ? null : grades.get(grade);
                if (lines == null) {
                    cleanGrades.put(grade, SicknessEffect.defaults(difficulty, grade));
                    continue;
                }
                List<SicknessEffect> cleanLines = new ArrayList<>();
                for (SicknessEffect line : lines) {
                    if (line == null || line.effect == null || line.effect.isBlank()) continue;
                    cleanLines.add(new SicknessEffect(line.effect.trim().toLowerCase(Locale.ROOT),
                            clamp(line.chance, 0, 100), clamp(line.seconds, 1, SicknessEffect.MAX_SECONDS),
                            clamp(line.level, 1, SicknessEffect.MAX_LEVEL)));
                }
                cleanGrades.put(grade, cleanLines);
            }
            clean.put(difficulty, cleanGrades);
        }
        return clean;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static Map<String, int[]> defaultDrinks() {
        Map<String, int[]> values = new LinkedHashMap<>();
        put(values, 6, 8, "minecraft:potion");
        // Milk is as good as a bottle of water and never needs purifying, but it costs a bucket and
        // a cow. Honey is half the drink and lingers a little longer than its size suggests.
        put(values, 6, 8, "minecraft:milk_bucket");
        put(values, 4, 6, "minecraft:honey_bottle");
        put(values, 4, 5, "thirstwastaken2:terracotta_water_bowl");
        put(values, 4, 5, "thirstwastaken2:waterskin", "thirstwastaken2:copper_canteen", "thirstwastaken2:iron_flask");
        put(values, 8, 13, "farmersdelight:apple_cider", "farmersdelight:melon_juice", "farmersdelight:hot_cocoa");
        put(values, 6, 8, "farmersdelight:milk_bottle");
        kaleidoscopeCookeryDrinks(values);
        brewinAndChewinDrinks(values);
        coldSweatDrinks(values);
        culturalDelightsDrinks(values);
        fruitsDelightDrinks(values);
        expandedDelightDrinks(values);
        rusticDelightDrinks(values);
        farmAndCharmDrinks(values);
        herbalBrewsDrinks(values);
        beachpartyDrinks(values);
        hearthAndHarvestDrinks(values);
        noMansLandDrinks(values);
        vineryDrinks(values);
        croptopiaDrinks(values);
        minersDelightDrinks(values);
        return values;
    }

    private static Map<String, int[]> defaultFoods() {
        Map<String, int[]> values = new LinkedHashMap<>();
        put(values, 2, 3, "minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple");
        // Upstream gives the two stews an apple's 2, 3. Every other stew here, Farmer's Delight's and the
        // keyword value for soups included, is 4, 5, so these match them.
        put(values, 4, 5, "minecraft:mushroom_stew", "minecraft:rabbit_stew");
        put(values, 4, 5, "minecraft:melon_slice");
        put(values, 1, 2, "minecraft:carrot", "minecraft:beetroot", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:golden_carrot");
        put(values, 5, 7, "minecraft:beetroot_soup");
        put(values, 2, 1, "farmersdelight:pumpkin_slice");
        put(values, 2, 3, "farmersdelight:tomato", "farmersdelight:glow_berry_custard");
        put(values, 5, 7, "farmersdelight:bone_broth");
        put(values, 1, 2, "farmersdelight:cabbage_leaf");
        put(values, 7, 9, "farmersdelight:melon_popsicle");
        put(values, 6, 8, "farmersdelight:fruit_salad");
        put(values, 4, 5, "farmersdelight:tomato_sauce", "farmersdelight:mixed_salad", "farmersdelight:beef_stew", "farmersdelight:chicken_soup", "farmersdelight:vegetable_soup", "farmersdelight:fish_stew", "farmersdelight:pumpkin_soup", "farmersdelight:baked_cod_stew", "farmersdelight:noodle_soup", "farmersdelight:onion_soup");
        kaleidoscopeCookeryFoods(values);
        brewinAndChewinFoods(values);
        culturalDelightsFoods(values);
        fruitsDelightFoods(values);
        oceansDelightFoods(values);
        expandedDelightFoods(values);
        rusticDelightFoods(values);
        farmAndCharmFoods(values);
        candlelightFoods(values);
        beachpartyFoods(values);
        hearthAndHarvestFoods(values);
        noMansLandFoods(values);
        vineryFoods(values);
        croptopiaFoods(values);
        minersDelightFoods(values);
        return values;
    }

    /**
     * Kaleidoscope Cookery's teas and soups, by id alone: the official build and Refabricated share one
     * mod id, so these reach every node, one with no integration included, and an id a build lacks is
     * never matched. Listed because keyword matching is off by default, and when on, {@code tea} matches
     * {@code tea_egg}, a food, and misses {@code tieguanyin} and the other named teas.
     *
     * <p>A teacup is a cup of the four a bucket of water brews, a little above a bottle of water since
     * it costs a tea bag and heat. Tea is brewed from boiled water, so it is safe whatever went into the
     * teapot. Only what is eaten out of a bowl in hand is here: the pot soups are eaten off a placed
     * block, which is solid food and restores no thirst. Merged with {@code putIfAbsent}, so the same
     * call fills a fresh config and brings an older file up to date without overwriting a player's edit.
     */
    private static void kaleidoscopeCookeryDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 9, "kaleidoscope_cookery:barley_tea", "kaleidoscope_cookery:tieguanyin",
                "kaleidoscope_cookery:biluochun", "kaleidoscope_cookery:oolong", "kaleidoscope_cookery:sakura_fubuki",
                "kaleidoscope_cookery:flower_tea");
        putMissing(drinks, 6, 10, "kaleidoscope_cookery:butter_tea");
        // What a teapot brews from the wrong recipe.
        putMissing(drinks, 3, 3, "kaleidoscope_cookery:mystery_tea");
        putMissing(drinks, 8, 12, "kaleidoscope_cookery:clay_pot_milk_tea");
    }

    /** Kaleidoscope Cookery's soups and noodles; see {@link #kaleidoscopeCookeryDrinks}. */
    private static void kaleidoscopeCookeryFoods(Map<String, int[]> foods) {
        putMissing(foods, 5, 7, "kaleidoscope_cookery:pork_bone_soup");
        putMissing(foods, 4, 5, "kaleidoscope_cookery:seafood_miso_soup", "kaleidoscope_cookery:fearsome_thick_soup",
                "kaleidoscope_cookery:lamb_and_radish_soup", "kaleidoscope_cookery:wild_mushroom_rabbit_soup",
                "kaleidoscope_cookery:pufferfish_soup", "kaleidoscope_cookery:borscht",
                "kaleidoscope_cookery:beef_meatball_soup", "kaleidoscope_cookery:chicken_and_mushroom_stew",
                "kaleidoscope_cookery:laba_congee", "kaleidoscope_cookery:donkey_soup",
                "kaleidoscope_cookery:tomato_beef_brisket_soup");
        putMissing(foods, 3, 4, "kaleidoscope_cookery:beef_noodle", "kaleidoscope_cookery:hui_noodle",
                "kaleidoscope_cookery:udon_noodle");
        putMissing(foods, 2, 3, "kaleidoscope_cookery:tomato");
    }

    /**
     * Brewin' and Chewin's drinks, by id alone, as Kaleidoscope Cookery's are: they reach every node, one
     * with no integration included, and match nothing where the mod is absent. None of them is tagged
     * {@code c:drinks}, so without these they restore nothing.
     *
     * <p>A tankard or a bottle is one drink, a bottle of water's size. The stronger it is, the less it
     * restores: light brews a little under water, strong ones a third of it, and spirits nothing, so they
     * are left out, as are Salty Folly and Withering Dross, which no one drinks for their water. Brewed
     * drinks are safe whatever water went into the keg, as tea is.
     */
    private static void brewinAndChewinDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 8, "brewinandchewin:kombucha");
        putMissing(drinks, 5, 6, "brewinandchewin:beer", "brewinandchewin:mead", "brewinandchewin:egg_grog",
                "brewinandchewin:glittering_grenadine");
        putMissing(drinks, 4, 5, "brewinandchewin:bloody_mary");
        putMissing(drinks, 3, 4, "brewinandchewin:red_wine", "brewinandchewin:white_wine",
                "brewinandchewin:currant_wine", "brewinandchewin:verruca_wine", "brewinandchewin:twisted_wine",
                "brewinandchewin:rice_wine", "brewinandchewin:old_wine");
        putMissing(drinks, 3, 3, "brewinandchewin:pale_jane", "brewinandchewin:strongroot_ale",
                "brewinandchewin:dread_nog");
        putMissing(drinks, 2, 2, "brewinandchewin:saccharine_rum", "brewinandchewin:steel_toe_stout",
                "brewinandchewin:red_rum");
    }

    /** Brewin' and Chewin's soups and porridges eaten out of a bowl in hand; see {@link #brewinAndChewinDrinks}. */
    private static void brewinAndChewinFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "brewinandchewin:creamy_onion_soup");
        putMissing(foods, 2, 3, "brewinandchewin:fiery_fondue", "brewinandchewin:grits",
                "brewinandchewin:chopped_liver");
    }

    /**
     * Cold Sweat's filled waterskin, by id alone like the other mods' drinks. It holds 250 mB, a bottle,
     * and by default one sip empties it, so a sip is a bottle of water's worth. A server that gives it
     * more sips in Cold Sweat's config gets a bottle's worth from each.
     */
    private static void coldSweatDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 8, "cold_sweat:filled_waterskin");
    }

    /**
     * Cultural Delights' drinks, by id alone like the other mods': brewed in its vat since 0.18, a glass
     * bottle each, one drink per bottle. On Brewin' and Chewin's scale: the soft ones about a bottle of
     * water, light brews a little under, wine a third, a liqueur less. The spirits and the two that are
     * not drinks at all are listed as zero rather than left out: the mod tags its alcohol
     * {@code c:drinks/alcohol}, and a zero keeps the {@code c:drinks} tag value off them should a pack
     * fold that tag in. Brewed drinks are safe whatever water went into the vat, as tea is; sea water
     * brews nothing (see {@code src/main/culturaldelights}).
     */
    private static void culturalDelightsDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 8, "culturaldelights:cola");
        putMissing(drinks, 5, 6, "culturaldelights:ginger_beer", "culturaldelights:butterbeer",
                "culturaldelights:beer", "culturaldelights:mead", "culturaldelights:apple_cider");
        putMissing(drinks, 4, 5, "culturaldelights:bloody_mary", "culturaldelights:mojito",
                "culturaldelights:margarita");
        putMissing(drinks, 3, 4, "culturaldelights:wine", "culturaldelights:glow_wine");
        putMissing(drinks, 2, 2, "culturaldelights:lemon_liqueur");
        putMissing(drinks, 0, 0, "culturaldelights:tequila", "culturaldelights:gin", "culturaldelights:brandy",
                "culturaldelights:vodka", "culturaldelights:whiskey", "culturaldelights:rum",
                "culturaldelights:acid", "culturaldelights:vinegar");
    }

    /**
     * Cultural Delights' watery foods; see {@link #culturalDelightsDrinks}. A cucumber is most of a melon
     * slice, the salad and the soft corn and eggplant dishes are Farmer's Delight's. Pickles are salty and
     * the rest is dry, so neither is here.
     */
    private static void culturalDelightsFoods(Map<String, int[]> foods) {
        putMissing(foods, 3, 4, "culturaldelights:cucumber");
        putMissing(foods, 1, 2, "culturaldelights:cut_cucumber");
        putMissing(foods, 4, 5, "culturaldelights:hearty_salad");
        putMissing(foods, 2, 3, "culturaldelights:creamed_corn", "culturaldelights:poached_eggplants");
    }

    /**
     * Fruits Delight's juices and teas, by id alone like the other mods'. The mod ships thirst values
     * of its own, but only for the original Thirst Was Taken, whose mod id is not ours, so without these
     * its drinks restore nothing. The drinks keep its values, which are already Farmer's Delight's juice
     * value here; a juice is safe whatever water went into it, as tea is.
     */
    private static void fruitsDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 8, 13, "fruitsdelight:hamimelon_juice", "fruitsdelight:kiwi_juice",
                "fruitsdelight:orange_juice", "fruitsdelight:lemon_juice", "fruitsdelight:pear_juice",
                "fruitsdelight:hawberry_tea", "fruitsdelight:mango_tea", "fruitsdelight:peach_tea",
                "fruitsdelight:lychee_cherry_tea", "fruitsdelight:mangosteen_tea", "fruitsdelight:bayberry_soup");
        putMissing(drinks, 8, 12, "fruitsdelight:mango_milkshake");
        putMissing(drinks, 5, 6, "fruitsdelight:bellini_cocktail");
    }

    /**
     * Fruits Delight's watery foods; see {@link #fruitsDelightDrinks}. Its own values for food are two
     * to three times ours, so these follow the Farmer's Delight food each is closest to: a hamimelon
     * slice is a melon slice, a fruit an apple, a popsicle the melon popsicle, a stew a stew. Jam,
     * cookies, pies and the dry foods are left out.
     */
    private static void fruitsDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 8, 10, "fruitsdelight:hamimelon_shaved_ice");
        putMissing(foods, 7, 9, "fruitsdelight:hamimelon_popsicle", "fruitsdelight:kiwi_popsicle");
        putMissing(foods, 3, 4, "fruitsdelight:apple_jello", "fruitsdelight:bayberry_jello",
                "fruitsdelight:blueberry_jello", "fruitsdelight:chorus_jello", "fruitsdelight:cranberry_jello",
                "fruitsdelight:durian_jello", "fruitsdelight:fig_jello", "fruitsdelight:glowberry_jello",
                "fruitsdelight:hamimelon_jello", "fruitsdelight:hawberry_jello", "fruitsdelight:kiwi_jello",
                "fruitsdelight:lemon_jello", "fruitsdelight:lychee_jello", "fruitsdelight:mango_jello",
                "fruitsdelight:mangosteen_jello", "fruitsdelight:melon_jello", "fruitsdelight:orange_jello",
                "fruitsdelight:peach_jello", "fruitsdelight:pear_jello", "fruitsdelight:persimmon_jello",
                "fruitsdelight:pineapple_jello", "fruitsdelight:sweetberry_jello");
        putMissing(foods, 4, 5, "fruitsdelight:hamimelon_slice");
        putMissing(foods, 2, 3, "fruitsdelight:orange", "fruitsdelight:lychee", "fruitsdelight:pineapple_slice",
                "fruitsdelight:kiwi", "fruitsdelight:peach", "fruitsdelight:mango", "fruitsdelight:pear");
        putMissing(foods, 1, 2, "fruitsdelight:orange_slice", "fruitsdelight:lemon_slice", "fruitsdelight:baked_pear");
        putMissing(foods, 6, 8, "fruitsdelight:pear_with_rock_sugar");
        putMissing(foods, 4, 5, "fruitsdelight:fig_chicken_stew", "fruitsdelight:mango_salad");
        putMissing(foods, 2, 3, "fruitsdelight:blueberry_custard");
    }

    /**
     * Ocean's Delight's soup and bowls, by id alone: the mod has no drinks and uses no water, and its
     * Fabric and NeoForge builds share one mod id, so an id reaches every node and matches nothing where
     * the mod is absent. The Guardian Soup is a broth, the braised sea pickle a stew and the seagrass
     * salad a light bowl. The fried, baked and rolled seafood is left out.
     */
    private static void oceansDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 5, 7, "oceansdelight:bowl_of_guardian_soup");
        putMissing(foods, 4, 5, "oceansdelight:braised_sea_pickle");
        putMissing(foods, 2, 3, "oceansdelight:seagrass_salad");
    }

    /**
     * Expanded Delight's juices and goat milk, by id alone like the other mods'. None is tagged
     * {@code c:drinks}. The juices are Farmer's Delight's juice value, the goat milk its milk's. NeoForge
     * 1.21.1 is the mod's only build for a version this mod supports, so these match nothing elsewhere.
     */
    private static void expandedDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 8, 13, "expandeddelight:apple_juice", "expandeddelight:sweet_berry_juice",
                "expandeddelight:glow_berry_juice", "expandeddelight:cranberry_juice");
        putMissing(drinks, 6, 8, "expandeddelight:goat_milk_bottle", "expandeddelight:goat_milk_bucket");
    }

    /**
     * Expanded Delight's soups and salads; see {@link #expandedDelightDrinks}. Each is Farmer's Delight's
     * stew or mixed salad. Two of them cook from a water bucket, which takes no sea water (see
     * {@code src/main/expandeddelight}). The jellies are jam in a jar, and mac and cheese and the rest are
     * dry, so they are left out.
     */
    private static void expandedDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "expandeddelight:asparagus_soup", "expandeddelight:asparagus_soup_creamy",
                "expandeddelight:peanut_honey_soup", "expandeddelight:cinnamon_apples",
                "expandeddelight:peanut_salad", "expandeddelight:sweet_potato_salad",
                "expandeddelight:goat_cheese_beetroot_salad");
        putMissing(foods, 1, 2, "expandeddelight:cranberries");
    }

    /**
     * Rustic Delight's coffees and syrup, by id alone like the other mods'; its builds share one mod id
     * on every version, both loaders. The mod ships thirst values of its own, but only for the original
     * Thirst Was Taken, whose mod id is not ours. The drinks keep them: plain coffee a little under a
     * bottle of water, dark coffee less, the sweetened ones more. Its cooking oil is left out.
     */
    private static void rusticDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 5, 8, "rusticdelight:coffee");
        putMissing(drinks, 4, 6, "rusticdelight:dark_coffee");
        putMissing(drinks, 6, 10, "rusticdelight:milk_coffee");
        putMissing(drinks, 8, 11, "rusticdelight:syrup_coffee", "rusticdelight:chocolate_coffee",
                "rusticdelight:honey_coffee", "rusticdelight:pumpkin_coffee", "rusticdelight:cherry_blossom_coffee");
        putMissing(drinks, 2, 3, "rusticdelight:syrup");
    }

    /**
     * Rustic Delight's soups, sweet salad and raw bell peppers; see {@link #rusticDelightDrinks}. Its own
     * food values are close to ours, so these follow Farmer's Delight's: a soup is a stew, a pepper a
     * tomato, a slice a cabbage leaf. Roasted, rolled and stuffed peppers, the potato salad and the
     * batter are left out.
     */
    private static void rusticDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "rusticdelight:bell_pepper_soup", "rusticdelight:calamari_soup",
                "rusticdelight:sweet_salad");
        putMissing(foods, 2, 3, "rusticdelight:bell_pepper_green", "rusticdelight:bell_pepper_yellow",
                "rusticdelight:bell_pepper_red", "rusticdelight:bell_pepper_orange",
                "rusticdelight:bell_pepper_white", "rusticdelight:bell_pepper_pink",
                "rusticdelight:bell_pepper_blue", "rusticdelight:bell_pepper_purple",
                "rusticdelight:bell_pepper_black");
        putMissing(foods, 1, 2, "rusticdelight:bell_pepper_slice_green", "rusticdelight:bell_pepper_slice_yellow",
                "rusticdelight:bell_pepper_slice_red", "rusticdelight:bell_pepper_slice_orange",
                "rusticdelight:bell_pepper_slice_white", "rusticdelight:bell_pepper_slice_pink",
                "rusticdelight:bell_pepper_slice_blue", "rusticdelight:bell_pepper_slice_purple",
                "rusticdelight:bell_pepper_slice_black");
    }

    /**
     * Let's Do: Farm & Charm's herbal teas, by id alone, like the Farmer's Delight addons: reaches every
     * node and matches nothing where the mod is absent. The mod names no thirst mod and fills no
     * {@code c:drinks}. A cup is a cup of tea, as Kaleidoscope Cookery's; the jug drunk from the hand is
     * one serving too, though placed it pours two cups, so drinking it whole is the wasteful way. Tea is
     * boiled in the Cooking Pot, so it is safe whatever water went in.
     */
    private static void farmAndCharmDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 9, "farm_and_charm:strawberry_tea_cup", "farm_and_charm:nettle_tea_cup",
                "farm_and_charm:ribwort_tea_cup", "farm_and_charm:strawberry_tea", "farm_and_charm:nettle_tea",
                "farm_and_charm:ribwort_tea");
    }

    /**
     * Farm & Charm's soups, salad and the wetter crops; see {@link #farmAndCharmDrinks}. Soups and stews
     * follow Farmer's Delight's, the tomato Farmer's Delight's tomato, lettuce a cabbage leaf, a strawberry
     * a sweet berry. Corn grits are a porridge. Oatmeal is dry oats with no water in the recipe, and the
     * roasts, patties, breads and cakes are solid food; they are left out.
     */
    private static void farmAndCharmFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "farm_and_charm:barley_soup", "farm_and_charm:onion_soup",
                "farm_and_charm:potato_soup", "farm_and_charm:simple_tomato_soup", "farm_and_charm:goulash",
                "farm_and_charm:farmer_salad");
        putMissing(foods, 3, 4, "farm_and_charm:corn_grits");
        putMissing(foods, 2, 3, "farm_and_charm:tomato");
        putMissing(foods, 1, 2, "farm_and_charm:lettuce", "farm_and_charm:strawberry");
    }

    /**
     * Candlelight's wetter dishes. Candlelight is Farm & Charm's dining addon and has no drinks: its wine
     * glass is empty glassware. Soups and salads follow Farmer's Delight's, the mousse its glow berry
     * custard. Pasta, roasts, steaks, mozzarella and the dishes cooked in wine are solid food.
     */
    private static void candlelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "candlelight:tomato_soup", "candlelight:mushroom_soup", "candlelight:salad",
                "candlelight:beetroot_salad", "candlelight:fresh_garden_salad");
        putMissing(foods, 3, 4, "candlelight:tomato_mozzarella_salad");
        putMissing(foods, 2, 3, "candlelight:chocolate_mousse");
    }

    /**
     * Let's Do: HerbalBrews' teas and coffees, by id alone like Farm & Charm's. Each is brewed in the Tea
     * Kettle, which boils, so it is safe whatever fresh water went in; sea water brews nothing (see
     * {@code src/main/herbalbrews}). A tea is Farm & Charm's tea cup, the coffees Rustic Delight's. The
     * Flask, a mix of three potions, and the dried leaves are left out.
     */
    private static void herbalBrewsDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 9, "herbalbrews:green_tea", "herbalbrews:black_tea", "herbalbrews:lavender_tea",
                "herbalbrews:yerba_mate_tea", "herbalbrews:oolong_tea", "herbalbrews:rooibos_tea",
                "herbalbrews:hibiscus_tea");
        putMissing(drinks, 5, 8, "herbalbrews:coffee");
        putMissing(drinks, 6, 10, "herbalbrews:milk_coffee");
    }

    /**
     * Let's Do: Beachparty's cocktails: fruit and ice in a glass, a little under Farmer's Delight's juices.
     * Placed, a glass is sipped three times, each sip a third of this (see {@code src/main/beachparty}).
     */
    private static void beachpartyDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 7, 10, "beachparty:coconut_cocktail", "beachparty:sweetberries_cocktail",
                "beachparty:cocoa_cocktail", "beachparty:pumpkin_cocktail", "beachparty:honey_cocktail",
                "beachparty:melon_cocktail");
    }

    /**
     * Beachparty's opened coconut, half a coconut with its water in it, a melon slice's value. The whole
     * coconut is thrown rather than eaten, and the mussels are solid food.
     */
    private static void beachpartyFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "beachparty:coconut_open");
    }

    /**
     * Hearth and Harvest's drinks, by id alone; its 1.20.1 build has a subset of the same ids. Its own
     * Thirst compat names the upstream mod and never runs with this one, and its values are two to four
     * times ours for alcohol, so these follow Farmer's Delight's juices and milk and Brewin' and Chewin's
     * beer, wine and rum instead. Aged drinks are safe whatever fresh water went into the cask; sea water
     * ages nothing (see {@code src/main/hearthandharvest}).
     */
    private static void hearthAndHarvestDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 8, 13, "hearthandharvest:blueberry_juice", "hearthandharvest:cherry_juice",
                "hearthandharvest:raspberry_juice", "hearthandharvest:red_grape_juice",
                "hearthandharvest:green_grape_juice", "hearthandharvest:sweet_berry_juice",
                "hearthandharvest:glow_berry_juice");
        putMissing(drinks, 6, 8, "hearthandharvest:goat_milk_bottle", "hearthandharvest:chocolate_milk_bottle");
        putMissing(drinks, 5, 6, "hearthandharvest:mead", "hearthandharvest:hard_cider", "hearthandharvest:root_beer");
        putMissing(drinks, 3, 4, "hearthandharvest:blueberry_wine", "hearthandharvest:cherry_wine",
                "hearthandharvest:raspberry_wine", "hearthandharvest:red_grape_wine",
                "hearthandharvest:green_grape_wine", "hearthandharvest:sweet_berry_wine",
                "hearthandharvest:glow_berry_wine", "hearthandharvest:melon_wine");
        putMissing(drinks, 2, 2, "hearthandharvest:moonshine");
        putMissing(drinks, 2, 3, "hearthandharvest:syrup_bottle");
    }

    /**
     * Hearth and Harvest's stews and fruit: a stew as Farmer's Delight's, berries and grapes as sweet
     * berries, a baked or caramel apple as an apple. {@code onion_soup} is its 1.20.1 build's own. Jams,
     * pickles, cheese and the dry foods are left out.
     */
    private static void hearthAndHarvestFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "hearthandharvest:corn_stew", "hearthandharvest:onion_soup");
        putMissing(foods, 1, 2, "hearthandharvest:blueberries", "hearthandharvest:raspberry",
                "hearthandharvest:cherry", "hearthandharvest:red_grapes", "hearthandharvest:green_grapes");
        putMissing(foods, 2, 3, "hearthandharvest:baked_apple", "hearthandharvest:caramel_apple");
    }

    /**
     * No Man's Land's drinks, by id alone. It tags all three {@code c:drinks}, which would give each
     * {@link #drinkTagValue}; these entries win over the tag. The pear juice is Farmer's Delight's juices,
     * the maple syrup the honey bottle, and the pesto, a sauce of oil and basil drunk from its bottle, less
     * than Farmer's Delight's tomato sauce. The juice and the pesto exist only with Farmer's Delight.
     * Sipping from its milk cauldron is a quarter of a milk bucket (see {@code src/main/nomansland}).
     */
    private static void noMansLandDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 8, 13, "nomansland:pear_juice");
        putMissing(drinks, 4, 6, "nomansland:maple_syrup_bottle");
        putMissing(drinks, 1, 2, "nomansland:pesto_bottle");
    }

    /**
     * No Man's Land's stew, Farmer's Delight's stews, and its pears, the apple's. The witch stew exists only
     * with Farmer's Delight. Meats, fish, nuts, breads, pastas, tarts and cakes are solid food.
     */
    private static void noMansLandFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "nomansland:witch_stew");
        putMissing(foods, 2, 3, "nomansland:pear", "nomansland:syruped_pear", "nomansland:honeyed_apple");
    }

    /**
     * Let's Do: Vinery's juices and wines, by id alone like the other Let's Do mods. Vinery holds no water:
     * the Apple Press squeezes fruit and the Fermentation Barrel ferments juice, so nothing here has a
     * grade, and a placed bottle is only stored, never drunk. Juices are Farmer's Delight's, wines Brewin'
     * and Chewin's, the ciders and mead its beer and mead, as Hearth and Harvest's are. A big bottle and a
     * small one restore the same: Vinery's size is only its look. Vinery 1.5.4's juices drink on NeoForge
     * only; on Fabric its juice item never finishes being used, ours or not (see
     * {@code tools/agent/integrations/vinery.jsonl}).
     */
    private static void vineryDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 8, 13, "vinery:apple_juice", "vinery:red_grapejuice", "vinery:white_grapejuice",
                "vinery:red_savanna_grapejuice", "vinery:white_savanna_grapejuice", "vinery:red_taiga_grapejuice",
                "vinery:white_taiga_grapejuice", "vinery:red_jungle_grapejuice", "vinery:white_jungle_grapejuice");
        putMissing(drinks, 5, 6, "vinery:apple_cider", "vinery:kelp_cider", "vinery:mead");
        putMissing(drinks, 3, 4, "vinery:apple_wine", "vinery:glowing_wine", "vinery:solaris_wine",
                "vinery:eiswein", "vinery:aegis_wine", "vinery:villagers_fright", "vinery:clark_wine",
                "vinery:jellie_wine", "vinery:noir_wine", "vinery:red_wine", "vinery:strad_wine",
                "vinery:cherry_wine", "vinery:cristel_wine", "vinery:lilitu_wine", "vinery:jo_special_mixture",
                "vinery:bolvar_wine", "vinery:magnetic_wine", "vinery:stal_wine", "vinery:chenet_wine",
                "vinery:bottle_mojang_noir", "vinery:chorus_wine", "vinery:creepers_crush", "vinery:mellohi_wine");
    }

    /**
     * Vinery's fruit; see {@link #vineryDrinks}. Every grape is a sweet berry, whatever food Vinery gives
     * the jungle ones, the cherry a sweet berry too, apple mash an apple. The rotten cherry is left out.
     */
    private static void vineryFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 3, "vinery:apple_mash");
        putMissing(foods, 1, 2, "vinery:red_grape", "vinery:white_grape", "vinery:savanna_grapes_red",
                "vinery:savanna_grapes_white", "vinery:taiga_grapes_red", "vinery:taiga_grapes_white",
                "vinery:jungle_grapes_red", "vinery:jungle_grapes_white", "vinery:cherry");
    }

    /**
     * Croptopia's drinks, by id alone: its builds share one mod id on every version and loader, so these
     * reach every node, one with no integration included, and match nothing where the mod is absent. None
     * is tagged {@code c:drinks}. Juices and lemonade are Farmer's Delight's juice, smoothies Beachparty's
     * cocktails, the milkshake Fruits Delight's, coffee, the latte and tea HerbalBrews', soy milk and
     * horchata a bottle of milk, beer, mead and wine Brewin' and Chewin's, rum its saccharine rum. Tea is
     * crafted cold from a water bottle, but it is its own item and safe, as every other mod's is; sea water
     * crafts nothing (see {@code src/main/croptopia}). Croptopia's water bottle and milk bottle are
     * ingredients that cannot be drunk, so they are left out.
     */
    private static void croptopiaDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 8, 13, "croptopia:apple_juice", "croptopia:cranberry_juice", "croptopia:grape_juice",
                "croptopia:melon_juice", "croptopia:orange_juice", "croptopia:pineapple_juice",
                "croptopia:saguaro_juice", "croptopia:tomato_juice", "croptopia:lemonade", "croptopia:limeade");
        putMissing(drinks, 7, 10, "croptopia:banana_smoothie", "croptopia:strawberry_smoothie",
                "croptopia:fruit_smoothie", "croptopia:kale_smoothie");
        putMissing(drinks, 8, 12, "croptopia:chocolate_milkshake");
        putMissing(drinks, 6, 9, "croptopia:tea");
        putMissing(drinks, 5, 8, "croptopia:coffee");
        putMissing(drinks, 6, 10, "croptopia:pumpkin_spice_latte");
        putMissing(drinks, 6, 8, "croptopia:soy_milk", "croptopia:horchata");
        putMissing(drinks, 5, 6, "croptopia:beer", "croptopia:mead");
        putMissing(drinks, 3, 4, "croptopia:wine");
        putMissing(drinks, 2, 2, "croptopia:rum");
    }

    /**
     * Croptopia's soups, salads and wetter fruit; see {@link #croptopiaDrinks}. Soups and stews are
     * Farmer's Delight's, the fruit salad its fruit salad, the others its mixed salad, a sorbet its melon
     * popsicle, ice cream and yoghurt its custard. Cantaloupe and honeydew are a melon slice, the cucumber
     * Cultural Delights', the tomato Farmer's Delight's, the tree fruit an apple, berries, cherries,
     * kumquats, lemons and limes a sweet berry, lettuce and celery a cabbage leaf.
     * {@code bibim_naengmyeon}, a cold noodle soup, is in the 26.x builds
     * only. Pickles are salty, and the roasts, breads, pies and jams are solid food; they are left out.
     */
    private static void croptopiaFoods(Map<String, int[]> foods) {
        putMissing(foods, 4, 5, "croptopia:leek_soup", "croptopia:pumpkin_soup", "croptopia:potato_soup",
                "croptopia:beef_stew", "croptopia:nether_wart_stew", "croptopia:borscht", "croptopia:goulash",
                "croptopia:chicken_and_dumplings", "croptopia:tofu_and_dumplings", "croptopia:chicken_and_noodles",
                "croptopia:bibim_naengmyeon", "croptopia:cucumber_salad", "croptopia:caesar_salad",
                "croptopia:leafy_salad", "croptopia:veggie_salad", "croptopia:beetroot_salad");
        putMissing(foods, 6, 8, "croptopia:fruit_salad");
        putMissing(foods, 7, 9, "croptopia:kiwi_sorbet");
        putMissing(foods, 2, 3, "croptopia:vanilla_ice_cream", "croptopia:strawberry_ice_cream",
                "croptopia:mango_ice_cream", "croptopia:pecan_ice_cream", "croptopia:chocolate_ice_cream",
                "croptopia:rum_raisin_ice_cream", "croptopia:yoghurt");
        putMissing(foods, 4, 5, "croptopia:cantaloupe", "croptopia:honeydew");
        putMissing(foods, 3, 4, "croptopia:cucumber");
        putMissing(foods, 2, 3, "croptopia:tomato", "croptopia:orange", "croptopia:grapefruit", "croptopia:peach",
                "croptopia:pear", "croptopia:plum", "croptopia:nectarine", "croptopia:apricot", "croptopia:mango",
                "croptopia:pineapple", "croptopia:kiwi", "croptopia:starfruit", "croptopia:dragonfruit",
                "croptopia:persimmon");
        putMissing(foods, 1, 2, "croptopia:grape", "croptopia:strawberry", "croptopia:blueberry",
                "croptopia:blackberry", "croptopia:raspberry", "croptopia:cranberry", "croptopia:currant",
                "croptopia:elderberry", "croptopia:cherry", "croptopia:kumquat", "croptopia:lemon",
                "croptopia:lime", "croptopia:lettuce", "croptopia:celery");
    }

    private static void put(Map<String, int[]> values, int thirst, int quenched, String... ids) {
        for (String id : ids) values.put(id, new int[]{thirst, quenched});
    }

    /**
     * Miner's Delight's milk cup, by id alone like the other mods'. The mod is {@code minersdelight} on
     * NeoForge 1.21.1 and {@code miners_delight} on Forge 1.20.1, its only builds for a version this mod
     * supports, so every id is listed under both. The milk cup is Farmer's Delight's milk bottle. The
     * water cup is a water container with no value of its own, like the water bucket; see
     * {@code WaterPurity}.
     */
    private static void minersDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 8, "minersdelight:milk_cup", "miners_delight:milk_cup");
    }

    /**
     * Miner's Delight's soups; see {@link #minersDelightDrinks}. Its three bowls are Farmer's Delight's
     * stews. A cup is half a bowl, eaten fast, so each cup gets half its bowl's value, rounded up. The
     * cave foods, plates and sandwiches are dry and left out.
     */
    private static void minersDelightFoods(Map<String, int[]> foods) {
        for (String mod : new String[]{"minersdelight:", "miners_delight:"}) {
            putMissing(foods, 4, 5, mod + "cave_soup", mod + "bat_soup", mod + "insect_stew");
            putMissing(foods, 2, 3, mod + "beef_stew_cup", mod + "chicken_soup_cup", mod + "fish_stew_cup",
                    mod + "baked_cod_stew_cup", mod + "noodle_soup_cup", mod + "pumpkin_soup_cup",
                    mod + "vegetable_soup_cup", mod + "onion_soup_cup", mod + "mushroom_stew_cup",
                    mod + "rabbit_stew_cup", mod + "cave_soup_cup", mod + "bat_soup_cup", mod + "insect_stew_cup");
            putMissing(foods, 3, 4, mod + "bone_broth_cup", mod + "beetroot_soup_cup");
        }
    }

    private static void putMissing(Map<String, int[]> values, int thirst, int quenched, String... ids) {
        for (String id : ids) values.putIfAbsent(id, new int[]{thirst, quenched});
    }
}
