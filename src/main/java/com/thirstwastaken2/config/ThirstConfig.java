package com.thirstwastaken2.config;

import com.google.gson.JsonObject;
import com.thirstwastaken2.config.itemvalues.DefaultItemValues;
import com.thirstwastaken2.data.ThirstData;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loader-independent replacement for the original Forge config specs: every setting, as public fields
 * Gson maps the file onto, and {@link #sanitize()}, which clamps them.
 *
 * <p>Around it: {@link ConfigStore} keeps the active instance and the file, behind the static methods
 * here; {@link DefaultItemValues} holds what a fresh {@link #drinks} and {@link #foods} list, by mod
 * family; {@link SicknessEffect} the sickness tables; {@link KeywordPatterns} the compiled keyword lists.
 *
 * <p>The instance is swapped wholesale on {@link #load()}; {@link #generation()} increments on every
 * swap so derived caches (compiled patterns, per-item lookups) can invalidate themselves cheaply.
 */
public final class ThirstConfig {
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
    /**
     * Whether scooping water from the world takes the source block, as a bucket does. Auto does only
     * where the waterSourceConversion game rule is off. See {@code purity/SourceDrawing}.
     */
    public WaterSourceUse waterFillTakesSource = WaterSourceUse.AUTO;

    // ---- containers -------------------------------------------------------
    // Every count here is 1 to MAX_CONTAINER. Lowering one never takes water away: a vessel or pot that
    // holds more keeps it, can be drunk from or drawn, and takes nothing more until it has room.
    /** Off, the copper canteen and the iron flask no longer boil their water over a campfire. */
    public boolean enableBoilingInHand = true;
    /** Off, no furnace or smoker boils water: their water recipes are not loaded. Needs a reload. */
    public boolean enableFurnaceBoiling = true;
    /** Servings in a full waterskin, copper canteen, and iron, diamond and netherite flask. */
    public int waterskinCapacity = 4;
    public int copperCanteenCapacity = 4;
    public int ironFlaskCapacity = 6;
    public int diamondFlaskCapacity = 10;
    public int netheriteFlaskCapacity = 15;
    /**
     * Filled terracotta bowls in a stack, each still one serving. Read when the item is registered, so the
     * server and every client have to agree, and a change needs a restart. Water bottles do not stack:
     * a potion that stacks surprised other mods' brewing and storage code, so they stay vanilla's.
     */
    public int terracottaWaterBowlStackSize = 3;
    /** Seconds each serving takes to boil, held over a campfire or in a furnace, and in a pot. */
    public int copperCanteenBoilSeconds = 2;
    public int ironFlaskBoilSeconds = 3;
    public int diamondFlaskBoilSeconds = 2;
    public int netheriteFlaskBoilSeconds = 2;
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
    // Keywords are whole words of the id, so the blacklist spells out the plurals "s" and "es" do not make.
    public String keywordBlacklist = "dried|candied|leaf|leaves|gummy|crate|jam|sauce|bucket|seed|cookie|pie|bush|sapling|bean|curry|curries|cake|candy|candies|toast|sandwich";
    public String drinkKeywords = "drink|juice|tea|soda|coffee|wine|beer|cider|yogurt|milkshake|smoothie";
    public String soupKeywords = "soup|stew|porridge";
    public String fruitKeywords = "fruit|berry|berries|grape|orange|peach|pear|coconut|lemon|melon|cherry|apple";
    public int[] keywordDrinkValue = {6, 4};
    public int[] keywordSoupValue = {6, 4};
    public int[] keywordFruitValue = {2, 0};
    public Set<String> itemBlacklist = new LinkedHashSet<>();
    public Map<String, int[]> drinks = DefaultItemValues.drinks();
    public Map<String, int[]> foods = DefaultItemValues.foods();

    // ---- the mod's own items ----------------------------------------------
    // For a pack that brings its own. Off takes the item's recipes and its creative tab entry away; the
    // item stays registered, because registries must match between server and client and a world may
    // still hold one, and a stack that already exists keeps working. See isItemEnabled.
    /** The clay bowl, the terracotta bowl and the water bowl, which make no sense one without the others. */
    public boolean enableBowls = true;
    public boolean enableWaterskin = true;
    public boolean enableCopperCanteen = true;
    public boolean enableIronFlask = true;
    public boolean enableDiamondFlask = true;
    /** Off with the diamond flask on, the netherite one can no longer be upgraded from it. */
    public boolean enableNetheriteFlask = true;
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

    private transient KeywordPatterns keywordPatterns;

    public static ThirstConfig get() {
        return ConfigStore.get();
    }

    /** The config file on disk. */
    public static Path path() {
        return ConfigStore.path();
    }

    /** Bumped whenever {@link #load()} replaces the active instance. */
    public static int generation() {
        return ConfigStore.generation();
    }

    /** Reads the file, or makes a fresh config when there is none, makes it the active one and saves it. */
    public static ThirstConfig load() {
        return ConfigStore.load();
    }

    /**
     * A config read from the file's JSON, renamed keys carried over first: this edits {@code json} in
     * place. Sanitised, so ready to use. Public for the gametests.
     */
    public static ThirstConfig read(JsonObject json) {
        return ConfigStore.read(json);
    }

    public static void save() {
        ConfigStore.save();
    }

    /** Re-validates and persists after an in-place edit (used by the config screen). */
    public static void commit() {
        ConfigStore.commit();
    }

    /** A detached copy of the active config, for a screen that may throw its edits away. */
    public static ThirstConfig snapshot() {
        return ConfigStore.snapshot();
    }

    /**
     * Makes a {@link #snapshot()} the active config again, without saving: edits are only written by
     * {@link #commit()}, so the file on disk still holds what the snapshot does.
     */
    public static void restore(ThirstConfig snapshot) {
        ConfigStore.restore(snapshot);
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
            case "thirstwastaken2:diamond_flask" -> enableDiamondFlask;
            case "thirstwastaken2:netherite_flask" -> enableNetheriteFlask;
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

    public Pattern keywordBlacklistPattern() { return keywordPatterns.blacklist; }
    public Pattern drinkKeywordPattern() { return keywordPatterns.drink; }
    public Pattern soupKeywordPattern() { return keywordPatterns.soup; }
    public Pattern fruitKeywordPattern() { return keywordPatterns.fruit; }

    /** Clamps every setting into its range and fills in what the file left out. Every load and edit runs it. */
    void sanitize() {
        if (drinks == null) drinks = DefaultItemValues.drinks();
        if (foods == null) foods = DefaultItemValues.foods();
        DefaultItemValues.bringUpToDate(drinks, foods);
        clampValues(drinks);
        clampValues(foods);
        if (itemBlacklist == null) itemBlacklist = new LinkedHashSet<>();
        itemBlacklist.remove(null);
        sicknessEffects = SicknessEffect.sanitize(sicknessEffects);
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
        diamondFlaskCapacity = clamp(diamondFlaskCapacity, 1, MAX_CONTAINER);
        netheriteFlaskCapacity = clamp(netheriteFlaskCapacity, 1, MAX_CONTAINER);
        terracottaWaterBowlStackSize = clamp(terracottaWaterBowlStackSize, 1, MAX_CONTAINER);
        copperHangingPotCapacity = clamp(copperHangingPotCapacity, 1, MAX_CONTAINER);
        ironHangingPotCapacity = clamp(ironHangingPotCapacity, 1, MAX_CONTAINER);
        copperCanteenBoilSeconds = clamp(copperCanteenBoilSeconds, 1, MAX_BOIL_SECONDS);
        ironFlaskBoilSeconds = clamp(ironFlaskBoilSeconds, 1, MAX_BOIL_SECONDS);
        diamondFlaskBoilSeconds = clamp(diamondFlaskBoilSeconds, 1, MAX_BOIL_SECONDS);
        netheriteFlaskBoilSeconds = clamp(netheriteFlaskBoilSeconds, 1, MAX_BOIL_SECONDS);
        copperHangingPotBoilSeconds = clamp(copperHangingPotBoilSeconds, 1, MAX_BOIL_SECONDS);
        ironHangingPotBoilSeconds = clamp(ironHangingPotBoilSeconds, 1, MAX_BOIL_SECONDS);
        distillerServingSeconds = clamp(distillerServingSeconds, 1, MAX_BOIL_SECONDS);
        distillerTankServings = clamp(distillerTankServings, 1, MAX_CONTAINER);
        distillerSaltItem = distillerSaltItem == null ? "" : distillerSaltItem.trim();
        // Gson reads a name it does not know, including a hand typo, as null.
        if (appleskinQuenchedOverlay == null) appleskinQuenchedOverlay = QuenchedOverlay.DIAMOND;
        if (waterFillTakesSource == null) waterFillTakesSource = WaterSourceUse.AUTO;
        thirstDepletionModifier = clamp(thirstDepletionModifier, 0.0, 10.0);
        foodHealMinThirstPercent = clamp(foodHealMinThirstPercent, 0, 100);
        quenchedHealMinFoodPercent = clamp(quenchedHealMinFoodPercent, 0, 100);
        seasonDrainSpring = clamp(seasonDrainSpring, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);
        seasonDrainSummer = clamp(seasonDrainSummer, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);
        seasonDrainAutumn = clamp(seasonDrainAutumn, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);
        seasonDrainWinter = clamp(seasonDrainWinter, MIN_SEASON_DRAIN, MAX_SEASON_DRAIN);

        keywordPatterns = new KeywordPatterns(this);
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

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
