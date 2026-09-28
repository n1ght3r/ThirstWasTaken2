package com.thirstwastaken2.api;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.DataPackDrinks;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The public, loader-neutral API: what items restore, a player's thirst, and water purity. Callbacks
 * live in {@link ThirstEvents}. Everything in {@code com.thirstwastaken2.api} is public API and changes
 * only after a deprecation; everything outside it is internal. Documented for mod
 * authors on the site, under For Developers ({@code docs/docs/developers/}).
 *
 * <p>ThirstWasTaken2 is an optional dependency for any mod calling this, so guard every call with
 * "is {@code thirstwastaken2} loaded" and keep the calls in a class of their own.
 *
 * <p>Item resolution is per {@link Item} and is memoised, because the tooltip renderer asks for it once
 * per frame per hovered stack. The order is: the config's {@code itemBlacklist}, the config's
 * {@code drinks} and {@code foods}, data packs ({@code data/<namespace>/thirstwastaken2/drinks/}), the
 * {@code c:drinks} tag, then keywords.
 */
public final class ThirstApi {
    /**
     * Bumped on every addition to the API, so a mod can check for a method before calling it.
     * 2: player methods, water purity, {@link ThirstEvents}, data pack values. Releases up to 1.0.9.1
     * had only {@link #thirstValues} and {@link #restoresThirst}, and no such field at all.
     *
     * <p>Deliberately not a compile-time constant: javac copies a constant into the caller, which would
     * then read the version it was compiled against instead of the one installed.
     */
    public static final int API_VERSION = apiVersion();
    /** What {@link #purity} returns for anything that is not fresh water. */
    public static final int NOT_WATER = -1;

    private static final int[] NONE = new int[0];
    private static final TagKey<Item> DRINKS = conventionTag("drinks");
    private static final TagKey<Item> MAGIC_DRINKS = conventionTag("drinks/magic");
    private static final TagKey<Item> OMINOUS_DRINKS = conventionTag("drinks/ominous");
    private static final Map<Item, int[]> CACHE = new ConcurrentHashMap<>();
    private static volatile int cachedGeneration = -1;

    private ThirstApi() { }

    private static int apiVersion() {
        return 2;
    }

    /**
     * Forgets every resolved item. ThirstWasTaken2 calls it itself whenever tags or data pack values
     * change, and the config drops the cache on its own, so another mod never needs to; calling it is
     * harmless and only costs the next lookups their cache.
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * @return {thirst, quenched}, or {@code null} when the item restores no thirst. The array is shared:
     *     read it, never write to it.
     */
    public static int[] thirstValues(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (WaterskinItem.is(stack) && WaterskinItem.servings(stack) == 0) return null;
        return thirstValues(stack.getItem());
    }

    public static int[] thirstValues(Item item) {
        int generation = ThirstConfig.generation();
        if (generation != cachedGeneration) {
            CACHE.clear();
            cachedGeneration = generation;
        }
        int[] cached = CACHE.get(item);
        if (cached == null) cached = CACHE.computeIfAbsent(item, ThirstApi::resolve);
        return cached == NONE ? null : cached;
    }

    public static boolean restoresThirst(ItemStack stack) {
        return thirstValues(stack) != null;
    }

    // Players. Reads work on both sides, on the client for the local player only, since thirst is synced
    // to its owner alone. Writes happen on the server; on the client they do nothing.

    /** The most thirst, and quenched, a player can have. Use this rather than writing 20. */
    public static int maxThirst() {
        return ThirstData.MAX;
    }

    /** @return {@code 0..}{@link #maxThirst()} */
    public static int thirst(Player player) {
        return ThirstManager.get(player).thirst();
    }

    /** @return {@code 0..}{@link #thirst(Player)}: the hidden buffer spent before thirst. */
    public static int quenched(Player player) {
        return ThirstManager.get(player).quenched();
    }

    /**
     * Whether thirst applies to {@code player} right now: false in creative and spectator, and when an
     * operator turned it off for them with {@code /thirst enable}.
     */
    public static boolean isEnabled(Player player) {
        return ThirstManager.get(player).enabled() && !player.getAbilities().invulnerable;
    }

