package com.thirstwastaken2.gametest;

import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.config.SicknessEffect;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;

/**
 * What an item restores, and the config that decides it: the drink and food tables, the blacklist,
 * the {@code c:drinks} tag, keyword matching, and the clamping that keeps a hand-edited config file
 * from breaking the game.
 *
 * <p>Expected values are read from the live config rather than written out, because the test server
 * keeps its config file between runs and a value is only wrong if the API disagrees with the config.
 */
public final class ThirstApiGameTest {
    private static final TagKey<Item> DRINKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "drinks"));

    @GameTest
    public void configuredDrinksAndFoodsRestoreWhatTheConfigSays(GameTestHelper helper) {
        ThirstConfig config = ThirstConfig.get();
        restores(helper, Items.POTION, config.drinks.get("minecraft:potion"));
        restores(helper, Items.MILK_BUCKET, config.drinks.get("minecraft:milk_bucket"));
        restores(helper, Items.APPLE, config.foods.get("minecraft:apple"));
        restores(helper, ThirstItems.TERRACOTTA_WATER_BOWL, config.drinks.get("thirstwastaken2:terracotta_water_bowl"));
        TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.STONE)) == null,
                "stone should restore nothing");
        helper.succeed();
    }

    @GameTest
    public void aWaterskinOnlyRestoresWhileItHoldsWater(GameTestHelper helper) {
        ItemStack skin = new ItemStack(ThirstItems.WATERSKIN);
        TestFixtures.check(helper, ThirstApi.thirstValues(skin) == null, "an empty waterskin should restore nothing");

        WaterskinItem.addWater(skin, WaterQuality.fresh(2), 1);
        TestFixtures.check(helper, Arrays.equals(ThirstApi.thirstValues(skin),
                        ThirstConfig.get().drinks.get("thirstwastaken2:waterskin")),
                "a filled waterskin should restore its configured value, got "
                        + Arrays.toString(ThirstApi.thirstValues(skin)));
        helper.succeed();
    }

    /** Also proves the per-item cache is dropped on commit, in both directions. */
    @GameTest
    public void blacklistedItemsRestoreNothing(GameTestHelper helper) {
        TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.APPLE)) != null,
                "an apple should restore something before it is blacklisted");

        TestFixtures.withConfig(config -> config.itemBlacklist.add("minecraft:apple"), () ->
                TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.APPLE)) == null,
                        "a blacklisted apple should restore nothing, got "
                                + Arrays.toString(ThirstApi.thirstValues(new ItemStack(Items.APPLE)))));

        TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.APPLE)) != null,
                "the apple should restore something again once the blacklist is restored");
        helper.succeed();
    }

    /**
     * The gametest mod tags a nautilus shell {@code c:drinks} in its own data, standing in for a drink
     * from a mod the config has never heard of. Vanilla has no such item: everything it tags is listed.
     */
    @GameTest
    public void itemsTaggedAsDrinksRestoreTheTagValue(GameTestHelper helper) {
        TestFixtures.check(helper, new ItemStack(Items.NAUTILUS_SHELL).is(DRINKS),
                "the gametest data pack should tag the nautilus shell c:drinks, or this test proves nothing");
        restores(helper, Items.NAUTILUS_SHELL, ThirstConfig.get().drinkTagValue);
        // Fabric API tags the ominous bottle c:drinks too, as a magic drink. It arrived with 1.20.5.
        //? if >=1.20.5 {
        TestFixtures.check(helper, new ItemStack(Items.OMINOUS_BOTTLE).is(DRINKS),
                "the ominous bottle should be tagged c:drinks, or its exclusion below proves nothing");
        TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.OMINOUS_BOTTLE)) == null,
                "a magic drink should restore nothing, got "
                        + Arrays.toString(ThirstApi.thirstValues(new ItemStack(Items.OMINOUS_BOTTLE))));
        //?}
        helper.succeed();
    }

    @GameTest
    public void theConfigAndBlacklistWinOverTheDrinkTag(GameTestHelper helper) {
        // Honey is tagged c:drinks/honey and listed at a different value, so the listed one must win.
        ThirstConfig config = ThirstConfig.get();
        TestFixtures.check(helper, !Arrays.equals(config.drinks.get("minecraft:honey_bottle"), config.drinkTagValue),
                "honey should be listed at a value other than the tag value, or this test proves nothing");
        restores(helper, Items.HONEY_BOTTLE, config.drinks.get("minecraft:honey_bottle"));

        TestFixtures.withConfig(edited -> edited.itemBlacklist.add("minecraft:nautilus_shell"), () ->
                TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.NAUTILUS_SHELL)) == null,
                        "a blacklisted item should restore nothing whatever its tags say"));
        helper.succeed();
    }

    @GameTest
    public void drinkTagMatchingCanBeTurnedOff(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.enableDrinkTagMatching = false, () ->
                TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.NAUTILUS_SHELL)) == null,
                        "with drink tag matching off, a tagged item nobody listed should restore nothing, got "
                                + Arrays.toString(ThirstApi.thirstValues(new ItemStack(Items.NAUTILUS_SHELL)))));
        TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.NAUTILUS_SHELL)) != null,
                "the tagged item should restore something again once matching is back on");
        helper.succeed();
    }

    @GameTest
    public void keywordMatchingOnlyAppliesWhenTurnedOn(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.enableKeywordMatching = false, () -> {
            TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.SUSPICIOUS_STEW)) == null,
                    "with keyword matching off, a stew nobody listed should restore nothing");
        });

        TestFixtures.withConfig(config -> config.enableKeywordMatching = true, () -> {
            ThirstConfig config = ThirstConfig.get();
            restores(helper, Items.SUSPICIOUS_STEW, config.keywordSoupValue);
            restores(helper, Items.CHORUS_FRUIT, config.keywordFruitValue);
            // "melon" matches the fruit keywords, and "seed" is on the keyword blacklist, which wins.
            TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.MELON_SEEDS)) == null,
                    "the keyword blacklist should win over a fruit keyword, got "
                            + Arrays.toString(ThirstApi.thirstValues(new ItemStack(Items.MELON_SEEDS))));
        });
        helper.succeed();
    }

    @GameTest
    public void aHandEditedConfigIsClampedBackIntoRange(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            config.defaultPurity = 99;
            config.thirstDepletionModifier = 50.0;
            config.sicknessEffects = null;
            config.drinks.remove("minecraft:milk_bucket");
            config.drinks.remove("farmersdelight:milk_bottle");
            config.foods.remove("farmersdelight:bone_broth");
            config.drinkTagValue = new int[] {3};
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            TestFixtures.check(helper, config.defaultPurity == 3, "default_purity should clamp to 3, got " + config.defaultPurity);
            TestFixtures.check(helper, config.thirstDepletionModifier == 10.0, "thirst_depletion_modifier should clamp to 10");
            TestFixtures.check(helper, config.sicknessEffects.equals(SicknessEffect.defaults()),
                    "missing sickness tables should fall back to the defaults, got " + config.sicknessEffects);
            TestFixtures.check(helper, config.drinks.containsKey("minecraft:milk_bucket"),
                    "a config file written before milk counted should have it merged back in");
            TestFixtures.check(helper, config.drinks.containsKey("farmersdelight:milk_bottle")
                            && config.foods.containsKey("farmersdelight:bone_broth"),
                    "a config file written before the added Farmer's Delight entries should have them merged back in");
            TestFixtures.check(helper, config.drinkTagValue.length == 2,
                    "a drink tag value of the wrong length should be reset, got " + Arrays.toString(config.drinkTagValue));
        });
        helper.succeed();
    }

    /**
     * Kaleidoscope Cookery is never installed here, so its items cannot be asked for: what is checked is
     * that the config names them, which is all that decides their value once the mod is there.
     */
    @GameTest
    public void kaleidoscopeCookeryTeasAndSoupsAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"barley_tea", "tieguanyin", "biluochun", "oolong", "sakura_fubuki", "flower_tea",
                "butter_tea", "mystery_tea", "clay_pot_milk_tea"};
        String[] foods = {"pork_bone_soup", "seafood_miso_soup", "borscht", "laba_congee", "donkey_soup",
                "beef_noodle", "udon_noodle", "tomato"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("kaleidoscope_cookery:" + drink),
                    "the default drinks should list kaleidoscope_cookery:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("kaleidoscope_cookery:" + food),
                    "the default foods should list kaleidoscope_cookery:" + food);
        }
        TestFixtures.check(helper, !defaults.foods.containsKey("kaleidoscope_cookery:tea_egg"),
                "a tea egg is food, not tea, and should not be listed");

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("kaleidoscope_cookery:" + drink);
            for (String food : foods) config.foods.remove("kaleidoscope_cookery:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("kaleidoscope_cookery:oolong", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                if (drink.equals("oolong")) continue;
                String id = "kaleidoscope_cookery:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "kaleidoscope_cookery:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("kaleidoscope_cookery:oolong"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("kaleidoscope_cookery:oolong")));
        });
        helper.succeed();
    }

    /** Brewin' and Chewin' is never installed here either; see the Kaleidoscope Cookery test above. */
    @GameTest
    public void brewinAndChewinDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"kombucha", "beer", "mead", "bloody_mary", "red_wine", "old_wine", "strongroot_ale",
                "red_rum"};
        String[] foods = {"creamy_onion_soup", "fiery_fondue", "grits"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("brewinandchewin:" + drink),
                    "the default drinks should list brewinandchewin:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("brewinandchewin:" + food),
                    "the default foods should list brewinandchewin:" + food);
        }
        for (String spirit : new String[]{"vodka", "brandy", "aqua_vitae", "salty_folly", "withering_dross"}) {
            TestFixtures.check(helper, !defaults.drinks.containsKey("brewinandchewin:" + spirit),
                    "brewinandchewin:" + spirit + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("brewinandchewin:" + drink);
            for (String food : foods) config.foods.remove("brewinandchewin:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("brewinandchewin:beer", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                if (drink.equals("beer")) continue;
                String id = "brewinandchewin:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "brewinandchewin:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("brewinandchewin:beer"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("brewinandchewin:beer")));
        });
        helper.succeed();
    }

    /** Cultural Delights is never installed here either; see the Kaleidoscope Cookery test above. */
    @GameTest
    public void culturalDelightsDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"cola", "beer", "apple_cider", "mojito", "wine", "lemon_liqueur", "vodka"};
        String[] foods = {"cucumber", "hearty_salad", "creamed_corn"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("culturaldelights:" + drink),
                    "the default drinks should list culturaldelights:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("culturaldelights:" + food),
                    "the default foods should list culturaldelights:" + food);
        }
        for (String spirit : new String[]{"vodka", "rum", "acid", "vinegar"}) {
            TestFixtures.check(helper, Arrays.equals(defaults.drinks.get("culturaldelights:" + spirit), new int[]{0, 0}),
                    "culturaldelights:" + spirit + " restores no thirst and should be listed as zero, so no tag gives it one");
        }
        TestFixtures.check(helper, !defaults.foods.containsKey("culturaldelights:pickle"),
                "culturaldelights:pickle is salty and should not be listed");

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("culturaldelights:" + drink);
            for (String food : foods) config.foods.remove("culturaldelights:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("culturaldelights:beer", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                if (drink.equals("beer")) continue;
                String id = "culturaldelights:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "culturaldelights:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("culturaldelights:beer"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("culturaldelights:beer")));
        });
        helper.succeed();
    }

    /** Fruits Delight is never installed here either; see the Kaleidoscope Cookery test above. */
    @GameTest
    public void fruitsDelightDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"orange_juice", "mango_tea", "bayberry_soup", "mango_milkshake", "bellini_cocktail"};
        String[] foods = {"hamimelon_slice", "kiwi_popsicle", "apple_jello", "pear", "fig_chicken_stew"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("fruitsdelight:" + drink),
                    "the default drinks should list fruitsdelight:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("fruitsdelight:" + food),
                    "the default foods should list fruitsdelight:" + food);
        }
        for (String dry : new String[]{"orange_jam", "lemon_cookie", "durian_flesh", "dried_persimmon"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey("fruitsdelight:" + dry)
                            && !defaults.drinks.containsKey("fruitsdelight:" + dry),
                    "fruitsdelight:" + dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("fruitsdelight:" + drink);
            for (String food : foods) config.foods.remove("fruitsdelight:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("fruitsdelight:orange_juice", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                if (drink.equals("orange_juice")) continue;
                String id = "fruitsdelight:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "fruitsdelight:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("fruitsdelight:orange_juice"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("fruitsdelight:orange_juice")));
        });
        helper.succeed();
    }

    /** Hearth and Harvest is never installed here either; see the Kaleidoscope Cookery test above. */
    @GameTest
    public void hearthAndHarvestDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"cherry_juice", "goat_milk_bottle", "mead", "red_grape_wine", "moonshine", "syrup_bottle"};
        String[] foods = {"corn_stew", "onion_soup", "red_grapes", "caramel_apple"};
        TestFixtures.check(helper, Arrays.equals(defaults.drinks.get("hearthandharvest:red_grape_wine"), new int[]{3, 4}),
                "a wine should restore what Brewin' and Chewin's do, not upstream's 10, 14");
        for (String dry : new String[]{"grape_jam", "pickled_carrots", "cheddar_cheese_slice", "sap_bucket", "macaroni_and_cheese"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey("hearthandharvest:" + dry)
                            && !defaults.drinks.containsKey("hearthandharvest:" + dry),
                    "hearthandharvest:" + dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("hearthandharvest:" + drink);
            for (String food : foods) config.foods.remove("hearthandharvest:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("hearthandharvest:mead", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                if (drink.equals("mead")) continue;
                String id = "hearthandharvest:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "hearthandharvest:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("hearthandharvest:mead"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("hearthandharvest:mead")));
        });
        helper.succeed();
    }

    /**
     * Farm & Charm and Candlelight are never installed here either; see the Kaleidoscope Cookery test
     * above. One test for both, since Candlelight is Farm & Charm's addon and merges in the same pass.
     */
    @GameTest
    public void farmAndCharmDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"farm_and_charm:nettle_tea_cup", "farm_and_charm:strawberry_tea"};
        String[] foods = {"farm_and_charm:barley_soup", "farm_and_charm:corn_grits", "farm_and_charm:tomato",
                "candlelight:tomato_soup", "candlelight:chocolate_mousse"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey(drink), "the default drinks should list " + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey(food), "the default foods should list " + food);
        }
        for (String dry : new String[]{"farm_and_charm:oatmeal_with_strawberries", "farm_and_charm:corn",
                "farm_and_charm:farmers_bread", "candlelight:mozzarella", "candlelight:lasagne",
                "candlelight:wine_glass"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey(dry) && !defaults.drinks.containsKey(dry),
                    dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove(drink);
            for (String food : foods) config.foods.remove(food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("farm_and_charm:nettle_tea_cup", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            String id = "farm_and_charm:strawberry_tea";
            TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                    id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                            + ", got " + Arrays.toString(config.drinks.get(id)));
            for (String food : foods) {
                TestFixtures.check(helper, Arrays.equals(config.foods.get(food), defaults.foods.get(food)),
                        food + " should be merged back as " + Arrays.toString(defaults.foods.get(food))
                                + ", got " + Arrays.toString(config.foods.get(food)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("farm_and_charm:nettle_tea_cup"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("farm_and_charm:nettle_tea_cup")));
        });
        helper.succeed();
    }

    /**
     * HerbalBrews and Beachparty, two more Let's Do mods, are never installed here either. One test for
     * both, since they were added in the same pass.
     */
    @GameTest
    public void herbalBrewsAndBeachpartyDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"herbalbrews:green_tea", "herbalbrews:coffee", "herbalbrews:milk_coffee",
                "beachparty:coconut_cocktail", "beachparty:melon_cocktail"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey(drink), "the default drinks should list " + drink);
        }
        TestFixtures.check(helper, defaults.foods.containsKey("beachparty:coconut_open"),
                "the default foods should list beachparty:coconut_open");
        for (String dry : new String[]{"herbalbrews:flask", "herbalbrews:dried_green_tea", "herbalbrews:coffee_beans",
                "beachparty:coconut", "beachparty:cooked_mussel_meat"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey(dry) && !defaults.drinks.containsKey(dry),
                    dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove(drink);
            config.foods.remove("beachparty:coconut_open");
            // A player's own value, which merging must leave alone.
            config.drinks.put("herbalbrews:green_tea", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String id : new String[]{"herbalbrews:coffee", "herbalbrews:milk_coffee",
                    "beachparty:coconut_cocktail", "beachparty:melon_cocktail"}) {
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            String food = "beachparty:coconut_open";
            TestFixtures.check(helper, Arrays.equals(config.foods.get(food), defaults.foods.get(food)),
                    food + " should be merged back as " + Arrays.toString(defaults.foods.get(food))
                            + ", got " + Arrays.toString(config.foods.get(food)));
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("herbalbrews:green_tea"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("herbalbrews:green_tea")));
        });
        helper.succeed();
    }

    /**
     * No Man's Land is never installed here either. Its own {@code c:drinks} tag would give the syrup and
     * the pesto the tag's value; the explicit entries are what take them off it.
     */
    @GameTest
    public void noMansLandDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"pear_juice", "maple_syrup_bottle", "pesto_bottle"};
        String[] foods = {"witch_stew", "pear", "syruped_pear", "honeyed_apple"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("nomansland:" + drink),
                    "the default drinks should list nomansland:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("nomansland:" + food),
                    "the default foods should list nomansland:" + food);
        }
        TestFixtures.check(helper, Arrays.equals(defaults.drinks.get("nomansland:pesto_bottle"), new int[]{1, 2}),
                "the pesto should restore 1, 2, got " + Arrays.toString(defaults.drinks.get("nomansland:pesto_bottle")));
        for (String dry : new String[]{"resin_oil_bottle", "awkward_residue", "trail_mix", "pear_cobbler_slice"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey("nomansland:" + dry)
                            && !defaults.drinks.containsKey("nomansland:" + dry),
                    "nomansland:" + dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("nomansland:" + drink);
            for (String food : foods) config.foods.remove("nomansland:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("nomansland:maple_syrup_bottle", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : new String[]{"pear_juice", "pesto_bottle"}) {
                String id = "nomansland:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "nomansland:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("nomansland:maple_syrup_bottle"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("nomansland:maple_syrup_bottle")));
        });
        helper.succeed();
    }

    /**
     * Vinery, another Let's Do mod, is never installed here either. Alcohol restores less than juice, and
     * the wine bottle and the rotten cherry are not listed.
     */
    @GameTest
    public void vineryDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"red_grapejuice", "apple_juice", "apple_cider", "red_wine", "chorus_wine"};
        String[] foods = {"red_grape", "jungle_grapes_white", "cherry", "apple_mash"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("vinery:" + drink),
                    "the default drinks should list vinery:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("vinery:" + food),
                    "the default foods should list vinery:" + food);
        }
        TestFixtures.check(helper, defaults.drinks.get("vinery:red_wine")[0] < defaults.drinks.get("vinery:red_grapejuice")[0],
                "wine should restore less than the juice it is made from");
        for (String dry : new String[]{"wine_bottle", "rotten_cherry", "red_grape_seeds"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey("vinery:" + dry)
                            && !defaults.drinks.containsKey("vinery:" + dry),
                    "vinery:" + dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("vinery:" + drink);
            for (String food : foods) config.foods.remove("vinery:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("vinery:mead", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                String id = "vinery:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "vinery:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("vinery:mead"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("vinery:mead")));
        });
        helper.succeed();
    }

    /** Ocean's Delight is never installed here either; see the Kaleidoscope Cookery test above. */
    @GameTest
    public void oceansDelightFoodsAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] foods = {"bowl_of_guardian_soup", "braised_sea_pickle", "seagrass_salad"};
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("oceansdelight:" + food),
                    "the default foods should list oceansdelight:" + food);
        }
        for (String dry : new String[]{"squid_rings", "fugu_roll", "honey_fried_kelp"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey("oceansdelight:" + dry)
                            && !defaults.drinks.containsKey("oceansdelight:" + dry),
                    "oceansdelight:" + dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String food : foods) config.foods.remove("oceansdelight:" + food);
            // A player's own value, which merging must leave alone.
            config.foods.put("oceansdelight:seagrass_salad", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String food : foods) {
                if (food.equals("seagrass_salad")) continue;
                String id = "oceansdelight:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.foods.get("oceansdelight:seagrass_salad"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.foods.get("oceansdelight:seagrass_salad")));
        });
        helper.succeed();
    }

    /** Expanded Delight is never installed here either; see the Kaleidoscope Cookery test above. */
    @GameTest
    public void expandedDelightDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"apple_juice", "cranberry_juice", "goat_milk_bottle"};
        String[] foods = {"asparagus_soup", "cinnamon_apples", "peanut_salad", "cranberries"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("expandeddelight:" + drink),
                    "the default drinks should list expandeddelight:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("expandeddelight:" + food),
                    "the default foods should list expandeddelight:" + food);
        }
        for (String dry : new String[]{"mac_and_cheese", "sweet_berry_jelly", "grilled_cheese"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey("expandeddelight:" + dry)
                            && !defaults.drinks.containsKey("expandeddelight:" + dry),
                    "expandeddelight:" + dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("expandeddelight:" + drink);
            for (String food : foods) config.foods.remove("expandeddelight:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("expandeddelight:apple_juice", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                if (drink.equals("apple_juice")) continue;
                String id = "expandeddelight:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "expandeddelight:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("expandeddelight:apple_juice"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("expandeddelight:apple_juice")));
        });
        helper.succeed();
    }

    /** Rustic Delight is never installed here either; see the Kaleidoscope Cookery test above. */
    @GameTest
    public void rusticDelightDrinksAreMergedIntoAnOlderConfig(GameTestHelper helper) {
        ThirstConfig defaults = new ThirstConfig();
        String[] drinks = {"coffee", "dark_coffee", "milk_coffee", "honey_coffee", "syrup"};
        String[] foods = {"bell_pepper_soup", "calamari_soup", "sweet_salad", "bell_pepper_red", "bell_pepper_slice_black"};
        for (String drink : drinks) {
            TestFixtures.check(helper, defaults.drinks.containsKey("rusticdelight:" + drink),
                    "the default drinks should list rusticdelight:" + drink);
        }
        for (String food : foods) {
            TestFixtures.check(helper, defaults.foods.containsKey("rusticdelight:" + food),
                    "the default foods should list rusticdelight:" + food);
        }
        for (String dry : new String[]{"cooking_oil", "batter", "roasted_bell_pepper_red", "stuffed_bell_pepper_green", "potato_salad"}) {
            TestFixtures.check(helper, !defaults.foods.containsKey("rusticdelight:" + dry)
                            && !defaults.drinks.containsKey("rusticdelight:" + dry),
                    "rusticdelight:" + dry + " restores no thirst and should not be listed");
        }

        TestFixtures.withConfig(config -> {
            for (String drink : drinks) config.drinks.remove("rusticdelight:" + drink);
            for (String food : foods) config.foods.remove("rusticdelight:" + food);
            // A player's own value, which merging must leave alone.
            config.drinks.put("rusticdelight:coffee", new int[]{1, 1});
        }, () -> {
            ThirstConfig config = ThirstConfig.get();
            for (String drink : drinks) {
                if (drink.equals("coffee")) continue;
                String id = "rusticdelight:" + drink;
                TestFixtures.check(helper, Arrays.equals(config.drinks.get(id), defaults.drinks.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.drinks.get(id))
                                + ", got " + Arrays.toString(config.drinks.get(id)));
            }
            for (String food : foods) {
                String id = "rusticdelight:" + food;
                TestFixtures.check(helper, Arrays.equals(config.foods.get(id), defaults.foods.get(id)),
                        id + " should be merged back as " + Arrays.toString(defaults.foods.get(id))
                                + ", got " + Arrays.toString(config.foods.get(id)));
            }
            TestFixtures.check(helper, Arrays.equals(config.drinks.get("rusticdelight:coffee"), new int[]{1, 1}),
                    "a value the player set should survive the merge, got "
                            + Arrays.toString(config.drinks.get("rusticdelight:coffee")));
        });
        helper.succeed();
    }

    @GameTest
    public void aBrokenKeywordPatternIsIgnoredRatherThanFatal(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            config.enableKeywordMatching = true;
            config.fruitKeywords = "(";
        }, () -> {
            TestFixtures.check(helper, ThirstConfig.get().fruitKeywordPattern() == null,
                    "an invalid pattern should be dropped, not compiled");
            TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.CHORUS_FRUIT)) == null,
                    "with the fruit pattern dropped, chorus fruit should match nothing");
            TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.SUSPICIOUS_STEW)) != null,
                    "the other keyword patterns should keep working");
        });
        helper.succeed();
    }

    private static void restores(GameTestHelper helper, Item item, int[] expected) {
        int[] actual = ThirstApi.thirstValues(new ItemStack(item));
        TestFixtures.check(helper, expected != null && Arrays.equals(actual, expected),
                item + " should restore " + Arrays.toString(expected) + ", got " + Arrays.toString(actual));
    }
}
