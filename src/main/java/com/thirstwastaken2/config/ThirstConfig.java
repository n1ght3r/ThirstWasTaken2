package com.thirstwastaken2.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.data.ThirstData;
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
    /**
     * The most any container setting may be set to: servings in a vessel, pot or distiller tank, and
     * filled bottles or bowls in a stack. A pack option, not a balance target.
     */
    public static final int MAX_CONTAINER = 64;
    /** The fastest and slowest the mod's own vessels may be set to drink, in ticks. */
    public static final int MIN_DRINK_TICKS = 8;
    public static final int MAX_DRINK_TICKS = 100;
    /** The mod's own water containers, which restore {@link #plainWaterValue} rather than an entry of their own. */
    private static final String[] OWN_WATER_VESSELS = {"thirstwastaken2:terracotta_water_bowl",
            "thirstwastaken2:waterskin", "thirstwastaken2:copper_canteen", "thirstwastaken2:iron_flask"};
    /** The range of a season's drain factor, which is also the config screen's slider range. */
    public static final double MIN_SEASON_DRAIN = 0.25;
    public static final double MAX_SEASON_DRAIN = 4.0;

    // ---- thirst depletion -------------------------------------------------
    public double thirstDepletionModifier = 1.2;
    public boolean thirstDepletionInPeaceful = false;
    public boolean preventSprintingWhenThirsty = true;
    public boolean canDrinkByHand = true;
    /** On, food does not heal while the thirst bar is under {@link #foodHealMinThirstPercent}. */
    public boolean dehydrationHaltsHealthRegen = true;
    /** How full, in percent, the thirst bar has to be for food to heal. */
    public int foodHealMinThirstPercent = 50;
    // quenchedHealthRegen, quenched's healing speed, was dropped: quenched heals exactly as fast as
    // saturation. An old file's key is ignored and gone on the next save.
    /**
     * How full, in percent, the food bar has to be for quenched to heal. Quenched also needs a full
     * thirst bar, no saturation left, since saturation heals first, and no Upset Stomach.
     */
    public int quenchedHealMinFoodPercent = 50;
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

    // ---- water quality ----------------------------------------------------
    public int defaultQuality = 2;

    // ---- water balance ----------------------------------------------------
    // The numbers WaterPurity used to hold as constants. Each is read as a drink or a fill happens, never
    // cached, so a change applies at once.
    /**
     * {thirst, quenched} one serving of plain fresh water restores, from any container: a bottle, a
     * bowl, a waterskin, a canteen or a flask. The quenched is cut by the water's grade below.
     */
    public int[] plainWaterValue = {6, 4};
    /** How long the mod's own vessels and bowl take to drink, in ticks. A water bottle keeps vanilla's 32. */
    public int plainWaterDrinkTicks = 32;
    /** Percent of a drink's quenched that water of each grade gives, Dirty first. */
    public int[] quenchedPercent = {0, 25, 50, 100};
    /** Off, ocean and beach water is graded like any other water instead of being sea water. */
    public boolean enableSeaWater = true;
    public int seaWaterNauseaSeconds = 8;
    public int seaWaterParchedSeconds = 30;
    /** Off, rain fills no hanging pot, and rain in a cauldron is left ungraded, as vanilla leaves it. */
    public boolean enableRainCollection = true;
    public int rainwaterQuality = 2;
    public int dripstoneQuality = 3;

    // ---- containers -------------------------------------------------------
    // Every count here is 1 to MAX_CONTAINER. Lowering one never takes water away: a vessel or pot that
    // holds more keeps it, can be drunk from or drawn, and takes nothing more until it has room.
    /** Off, the copper canteen and the iron flask no longer boil their water over a campfire. */
    public boolean enableBoilingInHand = true;
    /** Off, no furnace or smoker boils water: their water recipes are not loaded. Needs a reload. */
    public boolean enableFurnaceBoiling = true;
    /** Servings in a full waterskin, copper canteen and iron flask. */
    public int waterskinCapacity = 4;
    public int copperCanteenCapacity = 4;
    public int ironFlaskCapacity = 6;
    /**
     * Filled terracotta bowls in a stack, each still one serving. Read when the item is registered, so the
     * server and every client have to agree, and a change needs a restart. Water bottles do not stack:
     * a potion that stacks surprised other mods' brewing and storage code, so they stay vanilla's.
     */
    public int terracottaWaterBowlStackSize = 3;
    /** Seconds each serving takes to boil, held over a campfire or in a furnace, and in a pot. */
    public int copperCanteenBoilSeconds = 2;
    public int ironFlaskBoilSeconds = 3;
    public int copperHangingPotBoilSeconds = 3;
    public int ironHangingPotBoilSeconds = 4;
    /** Servings in a full copper and iron hanging pot. */
    public int copperHangingPotCapacity = 3;
    public int ironHangingPotCapacity = 6;
    /** Seconds the copper distiller takes to distil a serving, while its fire burns. */
    public int distillerServingSeconds = 8;
    /** Servings each of the copper distiller's two tanks holds. One under three cannot take a bucket. */
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
     * A prepared drink's 6 and 4, like every listed juice and tea, not a potion's 6 and 8: a cheap juice
     * should not out-quench Clean water just for being tagged. The keyword values follow the same bands.
     */
    public boolean enableDrinkTagMatching = true;
    public int[] drinkTagValue = {6, 4};
    public boolean enableKeywordMatching = false;
    public String keywordBlacklist = "dried|candied|leaf|leaves|gummy|crate|jam|sauce|bucket|seed|cookie|pie|bush|sapling|bean|curry|cake|candy";
    public String drinkKeywords = "drink|juice|tea|soda|coffee|wine|beer|cider|yogurt|milkshake|smoothie";
    public String soupKeywords = "soup|stew|porridge";
    public String fruitKeywords = "fruit|berry|berries|grape|orange|peach|pear|coconut|lemon|melon|cherry|apple";
    public int[] keywordDrinkValue = {6, 4};
    public int[] keywordSoupValue = {6, 4};
    public int[] keywordFruitValue = {2, 0};
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
                JsonElement json = JsonParser.parseReader(reader);
                if (json.isJsonObject()) loaded = read(json.getAsJsonObject());
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

    /**
     * A config read from the file's JSON, renamed keys carried over first: {@link #migrate} edits
     * {@code json} in place. Sanitised, so ready to use. Public for the gametests.
     */
    public static ThirstConfig read(JsonObject json) {
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

    /**
     * Whether the switch named {@code setting} is on, for the {@code thirstwastaken2:config_enabled}
     * recipe condition: read as recipes load, so a change takes effect on the next data reload. True for
     * a name that is not a switch here, so a typo in a data pack loads the recipe rather than hiding it.
     */
    public boolean isSettingEnabled(String setting) {
        return switch (setting) {
            case "enableFurnaceBoiling" -> enableFurnaceBoiling;
            default -> true;
        };
    }

    public Pattern keywordBlacklistPattern() { return keywordBlacklistPattern; }
    public Pattern drinkKeywordPattern() { return drinkKeywordPattern; }
    public Pattern soupKeywordPattern() { return soupKeywordPattern; }
    public Pattern fruitKeywordPattern() { return fruitKeywordPattern; }

    private void sanitize() {
        if (drinks == null) drinks = defaultDrinks();
        // Every serving of plain water restores plainWaterValue, whatever holds it, so the mod's own
        // vessels and bowl no longer have a value of their own. Files from before the purification
        // rework list them; the entries would show in the item list and do nothing, so they go.
        for (String vessel : OWN_WATER_VESSELS) drinks.remove(vessel);
        // Milk and honey came after the first lists. A player who does not want them can set both
        // values to zero or list the item in itemBlacklist; only a missing key is filled in.
        drinks.putIfAbsent("minecraft:milk_bucket", new int[]{4, 0});
        drinks.putIfAbsent("minecraft:honey_bottle", new int[]{2, 0});
        // And for the Farmer's Delight drinks and meals the first lists missed.
        drinks.putIfAbsent("farmersdelight:milk_bottle", new int[]{4, 0});
        drinks.putIfAbsent("farmersdelight:hot_cocoa", new int[]{6, 4});
        if (foods == null) foods = defaultFoods();
        foods.putIfAbsent("farmersdelight:bone_broth", new int[]{6, 4});
        foods.putIfAbsent("farmersdelight:onion_soup", new int[]{6, 4});
        foods.putIfAbsent("farmersdelight:glow_berry_custard", new int[]{1, 0});
        foods.putIfAbsent("farmersdelight:tomato", new int[]{2, 0});
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
        // And for Kaleidoscope Flora, added after that.
        kaleidoscopeFloraDrinks(drinks);
        // And for Extra Delight, added after that.
        extraDelightDrinks(drinks);
        extraDelightFoods(foods);
        // And for Kaleidoscope Chinese Food, added after that.
        kaleidoscopeChineseFoodDrinks(drinks);
        kaleidoscopeChineseFoodFoods(foods);
        // And for Kaleidoscope Tavern, added after that.
        kaleidoscopeTavernDrinks(drinks);
        kaleidoscopeTavernFoods(foods);
        clampValues(drinks);
        clampValues(foods);
        if (itemBlacklist == null) itemBlacklist = new LinkedHashSet<>();
        itemBlacklist.remove(null);
        sicknessEffects = sanitizeSickness(sicknessEffects);
        if (drinkTagValue == null || drinkTagValue.length != 2) drinkTagValue = new int[]{6, 4};
        if (keywordDrinkValue == null || keywordDrinkValue.length != 2) keywordDrinkValue = new int[]{6, 4};
        if (keywordSoupValue == null || keywordSoupValue.length != 2) keywordSoupValue = new int[]{6, 4};
        if (keywordFruitValue == null || keywordFruitValue.length != 2) keywordFruitValue = new int[]{2, 0};
        defaultQuality = clamp(defaultQuality, 0, 3);
        if (quenchedPercent == null || quenchedPercent.length != 4) quenchedPercent = new int[]{0, 25, 50, 100};
        if (plainWaterValue == null || plainWaterValue.length != 2) plainWaterValue = new int[]{6, 4};
        plainWaterValue[0] = clamp(plainWaterValue[0], 0, ThirstData.MAX);
        plainWaterValue[1] = clamp(plainWaterValue[1], 0, ThirstData.MAX);
        plainWaterDrinkTicks = clamp(plainWaterDrinkTicks, MIN_DRINK_TICKS, MAX_DRINK_TICKS);
        for (int grade = 0; grade < quenchedPercent.length; grade++) {
            quenchedPercent[grade] = clamp(quenchedPercent[grade], 0, 100);
        }
        seaWaterNauseaSeconds = clamp(seaWaterNauseaSeconds, 0, MAX_EFFECT_SECONDS);
        seaWaterParchedSeconds = clamp(seaWaterParchedSeconds, 0, MAX_EFFECT_SECONDS);
        rainwaterQuality = clamp(rainwaterQuality, 0, 3);
        dripstoneQuality = clamp(dripstoneQuality, 0, 3);
        waterskinCapacity = clamp(waterskinCapacity, 1, MAX_CONTAINER);
        copperCanteenCapacity = clamp(copperCanteenCapacity, 1, MAX_CONTAINER);
        ironFlaskCapacity = clamp(ironFlaskCapacity, 1, MAX_CONTAINER);
        terracottaWaterBowlStackSize = clamp(terracottaWaterBowlStackSize, 1, MAX_CONTAINER);
        copperHangingPotCapacity = clamp(copperHangingPotCapacity, 1, MAX_CONTAINER);
        ironHangingPotCapacity = clamp(ironHangingPotCapacity, 1, MAX_CONTAINER);
        copperCanteenBoilSeconds = clamp(copperCanteenBoilSeconds, 1, MAX_BOIL_SECONDS);
        ironFlaskBoilSeconds = clamp(ironFlaskBoilSeconds, 1, MAX_BOIL_SECONDS);
        copperHangingPotBoilSeconds = clamp(copperHangingPotBoilSeconds, 1, MAX_BOIL_SECONDS);
        ironHangingPotBoilSeconds = clamp(ironHangingPotBoilSeconds, 1, MAX_BOIL_SECONDS);
        distillerServingSeconds = clamp(distillerServingSeconds, 1, MAX_BOIL_SECONDS);
        distillerTankServings = clamp(distillerTankServings, 1, MAX_CONTAINER);
        distillerSaltItem = distillerSaltItem == null ? "" : distillerSaltItem.trim();
        // Gson reads a name it does not know, including a hand typo, as null.
        if (appleskinQuenchedOverlay == null) appleskinQuenchedOverlay = QuenchedOverlay.DIAMOND;
        thirstDepletionModifier = clamp(thirstDepletionModifier, 0.0, 10.0);
        foodHealMinThirstPercent = clamp(foodHealMinThirstPercent, 0, 100);
        quenchedHealMinFoodPercent = clamp(quenchedHealMinFoodPercent, 0, 100);
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
                    String group = line.group == null || line.group.isBlank() ? null : line.group.trim();
                    cleanLines.add(new SicknessEffect(line.effect.trim().toLowerCase(Locale.ROOT),
                            clamp(line.chance, 0, 100), clamp(line.seconds, 1, SicknessEffect.MAX_SECONDS),
                            clamp(line.level, 1, SicknessEffect.MAX_LEVEL), group));
                }
                nestPoisonInUpsetStomach(cleanLines);
                cleanGrades.put(grade, cleanLines);
            }
            clean.put(difficulty, cleanGrades);
        }
        return clean;
    }

    /**
     * Holds a Poison line to no more than the chance of the Upset Stomach line it shares a roll with, so
     * a shared roll can never give Poison without Upset Stomach. Lines that roll on their own are left
     * as they are: a pack that splits them apart has asked for independent chances.
     */
    private static void nestPoisonInUpsetStomach(List<SicknessEffect> lines) {
        for (SicknessEffect poison : lines) {
            if (poison.group == null || !SicknessEffect.POISON.equals(poison.effect)) continue;
            for (SicknessEffect upset : lines) {
                if (poison.group.equals(upset.group) && SicknessEffect.UPSET_STOMACH.equals(upset.effect)) {
                    poison.chance = Math.min(poison.chance, upset.chance);
                }
            }
        }
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    /**
     * What a fresh config lists. Since the purification rework every value sits in one of a few bands,
     * so that nothing but prepared water and prepared drinks builds a reserve:
     *
     * <ul>
     *   <li>Prepared soups and non-alcoholic drinks, a juice, a tea, a coffee, a cocoa: 6 thirst and 4
     *       quenched, a serving of Clean water's worth, since they are made, not scooped. A cup is half.</li>
     *   <li>Plain milk: 4 and none. Honey and syrups: 2 and none.</li>
     *   <li>Alcohol keeps the thirst it had, the stronger the less, and half that as quenched; spirits
     *       nothing.</li>
     *   <li>Solid food is incidental water and builds no reserve: wet fruit and salads at most 2, a
     *       vegetable, berry, slice or dessert 1, dry food nothing.</li>
     * </ul>
     *
     * <p>Potions keep their own value. Plain water is not here at all: it is {@link #plainWaterValue}.
     * The per-mod notes below still say which of the mod's items each one is matched to.
     */
    private static Map<String, int[]> defaultDrinks() {
        Map<String, int[]> values = new LinkedHashMap<>();
        put(values, 6, 8, "minecraft:potion");
        // Milk never needs purifying, but it is not water: it fills without building a reserve.
        put(values, 4, 0, "minecraft:milk_bucket");
        put(values, 2, 0, "minecraft:honey_bottle");


        put(values, 6, 4, "farmersdelight:apple_cider", "farmersdelight:melon_juice", "farmersdelight:hot_cocoa");
        put(values, 4, 0, "farmersdelight:milk_bottle");
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
        kaleidoscopeFloraDrinks(values);
        extraDelightDrinks(values);
        kaleidoscopeChineseFoodDrinks(values);
        kaleidoscopeTavernDrinks(values);
        return values;
    }

    private static Map<String, int[]> defaultFoods() {
        Map<String, int[]> values = new LinkedHashMap<>();
        put(values, 2, 0, "minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple");
        // Upstream gives the two stews an apple's 2, 3. Every other stew here, Farmer's Delight's and the
        // keyword value for soups included, is 4, 5, so these match them.
        put(values, 6, 4, "minecraft:mushroom_stew", "minecraft:rabbit_stew");
        put(values, 2, 0, "minecraft:melon_slice");
        put(values, 1, 0, "minecraft:carrot", "minecraft:beetroot", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:golden_carrot");
        put(values, 6, 4, "minecraft:beetroot_soup");
        put(values, 1, 0, "farmersdelight:pumpkin_slice");
        put(values, 2, 0, "farmersdelight:tomato");
        put(values, 1, 0, "farmersdelight:glow_berry_custard");
        put(values, 6, 4, "farmersdelight:bone_broth");
        put(values, 1, 0, "farmersdelight:cabbage_leaf");
        put(values, 2, 0, "farmersdelight:melon_popsicle");
        put(values, 2, 0, "farmersdelight:fruit_salad");
        put(values, 1, 0, "farmersdelight:tomato_sauce");
        put(values, 2, 0, "farmersdelight:mixed_salad");
        put(values, 6, 4, "farmersdelight:beef_stew", "farmersdelight:chicken_soup", "farmersdelight:vegetable_soup", "farmersdelight:fish_stew", "farmersdelight:pumpkin_soup", "farmersdelight:baked_cod_stew", "farmersdelight:noodle_soup", "farmersdelight:onion_soup");
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
        extraDelightFoods(values);
        kaleidoscopeChineseFoodFoods(values);
        kaleidoscopeTavernFoods(values);
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
        putMissing(drinks, 6, 4, "kaleidoscope_cookery:barley_tea", "kaleidoscope_cookery:tieguanyin", "kaleidoscope_cookery:biluochun", "kaleidoscope_cookery:oolong", "kaleidoscope_cookery:sakura_fubuki", "kaleidoscope_cookery:flower_tea");
        putMissing(drinks, 6, 4, "kaleidoscope_cookery:butter_tea");
        // What a teapot brews from the wrong recipe.
        putMissing(drinks, 3, 1, "kaleidoscope_cookery:mystery_tea");
        putMissing(drinks, 6, 4, "kaleidoscope_cookery:clay_pot_milk_tea");
    }

    /** Kaleidoscope Cookery's soups and noodles; see {@link #kaleidoscopeCookeryDrinks}. */
    private static void kaleidoscopeCookeryFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "kaleidoscope_cookery:pork_bone_soup");
        putMissing(foods, 6, 4, "kaleidoscope_cookery:seafood_miso_soup", "kaleidoscope_cookery:fearsome_thick_soup", "kaleidoscope_cookery:lamb_and_radish_soup", "kaleidoscope_cookery:wild_mushroom_rabbit_soup", "kaleidoscope_cookery:pufferfish_soup", "kaleidoscope_cookery:borscht", "kaleidoscope_cookery:beef_meatball_soup", "kaleidoscope_cookery:chicken_and_mushroom_stew", "kaleidoscope_cookery:laba_congee", "kaleidoscope_cookery:donkey_soup", "kaleidoscope_cookery:tomato_beef_brisket_soup");
        putMissing(foods, 3, 2, "kaleidoscope_cookery:beef_noodle", "kaleidoscope_cookery:hui_noodle", "kaleidoscope_cookery:udon_noodle");
        putMissing(foods, 2, 0, "kaleidoscope_cookery:tomato");
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
        putMissing(drinks, 6, 4, "brewinandchewin:kombucha");
        putMissing(drinks, 5, 2, "brewinandchewin:beer", "brewinandchewin:mead", "brewinandchewin:egg_grog", "brewinandchewin:glittering_grenadine");
        putMissing(drinks, 4, 2, "brewinandchewin:bloody_mary");
        putMissing(drinks, 3, 1, "brewinandchewin:red_wine", "brewinandchewin:white_wine", "brewinandchewin:currant_wine", "brewinandchewin:verruca_wine", "brewinandchewin:twisted_wine", "brewinandchewin:rice_wine", "brewinandchewin:old_wine");
        putMissing(drinks, 3, 1, "brewinandchewin:pale_jane", "brewinandchewin:strongroot_ale", "brewinandchewin:dread_nog");
        putMissing(drinks, 2, 1, "brewinandchewin:saccharine_rum", "brewinandchewin:steel_toe_stout", "brewinandchewin:red_rum");
    }

    /** Brewin' and Chewin's soups and porridges eaten out of a bowl in hand; see {@link #brewinAndChewinDrinks}. */
    private static void brewinAndChewinFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "brewinandchewin:creamy_onion_soup");
        putMissing(foods, 0, 0, "brewinandchewin:fiery_fondue", "brewinandchewin:chopped_liver");
        putMissing(foods, 2, 0, "brewinandchewin:grits");
    }

    /**
     * Cold Sweat's filled waterskin, by id alone like the other mods' drinks. It holds 250 mB, a bottle,
     * and by default one sip empties it, so a sip is a bottle of water's worth. A server that gives it
     * more sips in Cold Sweat's config gets a bottle's worth from each.
     */
    private static void coldSweatDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "cold_sweat:filled_waterskin");
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
        putMissing(drinks, 6, 4, "culturaldelights:cola");
        putMissing(drinks, 6, 4, "culturaldelights:ginger_beer", "culturaldelights:butterbeer");
        putMissing(drinks, 5, 2, "culturaldelights:beer", "culturaldelights:mead", "culturaldelights:apple_cider");
        putMissing(drinks, 4, 2, "culturaldelights:bloody_mary", "culturaldelights:mojito", "culturaldelights:margarita");
        putMissing(drinks, 3, 1, "culturaldelights:wine", "culturaldelights:glow_wine");
        putMissing(drinks, 2, 1, "culturaldelights:lemon_liqueur");
        putMissing(drinks, 0, 0, "culturaldelights:tequila", "culturaldelights:gin", "culturaldelights:brandy", "culturaldelights:vodka", "culturaldelights:whiskey", "culturaldelights:rum", "culturaldelights:acid", "culturaldelights:vinegar");
    }

    /**
     * Cultural Delights' watery foods; see {@link #culturalDelightsDrinks}. A cucumber is most of a melon
     * slice, the salad and the soft corn and eggplant dishes are Farmer's Delight's. Pickles are salty and
     * the rest is dry, so neither is here.
     */
    private static void culturalDelightsFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "culturaldelights:cucumber");
        putMissing(foods, 1, 0, "culturaldelights:cut_cucumber");
        putMissing(foods, 2, 0, "culturaldelights:hearty_salad");
        putMissing(foods, 1, 0, "culturaldelights:creamed_corn", "culturaldelights:poached_eggplants");
    }

    /**
     * Fruits Delight's juices and teas, by id alone like the other mods'. The mod ships thirst values
     * of its own, but only for the original Thirst Was Taken, whose mod id is not ours, so without these
     * its drinks restore nothing. The drinks keep its values, which are already Farmer's Delight's juice
     * value here; a juice is safe whatever water went into it, as tea is.
     */
    private static void fruitsDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "fruitsdelight:hamimelon_juice", "fruitsdelight:kiwi_juice", "fruitsdelight:orange_juice", "fruitsdelight:lemon_juice", "fruitsdelight:pear_juice", "fruitsdelight:hawberry_tea", "fruitsdelight:mango_tea", "fruitsdelight:peach_tea", "fruitsdelight:lychee_cherry_tea", "fruitsdelight:mangosteen_tea", "fruitsdelight:bayberry_soup");
        putMissing(drinks, 6, 4, "fruitsdelight:mango_milkshake");
        putMissing(drinks, 5, 2, "fruitsdelight:bellini_cocktail");
    }

    /**
     * Fruits Delight's watery foods; see {@link #fruitsDelightDrinks}. Its own values for food are two
     * to three times ours, so these follow the Farmer's Delight food each is closest to: a hamimelon
     * slice is a melon slice, a fruit an apple, a popsicle the melon popsicle, a stew a stew. Jam,
     * cookies, pies and the dry foods are left out.
     */
    private static void fruitsDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "fruitsdelight:hamimelon_shaved_ice");
        putMissing(foods, 2, 0, "fruitsdelight:hamimelon_popsicle", "fruitsdelight:kiwi_popsicle");
        putMissing(foods, 2, 0, "fruitsdelight:apple_jello", "fruitsdelight:bayberry_jello", "fruitsdelight:blueberry_jello", "fruitsdelight:chorus_jello", "fruitsdelight:cranberry_jello", "fruitsdelight:durian_jello", "fruitsdelight:fig_jello", "fruitsdelight:glowberry_jello", "fruitsdelight:hamimelon_jello", "fruitsdelight:hawberry_jello", "fruitsdelight:kiwi_jello", "fruitsdelight:lemon_jello", "fruitsdelight:lychee_jello", "fruitsdelight:mango_jello", "fruitsdelight:mangosteen_jello", "fruitsdelight:melon_jello", "fruitsdelight:orange_jello", "fruitsdelight:peach_jello", "fruitsdelight:pear_jello", "fruitsdelight:persimmon_jello", "fruitsdelight:pineapple_jello", "fruitsdelight:sweetberry_jello");
        putMissing(foods, 2, 0, "fruitsdelight:hamimelon_slice");
        putMissing(foods, 2, 0, "fruitsdelight:orange", "fruitsdelight:lychee", "fruitsdelight:pineapple_slice", "fruitsdelight:kiwi", "fruitsdelight:peach", "fruitsdelight:mango", "fruitsdelight:pear");
        putMissing(foods, 1, 0, "fruitsdelight:orange_slice", "fruitsdelight:lemon_slice", "fruitsdelight:baked_pear");
        putMissing(foods, 6, 4, "fruitsdelight:pear_with_rock_sugar");
        putMissing(foods, 6, 4, "fruitsdelight:fig_chicken_stew");
        putMissing(foods, 2, 0, "fruitsdelight:mango_salad");
        putMissing(foods, 1, 0, "fruitsdelight:blueberry_custard");
    }

    /**
     * Ocean's Delight's soup and bowls, by id alone: the mod has no drinks and uses no water, and its
     * Fabric and NeoForge builds share one mod id, so an id reaches every node and matches nothing where
     * the mod is absent. The Guardian Soup is a broth, the braised sea pickle a stew and the seagrass
     * salad a light bowl. The fried, baked and rolled seafood is left out.
     */
    private static void oceansDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "oceansdelight:bowl_of_guardian_soup");
        putMissing(foods, 6, 4, "oceansdelight:braised_sea_pickle");
        putMissing(foods, 2, 0, "oceansdelight:seagrass_salad");
    }

    /**
     * Expanded Delight's juices and goat milk, by id alone like the other mods'. None is tagged
     * {@code c:drinks}. The juices are Farmer's Delight's juice value, the goat milk its milk's. NeoForge
     * 1.21.1 is the mod's only build for a version this mod supports, so these match nothing elsewhere.
     */
    private static void expandedDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "expandeddelight:apple_juice", "expandeddelight:sweet_berry_juice", "expandeddelight:glow_berry_juice", "expandeddelight:cranberry_juice");
        putMissing(drinks, 4, 0, "expandeddelight:goat_milk_bottle", "expandeddelight:goat_milk_bucket");
    }

    /**
     * Expanded Delight's soups and salads; see {@link #expandedDelightDrinks}. Each is Farmer's Delight's
     * stew or mixed salad. Two of them cook from a water bucket, which takes no sea water (see
     * {@code src/main/expandeddelight}). The jellies are jam in a jar, and mac and cheese and the rest are
     * dry, so they are left out.
     */
    private static void expandedDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "expandeddelight:asparagus_soup", "expandeddelight:asparagus_soup_creamy", "expandeddelight:peanut_honey_soup");
        putMissing(foods, 2, 0, "expandeddelight:cinnamon_apples", "expandeddelight:peanut_salad", "expandeddelight:sweet_potato_salad", "expandeddelight:goat_cheese_beetroot_salad");
        putMissing(foods, 1, 0, "expandeddelight:cranberries");
    }

    /**
     * Rustic Delight's coffees and syrup, by id alone like the other mods'; its builds share one mod id
     * on every version, both loaders. The mod ships thirst values of its own, but only for the original
     * Thirst Was Taken, whose mod id is not ours. The drinks keep them: plain coffee a little under a
     * bottle of water, dark coffee less, the sweetened ones more. Its cooking oil is left out.
     */
    private static void rusticDelightDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "rusticdelight:coffee");
        putMissing(drinks, 6, 4, "rusticdelight:dark_coffee");
        putMissing(drinks, 6, 4, "rusticdelight:milk_coffee");
        putMissing(drinks, 6, 4, "rusticdelight:syrup_coffee", "rusticdelight:chocolate_coffee", "rusticdelight:honey_coffee", "rusticdelight:pumpkin_coffee", "rusticdelight:cherry_blossom_coffee");
        putMissing(drinks, 2, 0, "rusticdelight:syrup");
    }

    /**
     * Rustic Delight's soups, sweet salad and raw bell peppers; see {@link #rusticDelightDrinks}. Its own
     * food values are close to ours, so these follow Farmer's Delight's: a soup is a stew, a pepper a
     * tomato, a slice a cabbage leaf. Roasted, rolled and stuffed peppers, the potato salad and the
     * batter are left out.
     */
    private static void rusticDelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "rusticdelight:bell_pepper_soup", "rusticdelight:calamari_soup");
        putMissing(foods, 2, 0, "rusticdelight:sweet_salad");
        putMissing(foods, 1, 0, "rusticdelight:bell_pepper_green", "rusticdelight:bell_pepper_yellow", "rusticdelight:bell_pepper_red", "rusticdelight:bell_pepper_orange", "rusticdelight:bell_pepper_white", "rusticdelight:bell_pepper_pink", "rusticdelight:bell_pepper_blue", "rusticdelight:bell_pepper_purple", "rusticdelight:bell_pepper_black");
        putMissing(foods, 1, 0, "rusticdelight:bell_pepper_slice_green", "rusticdelight:bell_pepper_slice_yellow", "rusticdelight:bell_pepper_slice_red", "rusticdelight:bell_pepper_slice_orange", "rusticdelight:bell_pepper_slice_white", "rusticdelight:bell_pepper_slice_pink", "rusticdelight:bell_pepper_slice_blue", "rusticdelight:bell_pepper_slice_purple", "rusticdelight:bell_pepper_slice_black");
    }

    /**
     * Let's Do: Farm & Charm's herbal teas, by id alone, like the Farmer's Delight addons: reaches every
     * node and matches nothing where the mod is absent. The mod names no thirst mod and fills no
     * {@code c:drinks}. A cup is a cup of tea, as Kaleidoscope Cookery's; the jug drunk from the hand is
     * one serving too, though placed it pours two cups, so drinking it whole is the wasteful way. Tea is
     * boiled in the Cooking Pot, so it is safe whatever water went in.
     */
    private static void farmAndCharmDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "farm_and_charm:strawberry_tea_cup", "farm_and_charm:nettle_tea_cup", "farm_and_charm:ribwort_tea_cup", "farm_and_charm:strawberry_tea", "farm_and_charm:nettle_tea", "farm_and_charm:ribwort_tea");
    }

    /**
     * Farm & Charm's soups, salad and the wetter crops; see {@link #farmAndCharmDrinks}. Soups and stews
     * follow Farmer's Delight's, the tomato Farmer's Delight's tomato, lettuce a cabbage leaf, a strawberry
     * a sweet berry. Corn grits are a porridge. Oatmeal is dry oats with no water in the recipe, and the
     * roasts, patties, breads and cakes are solid food; they are left out.
     */
    private static void farmAndCharmFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "farm_and_charm:barley_soup", "farm_and_charm:onion_soup", "farm_and_charm:potato_soup", "farm_and_charm:simple_tomato_soup", "farm_and_charm:goulash");
        putMissing(foods, 2, 0, "farm_and_charm:farmer_salad");
        putMissing(foods, 2, 0, "farm_and_charm:corn_grits");
        putMissing(foods, 2, 0, "farm_and_charm:tomato");
        putMissing(foods, 1, 0, "farm_and_charm:lettuce", "farm_and_charm:strawberry");
    }

    /**
     * Candlelight's wetter dishes. Candlelight is Farm & Charm's dining addon and has no drinks: its wine
     * glass is empty glassware. Soups and salads follow Farmer's Delight's, the mousse its glow berry
     * custard. Pasta, roasts, steaks, mozzarella and the dishes cooked in wine are solid food.
     */
    private static void candlelightFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "candlelight:tomato_soup", "candlelight:mushroom_soup");
        putMissing(foods, 2, 0, "candlelight:salad", "candlelight:beetroot_salad", "candlelight:fresh_garden_salad");
        putMissing(foods, 2, 0, "candlelight:tomato_mozzarella_salad");
        putMissing(foods, 1, 0, "candlelight:chocolate_mousse");
    }

    /**
     * Let's Do: HerbalBrews' teas and coffees, by id alone like Farm & Charm's. Each is brewed in the Tea
     * Kettle, which boils, so it is safe whatever fresh water went in; sea water brews nothing (see
     * {@code src/main/herbalbrews}). A tea is Farm & Charm's tea cup, the coffees Rustic Delight's. The
     * Flask, a mix of three potions, and the dried leaves are left out.
     */
    private static void herbalBrewsDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "herbalbrews:green_tea", "herbalbrews:black_tea", "herbalbrews:lavender_tea", "herbalbrews:yerba_mate_tea", "herbalbrews:oolong_tea", "herbalbrews:rooibos_tea", "herbalbrews:hibiscus_tea");
        putMissing(drinks, 6, 4, "herbalbrews:coffee");
        putMissing(drinks, 6, 4, "herbalbrews:milk_coffee");
    }

    /**
     * Let's Do: Beachparty's cocktails: fruit and ice in a glass, a little under Farmer's Delight's juices.
     * Placed, a glass is sipped three times, each sip a third of this (see {@code src/main/beachparty}).
     */
    private static void beachpartyDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "beachparty:coconut_cocktail", "beachparty:sweetberries_cocktail", "beachparty:cocoa_cocktail", "beachparty:pumpkin_cocktail", "beachparty:honey_cocktail", "beachparty:melon_cocktail");
    }

    /**
     * Beachparty's opened coconut, half a coconut with its water in it, a melon slice's value. The whole
     * coconut is thrown rather than eaten, and the mussels are solid food.
     */
    private static void beachpartyFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "beachparty:coconut_open");
    }

    /**
     * Hearth and Harvest's drinks, by id alone; its 1.20.1 build has a subset of the same ids. Its own
     * Thirst compat names the upstream mod and never runs with this one, and its values are two to four
     * times ours for alcohol, so these follow Farmer's Delight's juices and milk and Brewin' and Chewin's
     * beer, wine and rum instead. Aged drinks are safe whatever fresh water went into the cask; sea water
     * ages nothing (see {@code src/main/hearthandharvest}).
     */
    private static void hearthAndHarvestDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "hearthandharvest:blueberry_juice", "hearthandharvest:cherry_juice", "hearthandharvest:raspberry_juice", "hearthandharvest:red_grape_juice", "hearthandharvest:green_grape_juice", "hearthandharvest:sweet_berry_juice", "hearthandharvest:glow_berry_juice");
        putMissing(drinks, 4, 0, "hearthandharvest:goat_milk_bottle");
        putMissing(drinks, 6, 4, "hearthandharvest:chocolate_milk_bottle");
        putMissing(drinks, 5, 2, "hearthandharvest:mead", "hearthandharvest:hard_cider");
        putMissing(drinks, 6, 4, "hearthandharvest:root_beer");
        putMissing(drinks, 3, 1, "hearthandharvest:blueberry_wine", "hearthandharvest:cherry_wine", "hearthandharvest:raspberry_wine", "hearthandharvest:red_grape_wine", "hearthandharvest:green_grape_wine", "hearthandharvest:sweet_berry_wine", "hearthandharvest:glow_berry_wine", "hearthandharvest:melon_wine");
        putMissing(drinks, 2, 1, "hearthandharvest:moonshine");
        putMissing(drinks, 2, 0, "hearthandharvest:syrup_bottle");
    }

    /**
     * Hearth and Harvest's stews and fruit: a stew as Farmer's Delight's, berries and grapes as sweet
     * berries, a baked or caramel apple as an apple. {@code onion_soup} is its 1.20.1 build's own. Jams,
     * pickles, cheese and the dry foods are left out.
     */
    private static void hearthAndHarvestFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "hearthandharvest:corn_stew", "hearthandharvest:onion_soup");
        putMissing(foods, 1, 0, "hearthandharvest:blueberries", "hearthandharvest:raspberry", "hearthandharvest:cherry", "hearthandharvest:red_grapes", "hearthandharvest:green_grapes");
        putMissing(foods, 1, 0, "hearthandharvest:baked_apple", "hearthandharvest:caramel_apple");
    }

    /**
     * No Man's Land's drinks, by id alone. It tags all three {@code c:drinks}, which would give each
     * {@link #drinkTagValue}; these entries win over the tag. The pear juice is Farmer's Delight's juices,
     * the maple syrup the honey bottle, and the pesto, a sauce of oil and basil drunk from its bottle, less
     * than Farmer's Delight's tomato sauce. The juice and the pesto exist only with Farmer's Delight.
     * Sipping from its milk cauldron is a quarter of a milk bucket (see {@code src/main/nomansland}).
     */
    private static void noMansLandDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "nomansland:pear_juice");
        putMissing(drinks, 2, 0, "nomansland:maple_syrup_bottle");
        putMissing(drinks, 1, 0, "nomansland:pesto_bottle");
    }

    /**
     * No Man's Land's stew, Farmer's Delight's stews, and its pears, the apple's. The witch stew exists only
     * with Farmer's Delight. Meats, fish, nuts, breads, pastas, tarts and cakes are solid food.
     */
    private static void noMansLandFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "nomansland:witch_stew");
        putMissing(foods, 2, 0, "nomansland:pear", "nomansland:syruped_pear", "nomansland:honeyed_apple");
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
        putMissing(drinks, 6, 4, "vinery:apple_juice", "vinery:red_grapejuice", "vinery:white_grapejuice", "vinery:red_savanna_grapejuice", "vinery:white_savanna_grapejuice", "vinery:red_taiga_grapejuice", "vinery:white_taiga_grapejuice", "vinery:red_jungle_grapejuice", "vinery:white_jungle_grapejuice");
        putMissing(drinks, 5, 2, "vinery:apple_cider", "vinery:kelp_cider", "vinery:mead");
        putMissing(drinks, 3, 1, "vinery:apple_wine", "vinery:glowing_wine", "vinery:solaris_wine", "vinery:eiswein", "vinery:aegis_wine", "vinery:villagers_fright", "vinery:clark_wine", "vinery:jellie_wine", "vinery:noir_wine", "vinery:red_wine", "vinery:strad_wine", "vinery:cherry_wine", "vinery:cristel_wine", "vinery:lilitu_wine", "vinery:jo_special_mixture", "vinery:bolvar_wine", "vinery:magnetic_wine", "vinery:stal_wine", "vinery:chenet_wine", "vinery:bottle_mojang_noir", "vinery:chorus_wine", "vinery:creepers_crush", "vinery:mellohi_wine");
    }

    /**
     * Vinery's fruit; see {@link #vineryDrinks}. Every grape is a sweet berry, whatever food Vinery gives
     * the jungle ones, the cherry a sweet berry too, apple mash an apple. The rotten cherry is left out.
     */
    private static void vineryFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "vinery:apple_mash");
        putMissing(foods, 1, 0, "vinery:red_grape", "vinery:white_grape", "vinery:savanna_grapes_red", "vinery:savanna_grapes_white", "vinery:taiga_grapes_red", "vinery:taiga_grapes_white", "vinery:jungle_grapes_red", "vinery:jungle_grapes_white", "vinery:cherry");
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
        putMissing(drinks, 6, 4, "croptopia:apple_juice", "croptopia:cranberry_juice", "croptopia:grape_juice", "croptopia:melon_juice", "croptopia:orange_juice", "croptopia:pineapple_juice", "croptopia:saguaro_juice", "croptopia:tomato_juice", "croptopia:lemonade", "croptopia:limeade");
        putMissing(drinks, 6, 4, "croptopia:banana_smoothie", "croptopia:strawberry_smoothie", "croptopia:fruit_smoothie", "croptopia:kale_smoothie");
        putMissing(drinks, 6, 4, "croptopia:chocolate_milkshake");
        putMissing(drinks, 6, 4, "croptopia:tea");
        putMissing(drinks, 6, 4, "croptopia:coffee");
        putMissing(drinks, 6, 4, "croptopia:pumpkin_spice_latte");
        putMissing(drinks, 4, 0, "croptopia:soy_milk");
        putMissing(drinks, 6, 4, "croptopia:horchata");
        putMissing(drinks, 5, 2, "croptopia:beer", "croptopia:mead");
        putMissing(drinks, 3, 1, "croptopia:wine");
        putMissing(drinks, 2, 1, "croptopia:rum");
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
        putMissing(foods, 6, 4, "croptopia:leek_soup", "croptopia:pumpkin_soup", "croptopia:potato_soup", "croptopia:beef_stew", "croptopia:nether_wart_stew", "croptopia:borscht", "croptopia:goulash", "croptopia:chicken_and_dumplings", "croptopia:tofu_and_dumplings", "croptopia:chicken_and_noodles", "croptopia:bibim_naengmyeon");
        putMissing(foods, 2, 0, "croptopia:cucumber_salad", "croptopia:caesar_salad", "croptopia:leafy_salad", "croptopia:veggie_salad", "croptopia:beetroot_salad");
        putMissing(foods, 2, 0, "croptopia:fruit_salad");
        putMissing(foods, 2, 0, "croptopia:kiwi_sorbet");
        putMissing(foods, 1, 0, "croptopia:vanilla_ice_cream", "croptopia:strawberry_ice_cream", "croptopia:mango_ice_cream", "croptopia:pecan_ice_cream", "croptopia:chocolate_ice_cream", "croptopia:rum_raisin_ice_cream", "croptopia:yoghurt");
        putMissing(foods, 2, 0, "croptopia:cantaloupe", "croptopia:honeydew");
        putMissing(foods, 2, 0, "croptopia:cucumber");
        putMissing(foods, 2, 0, "croptopia:tomato", "croptopia:orange", "croptopia:grapefruit", "croptopia:peach", "croptopia:pear", "croptopia:plum", "croptopia:nectarine", "croptopia:apricot", "croptopia:mango", "croptopia:pineapple", "croptopia:kiwi", "croptopia:starfruit", "croptopia:dragonfruit", "croptopia:persimmon");
        putMissing(foods, 1, 0, "croptopia:grape", "croptopia:strawberry", "croptopia:blueberry", "croptopia:blackberry", "croptopia:raspberry", "croptopia:cranberry", "croptopia:currant", "croptopia:elderberry", "croptopia:cherry", "croptopia:kumquat", "croptopia:lemon", "croptopia:lime", "croptopia:lettuce", "croptopia:celery");
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
        putMissing(drinks, 4, 0, "minersdelight:milk_cup", "miners_delight:milk_cup");
    }

    /**
     * Miner's Delight's soups; see {@link #minersDelightDrinks}. Its three bowls are Farmer's Delight's
     * stews. A cup is half a bowl, eaten fast, so each cup gets half its bowl's value, rounded up. The
     * cave foods, plates and sandwiches are dry and left out.
     */
    private static void minersDelightFoods(Map<String, int[]> foods) {
        for (String mod : new String[]{"minersdelight:", "miners_delight:"}) {
            putMissing(foods, 6, 4, mod + "cave_soup", mod + "bat_soup", mod + "insect_stew");
            putMissing(foods, 3, 2, mod + "beef_stew_cup", mod + "chicken_soup_cup", mod + "fish_stew_cup",
                    mod + "baked_cod_stew_cup", mod + "noodle_soup_cup", mod + "pumpkin_soup_cup",
                    mod + "vegetable_soup_cup", mod + "onion_soup_cup", mod + "mushroom_stew_cup",
                    mod + "rabbit_stew_cup", mod + "cave_soup_cup", mod + "bat_soup_cup", mod + "insect_stew_cup");
            putMissing(foods, 3, 2, mod + "bone_broth_cup", mod + "beetroot_soup_cup");
        }
    }

    /**
     * Kaleidoscope Chinese Food's teas, by id alone like the other mods'. Its NeoForge 1.21.1 build and
     * its Forge 1.20.1 one (both 1.1.14) register the same ids, through Kaleidoscope Cookery's teacup
     * registry under their own namespace, so each is a teacup brewed in Cookery's teapot. Dianhong is
     * brewed from water and gets the flower tea's value, Hong Kong milk tea from milk and the milk tea's.
     */
    private static void kaleidoscopeChineseFoodDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "kaleidoscope_chinesefood:dianhong_tea");
        putMissing(drinks, 6, 4, "kaleidoscope_chinesefood:hk_milk_tea");
    }

    /**
     * Kaleidoscope Chinese Food's soups, porridges and noodles, cooked in Cookery's stockpot and eaten out
     * of a bowl in hand, on Cookery's own scale; see {@link #kaleidoscopeCookeryDrinks}. Maocai is cooked
     * in lava, as Cookery's fearsome thick soup is, and gets its value. Its feasts (the yellow croaker
     * soups, the Sichuan fish and pork pots, the four joy meatballs) are eaten off a placed block, and the
     * lamb pilaf and the wok dishes are dry, so none of them is here.
     */
    private static void kaleidoscopeChineseFoodFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "kaleidoscope_chinesefood:douzhi", "kaleidoscope_chinesefood:seaweed_egg_drop_soup", "kaleidoscope_chinesefood:tomato_egg_drop_soup", "kaleidoscope_chinesefood:century_egg_congee", "kaleidoscope_chinesefood:pumpkin_porridge", "kaleidoscope_chinesefood:yangrou_paomo", "kaleidoscope_chinesefood:maocai");
        putMissing(foods, 3, 2, "kaleidoscope_chinesefood:wonton_noodles", "kaleidoscope_chinesefood:sauerkraut_beef_noodles", "kaleidoscope_chinesefood:sichuan_wonton");
    }

    /**
     * Kaleidoscope Tavern's drinks, by id alone like the other mods': the official build and Refabricated
     * share one mod id, so these reach every node, and none of the drinks is tagged {@code c:drinks} or has
     * food properties, so without these they restore nothing. Keyword matching would take {@code wine} and
     * {@code juice} and miss the spirits and the cocktails.
     *
     * <p>A bottle or a glass is one drink, on Brewin' and Chewin's scale: wines and cocktails a third of a
     * bottle of water, mead a little under it, juice about a bottle. The four spirits are listed at zero
     * rather than left out, as Cultural Delights' are: a spirit is strong enough to take as much water as
     * it gives, and zero keeps a tag or a keyword from ever giving it one. That the barrel ferments them
     * from water earns them nothing: what comes out is a spirit. The mystery cocktail, a failed mix,
     * gets less than any other cocktail, and vinegar is not drunk for its water. A juice bucket gets a
     * bottle's value, as milk does in a bucket and a bottle alike. The brew level (one to five stars)
     * changes the mod's effects, not this value. Fermented drinks are safe whatever water went into the
     * barrel, as tea is.
     */
    private static void kaleidoscopeTavernDrinks(Map<String, int[]> drinks) {
        String mod = "kaleidoscope_tavern:";
        for (String wine : new String[]{"wine", "champagne", "carignan", "sakura_wine", "plum_wine", "ice_wine",
                "polaris_sweet_white", "red_queen", "riesling_dry_white", "sunset_glow", "madame_shexiang",
                "sweet_berry_wine", "sherry", "mother_snow", "luminous_bride", "glowflower_brew",
                "sauvignon_blanc_dry_white", "miners_star"}) {
            putMissing(drinks, 3, 1, mod + wine);
        }
        putMissing(drinks, 5, 2, mod + "honey_wine");
        putMissing(drinks, 0, 0, mod + "vodka", mod + "whiskey", mod + "rum", mod + "brandy");
        for (String cocktail : new String[]{"white_lady", "emerald", "brass_heart", "godfather", "grasshopper",
                "screwdriver", "mojito", "allium_garden", "depth_charge", "nether_special", "bloody_mary",
                "sculk_special", "signature_cocktail"}) {
            putMissing(drinks, 3, 1, mod + cocktail);
        }
        putMissing(drinks, 2, 0, mod + "mystery_cocktail");
        putMissing(drinks, 6, 4, mod + "watermelon_juice");
        putMissing(drinks, 0, 0, mod + "vinegar");
        putMissing(drinks, 6, 4, mod + "grape_bucket", mod + "ice_grape_bucket", mod + "gold_grape_bucket",
                mod + "green_grape_bucket", mod + "sweet_berries_bucket", mod + "glow_berries_bucket");
    }

    /**
     * Kaleidoscope Tavern's four grapes, eaten raw: a juicy fruit, as a tomato is, so they get its value;
     * see {@link #kaleidoscopeTavernDrinks}.
     */
    private static void kaleidoscopeTavernFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "kaleidoscope_tavern:grape", "kaleidoscope_tavern:ice_grape",
                "kaleidoscope_tavern:gold_grape", "kaleidoscope_tavern:green_grape");
    }

    /**
     * Kaleidoscope Flora's flower teas, by id alone like the other mods'. Its NeoForge 1.21.1 build and its
     * Forge 1.20.1 one (both 0.3.4) register the same ids. The addon registers them through
     * Kaleidoscope Cookery's teacup registry under its own namespace, so each is a teacup like Cookery's
     * {@code flower_tea}, brewed in the same teapot and drunk the same way, and gets its value. Hanami
     * Tale is brewed in milk and gets the milk tea's. The four drinks from Vanilla Backport's flowers
     * ({@code the_gaze}, {@code as_you_wish}, {@code springtime_stroll}, {@code fleeting_bloom}) exist only
     * with that mod, and an id that does not exist is never matched. The mooncake and the flower cakes are
     * dry and left out.
     */
    private static void kaleidoscopeFloraDrinks(Map<String, int[]> drinks) {
        for (String tea : new String[]{"when_the_wind_rises", "lullaby", "first_bloom", "fire_waltz",
                "the_unnoticed", "crimson_heartbeat", "autumn_serenade", "absolution", "rosy_stride",
                "loves_me_not", "prussian_leap", "may_kiss", "fleurs_du_mal", "breath_of_ancients", "the_sunward",
                "spring_waltz", "tender_thorns", "coronation", "voracious_urn", "echo_of_the_end",
                "vernal_awakening", "the_gaze", "as_you_wish", "springtime_stroll", "fleeting_bloom"}) {
            putMissing(drinks, 6, 4, "kaleidoscope_flora:" + tea);
        }
        putMissing(drinks, 6, 4, "kaleidoscope_flora:hanami_tale");
    }

    /**
     * Extra Delight's drinks, by id alone like the other mods'. Its only official build is NeoForge 1.21.1;
     * an id another build lacks is never matched. Ades, punch and the juices are Farmer's Delight's juice,
     * lemon and lime juice drunk neat half of one, milkshakes Fruits Delight's, gourmet hot chocolate
     * Farmer's Delight's hot cocoa, the milky drinks and ginger beer a bottle of milk, tea the other mods'
     * teas, coffee Croptopia's. Its Tough As Nails tags rank them the same way.
     */
    private static void extraDelightDrinks(Map<String, int[]> drinks) {
        for (String juice : new String[]{"lemonade", "limeade", "orangeade", "punch", "glow_berry_juice",
                "sweet_berry_juice", "tomato_juice", "cactus_juice", "orange_juice", "grapefruit_juice"}) {
            putMissing(drinks, 6, 4, "extradelight:" + juice);
        }
        putMissing(drinks, 4, 2, "extradelight:lemon_juice", "extradelight:lime_juice");
        for (String shake : new String[]{"milkshake", "chocolate_milkshake", "glow_berry_milkshake",
                "sweet_berry_milkshake", "pumpkin_milkshake", "honey_milkshake", "apple_milkshake",
                "cookie_dough_milkshake", "mint_chip_milkshake", "nut_butter_milkshake"}) {
            putMissing(drinks, 6, 4, "extradelight:" + shake);
        }
        putMissing(drinks, 6, 4, "extradelight:gourmet_hot_chocolate");
        putMissing(drinks, 6, 4, "extradelight:chocolate_milk", "extradelight:eggnog", "extradelight:horchata", "extradelight:xocolati", "extradelight:ginger_beer");
        putMissing(drinks, 4, 0, "extradelight:soy_milk");
        putMissing(drinks, 6, 4, "extradelight:tea");
        putMissing(drinks, 6, 4, "extradelight:coffee", "extradelight:dalgona_coffee");
    }

    /**
     * Extra Delight's soups, popsicles and cold desserts; see {@link #extraDelightDrinks}. A soup or stew
     * is every other mod's, curry and chili thicker, like noodles. Fruit and honey popsicles are Farmer's
     * Delight's melon popsicle, the creamier ones less; ice cream and custard Croptopia's ice cream. The
     * rice dishes, pies, puddings and jellies are solid food, and each feast serves one of these bowls.
     */
    private static void extraDelightFoods(Map<String, int[]> foods) {
        for (String soup : new String[]{"borscht", "cactus_soup", "carrot_soup", "corn_chowder", "fish_soup",
                "hazelnut_soup", "miso_soup", "mulligatawny_soup", "onion_soup", "oxtail_soup", "potato_soup",
                "sauerkraut_soup", "tomato_soup", "gazpacho", "congee", "chicken_stew", "lamb_stew", "pork_stew"}) {
            putMissing(foods, 6, 4, "extradelight:" + soup);
        }
        putMissing(foods, 6, 4, "extradelight:melon_gazpacho");
        putMissing(foods, 2, 0, "extradelight:curry", "extradelight:chili_con_carne", "extradelight:white_chili");
        putMissing(foods, 2, 0, "extradelight:apple_popsicle", "extradelight:glow_berry_popsicle", "extradelight:sweet_berry_popsicle", "extradelight:honey_popsicle");
        putMissing(foods, 2, 0, "extradelight:caramel_popsicle", "extradelight:cinnamon_popsicle", "extradelight:fudge_popsicle");
        for (String cold : new String[]{"ice_cream", "apple_ice_cream", "chocolate_ice_cream",
                "cookie_dough_ice_cream", "glow_berry_ice_cream", "honey_ice_cream", "mint_chip_ice_cream",
                "nut_butter_ice_cream", "pumpkin_ice_cream", "sweet_berry_ice_cream", "stuffed_apple_ice_cream",
                "ice_cream_sundae", "affogato", "apple_custard", "caramel_custard", "chocolate_custard",
                "honey_custard", "nut_butter_custard", "pumpkin_custard", "sweet_berry_custard"}) {
            putMissing(foods, 1, 0, "extradelight:" + cold);
        }
    }

    private static void putMissing(Map<String, int[]> values, int thirst, int quenched, String... ids) {
        for (String id : ids) values.putIfAbsent(id, new int[]{thirst, quenched});
    }
}