    /**
     * Restores thirst and quenched the way a drink does, capped the way a drink is. Negative amounts
     * count as 0. Does not fire {@link ThirstEvents#DRINK}. Server side only.
     */
    public static void drink(Player player, int thirst, int quenched) {
        ThirstManager.drink(player, Math.max(0, thirst), Math.max(0, quenched));
    }

    /**
     * Charges exhaustion at once, scaled by the same climate and armour modifier as the rest; 4 spends
     * one point of quenched, or of thirst once quenched is empty. Does nothing to a player thirst does
     * not apply to, or for an amount that is not positive. Server side only.
     */
    public static void addExhaustion(Player player, float amount) {
        if (amount > 0.0F && Float.isFinite(amount)) ThirstManager.addExhaustion(player, amount);
    }

    // Water purity. Grades run from 0, dirty, to 3, pure; sea water has no grade.

    /** @return the lowest grade, dirty water */
    public static int minPurity() {
        return WaterPurity.MIN;
    }

    /** @return the highest grade, pure water */
    public static int maxPurity() {
        return WaterPurity.MAX;
    }

    /**
     * Whether {@code stack} holds water ThirstWasTaken2 grades: a water bottle, a water bucket, a
     * filled waterskin, a water bowl. Sea water counts.
     */
    public static boolean isWaterContainer(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack);
    }

    /** Whether {@code stack} holds sea water, which never hydrates and cannot be purified. */
    public static boolean isSalt(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack) && WaterPurity.isSalty(stack);
    }

    /**
     * @return the grade of the fresh water in {@code stack}, {@link #minPurity()} to {@link #maxPurity()},
     *     or {@link #NOT_WATER} when it holds no water or holds sea water
     */
    public static int purity(ItemStack stack) {
        if (!WaterPurity.isWaterContainer(stack) || WaterPurity.isSalty(stack)) return NOT_WATER;
        return WaterPurity.get(stack);
    }

    /** A water bottle of {@code purity}, clamped to the grades, for a mod that hands out water. */
    public static ItemStack waterBottle(int purity) {
        return WaterPurity.set(Vanilla.waterBottle(),
                Math.max(WaterPurity.MIN, Math.min(WaterPurity.MAX, purity)));
    }

    private static int[] resolve(Item item) {
        Identifier identifier = Vanilla.itemId(item);
        String id = identifier.toString();
        ThirstConfig config = ThirstConfig.get();
        if (config.itemBlacklist.contains(id)) return NONE;

        int[] value = config.drinks.get(id);
        if (value == null) value = config.foods.get(id);
        if (value != null) return value;
        // After the config, so a server owner always has the last word, and before the tag, so a mod's
        // own value beats the flat one. An entry of nothing takes the item out of the fallbacks below.
        value = DataPackDrinks.get(item);
        if (value != null) return value[0] == 0 && value[1] == 0 ? NONE : value;
        if (config.enableDrinkTagMatching && isTaggedDrink(item)) return config.drinkTagValue;
        if (!config.enableKeywordMatching) return NONE;

        String path = identifier.getPath();
        if (matches(config.keywordBlacklistPattern(), path)) return NONE;
        if (matches(config.drinkKeywordPattern(), path)) return config.keywordDrinkValue;
        if (matches(config.soupKeywordPattern(), path)) return config.keywordSoupValue;
        if (matches(config.fruitKeywordPattern(), path)) return config.keywordFruitValue;
        return NONE;
    }

    /**
     * Whether the item's own mod calls it a drink, through the {@code c:drinks} convention tag. Magic
     * drinks are left out: the tag counts every potion and the ominous bottle among them, and neither
     * is water.
     */
    private static boolean isTaggedDrink(Item item) {
        ItemStack stack = new ItemStack(item);
        return stack.is(DRINKS) && !stack.is(MAGIC_DRINKS) && !stack.is(OMINOUS_DRINKS);
    }

    private static boolean matches(Pattern pattern, String path) {
        return pattern != null && pattern.matcher(path).find();
    }

    private static TagKey<Item> conventionTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
