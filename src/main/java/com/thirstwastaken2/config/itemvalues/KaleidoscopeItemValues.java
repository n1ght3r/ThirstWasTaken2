package com.thirstwastaken2.config.itemvalues;

import java.util.Map;

import static com.thirstwastaken2.config.itemvalues.DefaultItemValues.putMissing;

/**
 * What the drinks and foods of the Kaleidoscope mods: Cookery, Flora, Chinese Food and Tavern. restore by default. Every entry goes in with
 * {@code putIfAbsent}, so one call fills a fresh config and brings an older file up to date without
 * overwriting a player's edit. {@link DefaultItemValues} calls them, in the order that keeps a new
 * file's entries where they always were.
 */
final class KaleidoscopeItemValues {
    private KaleidoscopeItemValues() { }

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
    static void kaleidoscopeCookeryDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "kaleidoscope_cookery:barley_tea", "kaleidoscope_cookery:tieguanyin", "kaleidoscope_cookery:biluochun", "kaleidoscope_cookery:oolong", "kaleidoscope_cookery:sakura_fubuki", "kaleidoscope_cookery:flower_tea");
        putMissing(drinks, 6, 4, "kaleidoscope_cookery:butter_tea");
        // What a teapot brews from the wrong recipe.
        putMissing(drinks, 3, 1, "kaleidoscope_cookery:mystery_tea");
        putMissing(drinks, 6, 4, "kaleidoscope_cookery:clay_pot_milk_tea");
    }

    /** Kaleidoscope Cookery's soups and noodles; see {@link #kaleidoscopeCookeryDrinks}. */
    static void kaleidoscopeCookeryFoods(Map<String, int[]> foods) {
        putMissing(foods, 6, 4, "kaleidoscope_cookery:pork_bone_soup");
        putMissing(foods, 6, 4, "kaleidoscope_cookery:seafood_miso_soup", "kaleidoscope_cookery:fearsome_thick_soup", "kaleidoscope_cookery:lamb_and_radish_soup", "kaleidoscope_cookery:wild_mushroom_rabbit_soup", "kaleidoscope_cookery:pufferfish_soup", "kaleidoscope_cookery:borscht", "kaleidoscope_cookery:beef_meatball_soup", "kaleidoscope_cookery:chicken_and_mushroom_stew", "kaleidoscope_cookery:laba_congee", "kaleidoscope_cookery:donkey_soup", "kaleidoscope_cookery:tomato_beef_brisket_soup");
        putMissing(foods, 3, 2, "kaleidoscope_cookery:beef_noodle", "kaleidoscope_cookery:hui_noodle", "kaleidoscope_cookery:udon_noodle");
        putMissing(foods, 2, 0, "kaleidoscope_cookery:tomato");
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
    static void kaleidoscopeFloraDrinks(Map<String, int[]> drinks) {
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
     * Kaleidoscope Chinese Food's teas, by id alone like the other mods'. Its NeoForge 1.21.1 build and
     * its Forge 1.20.1 one (both 1.1.14) register the same ids, through Kaleidoscope Cookery's teacup
     * registry under their own namespace, so each is a teacup brewed in Cookery's teapot. Dianhong is
     * brewed from water and gets the flower tea's value, Hong Kong milk tea from milk and the milk tea's.
     */
    static void kaleidoscopeChineseFoodDrinks(Map<String, int[]> drinks) {
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
    static void kaleidoscopeChineseFoodFoods(Map<String, int[]> foods) {
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
    static void kaleidoscopeTavernDrinks(Map<String, int[]> drinks) {
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
    static void kaleidoscopeTavernFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "kaleidoscope_tavern:grape", "kaleidoscope_tavern:ice_grape",
                "kaleidoscope_tavern:gold_grape", "kaleidoscope_tavern:green_grape");
    }
}
