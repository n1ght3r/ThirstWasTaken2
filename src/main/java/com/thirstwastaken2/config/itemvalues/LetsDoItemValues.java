package com.thirstwastaken2.config.itemvalues;

import java.util.Map;

import static com.thirstwastaken2.config.itemvalues.DefaultItemValues.putMissing;

/**
 * What the drinks and foods of the Let's Do mods: Farm & Charm, Candlelight, HerbalBrews, Beachparty, Vinery and Meadow, restore by default. Every entry goes in with
 * {@code putIfAbsent}, so one call fills a fresh config and brings an older file up to date without
 * overwriting a player's edit. {@link DefaultItemValues} calls them, in the order that keeps a new
 * file's entries where they always were.
 */
final class LetsDoItemValues {
    private LetsDoItemValues() { }

    /**
     * Let's Do: Farm & Charm's herbal teas, by id alone, like the Farmer's Delight addons: reaches every
     * node and matches nothing where the mod is absent. The mod names no thirst mod and fills no
     * {@code c:drinks}. A cup is a cup of tea, as Kaleidoscope Cookery's; the jug drunk from the hand is
     * one serving too, though placed it pours two cups, so drinking it whole is the wasteful way. Tea is
     * boiled in the Cooking Pot, so it is safe whatever water went in.
     */
    static void farmAndCharmDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "farm_and_charm:strawberry_tea_cup", "farm_and_charm:nettle_tea_cup", "farm_and_charm:ribwort_tea_cup", "farm_and_charm:strawberry_tea", "farm_and_charm:nettle_tea", "farm_and_charm:ribwort_tea");
    }

    /**
     * Farm & Charm's soups, salad and the wetter crops; see {@link #farmAndCharmDrinks}. Soups and stews
     * follow Farmer's Delight's, the tomato Farmer's Delight's tomato, lettuce a cabbage leaf, a strawberry
     * a sweet berry. Corn grits are a porridge. Oatmeal is dry oats with no water in the recipe, and the
     * roasts, patties, breads and cakes are solid food; they are left out.
     */
    static void farmAndCharmFoods(Map<String, int[]> foods) {
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
    static void candlelightFoods(Map<String, int[]> foods) {
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
    static void herbalBrewsDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "herbalbrews:green_tea", "herbalbrews:black_tea", "herbalbrews:lavender_tea", "herbalbrews:yerba_mate_tea", "herbalbrews:oolong_tea", "herbalbrews:rooibos_tea", "herbalbrews:hibiscus_tea");
        putMissing(drinks, 6, 4, "herbalbrews:coffee");
        putMissing(drinks, 6, 4, "herbalbrews:milk_coffee");
    }

    /**
     * Let's Do: Beachparty's cocktails: fruit and ice in a glass, a little under Farmer's Delight's juices.
     * Placed, a glass is sipped three times, each sip a third of this (see {@code src/main/beachparty}).
     */
    static void beachpartyDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "beachparty:coconut_cocktail", "beachparty:sweetberries_cocktail", "beachparty:cocoa_cocktail", "beachparty:pumpkin_cocktail", "beachparty:honey_cocktail", "beachparty:melon_cocktail");
    }

    /**
     * Beachparty's opened coconut, half a coconut with its water in it, a melon slice's value. The whole
     * coconut is thrown rather than eaten, and the mussels are solid food.
     */
    static void beachpartyFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "beachparty:coconut_open");
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
    static void vineryDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 6, 4, "vinery:apple_juice", "vinery:red_grapejuice", "vinery:white_grapejuice", "vinery:red_savanna_grapejuice", "vinery:white_savanna_grapejuice", "vinery:red_taiga_grapejuice", "vinery:white_taiga_grapejuice", "vinery:red_jungle_grapejuice", "vinery:white_jungle_grapejuice");
        putMissing(drinks, 5, 2, "vinery:apple_cider", "vinery:kelp_cider", "vinery:mead");
        putMissing(drinks, 3, 1, "vinery:apple_wine", "vinery:glowing_wine", "vinery:solaris_wine", "vinery:eiswein", "vinery:aegis_wine", "vinery:villagers_fright", "vinery:clark_wine", "vinery:jellie_wine", "vinery:noir_wine", "vinery:red_wine", "vinery:strad_wine", "vinery:cherry_wine", "vinery:cristel_wine", "vinery:lilitu_wine", "vinery:jo_special_mixture", "vinery:bolvar_wine", "vinery:magnetic_wine", "vinery:stal_wine", "vinery:chenet_wine", "vinery:bottle_mojang_noir", "vinery:chorus_wine", "vinery:creepers_crush", "vinery:mellohi_wine");
    }

    /**
     * Vinery's fruit; see {@link #vineryDrinks}. Every grape is a sweet berry, whatever food Vinery gives
     * the jungle ones, the cherry a sweet berry too, apple mash an apple. The rotten cherry is left out.
     */
    static void vineryFoods(Map<String, int[]> foods) {
        putMissing(foods, 2, 0, "vinery:apple_mash");
        putMissing(foods, 1, 0, "vinery:red_grape", "vinery:white_grape", "vinery:savanna_grapes_red", "vinery:savanna_grapes_white", "vinery:taiga_grapes_red", "vinery:taiga_grapes_white", "vinery:jungle_grapes_red", "vinery:jungle_grapes_white", "vinery:cherry");
    }

    /**
     * Let's Do: Meadow's seven wooden milk buckets, drunk like vanilla's milk bucket and worth what it is.
     * Its cheeses, sandwiches and buffalo meat are solid food, and the soup and stew its Cooking Pot makes
     * are vanilla's, valued already. Its water is in its wooden bucket, wooden cauldron and Cooking Pot,
     * none of them a drink (see {@code src/main/meadow}).
     */
    static void meadowDrinks(Map<String, int[]> drinks) {
        putMissing(drinks, 4, 0, "meadow:wooden_milk_bucket", "meadow:wooden_sheep_milk_bucket", "meadow:wooden_buffalo_milk_bucket", "meadow:wooden_goat_milk_bucket", "meadow:wooden_warped_milk_bucket", "meadow:wooden_grain_milk_bucket", "meadow:wooden_amethyst_milk_bucket");
    }
}
