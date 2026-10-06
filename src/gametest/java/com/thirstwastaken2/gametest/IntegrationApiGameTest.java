package com.thirstwastaken2.gametest;

import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.api.ThirstEvents;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.DataPackDrinks;
import com.thirstwastaken2.data.DrinkValuesPayload;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Map;

/**
 * What another mod can do without touching internal code: data pack thirst values, the player methods
 * and purity methods on {@link ThirstApi}, and the {@link ThirstEvents} callbacks. See docs/docs/developers/.
 *
 * <p>The gametest mod ships {@code data/thirstwastaken2_gametest/thirstwastaken2/drinks/integration_api.json},
 * one file naming an item for each rule of the resolution order, plus an item nobody registered, which
 * must be skipped without taking the rest of the file with it.
 *
 * <p>Listeners cannot be unregistered, and the tests of a batch share a tick, so every listener here
 * acts on its own test's players only and leaves everyone else's drinks alone.
 */
public final class IntegrationApiGameTest {
    private static final TagKey<Item> DRINKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "drinks"));

    @GameTest
    public void dataPackValuesLoadAndAnUnknownItemIsSkipped(GameTestHelper helper) {
        TestFixtures.check(helper, !ThirstConfig.get().drinks.containsKey("minecraft:sugar")
                        && !ThirstConfig.get().foods.containsKey("minecraft:sugar"),
                "the config should not list sugar, or this test proves nothing");
        // The unknown item shares a file with sugar, so sugar loading means the file was kept.
        restores(helper, Items.SUGAR, new int[]{3, 2});
        TestFixtures.check(helper, DataPackDrinks.size() >= 4,
                "every known item in the gametest file should load, got " + DataPackDrinks.size());
        helper.succeed();
    }

    @GameTest
    public void theConfigWinsOverADataPack(GameTestHelper helper) {
        TestFixtures.check(helper, Arrays.equals(DataPackDrinks.get(Items.APPLE), new int[]{9, 9}),
                "the gametest data pack should give the apple 9/9, or this test proves nothing");
        restores(helper, Items.APPLE, ThirstConfig.get().foods.get("minecraft:apple"));
        helper.succeed();
    }

    @GameTest
    public void theConfigBlacklistWinsOverADataPack(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.itemBlacklist.add("minecraft:sugar"), () ->
                TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.SUGAR)) == null,
                        "a blacklisted item should restore nothing whatever a data pack says"));
        restores(helper, Items.SUGAR, new int[]{3, 2});
        helper.succeed();
    }

    @GameTest
    public void aDataPackWinsOverTheDrinkTag(GameTestHelper helper) {
        ThirstConfig config = ThirstConfig.get();
        TestFixtures.check(helper, new ItemStack(Items.DRIED_KELP).is(DRINKS),
                "the gametest data pack should tag dried kelp c:drinks, or this test proves nothing");
        TestFixtures.check(helper, !Arrays.equals(config.drinkTagValue, new int[]{5, 1}),
                "the tag value should differ from the data pack's, or this test proves nothing");
        restores(helper, Items.DRIED_KELP, new int[]{5, 1});
        helper.succeed();
    }

    @GameTest
    public void aDataPackEntryOfNothingBeatsTheKeywords(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.enableKeywordMatching = true, () -> {
            ThirstConfig config = ThirstConfig.get();
            TestFixtures.check(helper, config.fruitKeywordPattern().matcher("glistering_melon_slice").find()
                            && !config.keywordBlacklistPattern().matcher("glistering_melon_slice").find(),
                    "the melon slice should match a fruit keyword, or this test proves nothing");
            // The positive control: an unlisted fruit the pack leaves alone does get the fruit value.
            restores(helper, Items.CHORUS_FRUIT, config.keywordFruitValue);
            TestFixtures.check(helper, ThirstApi.thirstValues(new ItemStack(Items.GLISTERING_MELON_SLICE)) == null,
                    "a data pack entry of 0 should take the item out of keyword matching, got "
                            + Arrays.toString(ThirstApi.thirstValues(new ItemStack(Items.GLISTERING_MELON_SLICE))));
        });
        helper.succeed();
    }

    /** The same parse /reload runs, over the packs the server has loaded now. */
    @GameTest
    public void valuesSurviveAReload(GameTestHelper helper) {
        int before = DataPackDrinks.size();
        DataPackDrinks.reload(helper.getLevel().getServer().getResourceManager());
        TestFixtures.check(helper, DataPackDrinks.size() == before,
                "a reload of the same packs should load the same items, got " + DataPackDrinks.size() + " for " + before);
        restores(helper, Items.SUGAR, new int[]{3, 2});
        helper.succeed();
    }

    /** What a client receives on join and after /reload. */
    @GameTest
    public void theSyncPayloadRoundTrips(GameTestHelper helper) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        new DrinkValuesPayload(Map.of(Items.SUGAR, new int[]{3, 2}, Items.APPLE, new int[]{0, 0})).write(buffer);
        Map<Item, int[]> received = DrinkValuesPayload.read(buffer).values();
        TestFixtures.check(helper, received.size() == 2
                        && Arrays.equals(received.get(Items.SUGAR), new int[]{3, 2})
                        && Arrays.equals(received.get(Items.APPLE), new int[]{0, 0})
                        && buffer.readableBytes() == 0,
                "the payload should decode to what was encoded, got " + received.size() + " entries");
        helper.succeed();
    }

    @GameTest
    public void playerMethodsReadAndWriteThirst(GameTestHelper helper) {
        ServerPlayer player = player(helper, 10, 0);
        TestFixtures.check(helper, ThirstApi.maxThirst() == ThirstData.MAX, "maxThirst should be ThirstData.MAX");
        TestFixtures.check(helper, ThirstApi.thirst(player) == 10 && ThirstApi.quenched(player) == 0,
                "the API should read what the player has, got " + ThirstManager.get(player));
        TestFixtures.check(helper, ThirstApi.isEnabled(player), "thirst should apply to a survival player");

        ThirstApi.drink(player, 3, 2);
        TestFixtures.check(helper, ThirstApi.thirst(player) == 13 && ThirstApi.quenched(player) == 2,
                "drink should restore 3 and 2, got " + ThirstManager.get(player));
        ThirstApi.drink(player, -5, -5);
        TestFixtures.check(helper, ThirstApi.thirst(player) == 13 && ThirstApi.quenched(player) == 2,
                "negative amounts should count as nothing, got " + ThirstManager.get(player));

        ThirstApi.addExhaustion(player, 2.0F);
        TestFixtures.check(helper, ThirstManager.get(player).exhaustion() > 0.0F,
                "addExhaustion should charge at once, got " + ThirstManager.get(player));

        ThirstManager.set(player, ThirstManager.get(player).withEnabled(false));
        TestFixtures.check(helper, !ThirstApi.isEnabled(player), "a player with thirst turned off is not enabled");
        ServerPlayer invulnerable = player(helper, 10, 0);
        invulnerable.getAbilities().invulnerable = true;
        TestFixtures.check(helper, !ThirstApi.isEnabled(invulnerable), "an invulnerable player is not enabled");
        helper.succeed();
    }

    @GameTest
    public void drinkListenersCanChangeOrCancelADrink(GameTestHelper helper) {
        int[] honey = ThirstConfig.get().drinks.get("minecraft:honey_bottle");
        ServerPlayer plain = player(helper, 5, 0);
        ServerPlayer doubled = player(helper, 5, 0);
        ServerPlayer cancelled = player(helper, 5, 0);
        ServerPlayer throwing = player(helper, 5, 0);
        ItemStack[] seen = new ItemStack[1];
        ThirstEvents.DRINK.register((player, stack, drink) -> {
            if (player == throwing) throw new IllegalStateException("a broken listener, on purpose");
        });
        ThirstEvents.DRINK.register((player, stack, drink) -> {
            if (player == doubled || player == throwing) {
                seen[0] = stack;
                drink.setThirst(drink.thirst() * 2);
            }
            if (player == cancelled) drink.cancel();
        });

        for (ServerPlayer player : new ServerPlayer[]{plain, doubled, cancelled, throwing}) {
            ThirstManager.drinkItem(player, new ItemStack(Items.HONEY_BOTTLE));
        }
        TestFixtures.check(helper, ThirstApi.thirst(plain) == 5 + honey[0],
                "a drink no listener touches should restore the honey value, got " + ThirstManager.get(plain));
        TestFixtures.check(helper, ThirstApi.thirst(doubled) == 5 + honey[0] * 2,
                "a listener should be able to double a drink, got " + ThirstManager.get(doubled));
        TestFixtures.check(helper, seen[0] != null && seen[0].is(Items.HONEY_BOTTLE),
                "the listener should be handed the stack being drunk, got " + seen[0]);
        TestFixtures.check(helper, ThirstApi.thirst(cancelled) == 5 && ThirstApi.quenched(cancelled) == 0,
                "a cancelled drink should restore nothing, got " + ThirstManager.get(cancelled));
        TestFixtures.check(helper, ThirstApi.thirst(throwing) == 5 + honey[0] * 2,
                "a listener that throws should be skipped and the rest still run, got " + ThirstManager.get(throwing));
        helper.succeed();
    }

    @GameTest
    public void saltWaterFiresNoDrink(GameTestHelper helper) {
        ServerPlayer player = player(helper, 5, 0);
        boolean[] fired = new boolean[1];
        ThirstEvents.DRINK.register((drinker, stack, drink) -> {
            if (drinker == player) fired[0] = true;
        });
        ThirstManager.drinkItem(player, WaterPurity.setQuality(
                new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), WaterQuality.SALT));
        TestFixtures.check(helper, !fired[0], "salt water restores nothing, so it should not fire DRINK");
        ThirstManager.drinkItem(player, new ItemStack(Items.HONEY_BOTTLE));
        TestFixtures.check(helper, fired[0], "honey should fire DRINK, or the check above proves nothing");
        helper.succeed();
    }

    @GameTest
    public void exhaustionListenersChangeWhatATickCharges(GameTestHelper helper) {
        ServerPlayer spared = player(helper, 10, 5);
        ServerPlayer charged = player(helper, 10, 5);
        float[] seen = new float[1];
        ThirstEvents.EXHAUSTION.register((player, amount) -> {
            if (player != spared) return amount;
            seen[0] = amount;
            return 0.0F;
        });

        for (ServerPlayer player : new ServerPlayer[]{spared, charged}) {
            ThirstManager.mirrorExhaustion(player, 2.0F);
            ThirstManager.tickPlayer(player);
        }
        TestFixtures.check(helper, seen[0] == 2.0F,
                "the listener should see the tick's raw exhaustion, got " + seen[0]);
        // Only the baseline is left, which no listener sees: a tick's worth, a few thousandths.
        TestFixtures.check(helper, ThirstManager.get(spared).exhaustion() < 0.01F,
                "a listener returning 0 should leave only the baseline, got " + ThirstManager.get(spared));
        TestFixtures.check(helper, ThirstManager.get(charged).exhaustion() > 0.0F,
                "a player the listener passes over should still be charged, got " + ThirstManager.get(charged));
        helper.succeed();
    }

    @GameTest
    public void purityMethodsReadWater(GameTestHelper helper) {
        ItemStack murky = ThirstApi.waterBottle(1);
        TestFixtures.check(helper, ThirstApi.isWaterContainer(murky) && ThirstApi.purity(murky) == 1
                        && !ThirstApi.isSalt(murky),
                "waterBottle(1) should be fresh water of grade 1, got " + WaterPurity.quality(murky));
        TestFixtures.check(helper, ThirstApi.purity(ThirstApi.waterBottle(99)) == ThirstApi.maxPurity(),
                "waterBottle should clamp to the highest grade");

        ItemStack sea = WaterPurity.setQuality(TestFixtures.waterBottle(), WaterQuality.SALT);
        TestFixtures.check(helper, ThirstApi.isSalt(sea) && ThirstApi.purity(sea) == ThirstApi.NOT_WATER,
                "sea water should be salt with no grade, got " + ThirstApi.purity(sea));

        ItemStack stone = new ItemStack(Items.STONE);
        TestFixtures.check(helper, !ThirstApi.isWaterContainer(stone) && !ThirstApi.isSalt(stone)
                        && ThirstApi.purity(stone) == ThirstApi.NOT_WATER,
                "stone holds no water");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, int thirst, int quenched) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        // Neither a multiple of 11 nor of 40, as in ThirstTickGameTest: no refill, no damage.
        player.tickCount = 1;
        ThirstManager.set(player, ThirstData.full().withLevels(thirst, quenched).withExhaustion(0.0F));
        return player;
    }

    private static void restores(GameTestHelper helper, Item item, int[] expected) {
        int[] actual = ThirstApi.thirstValues(new ItemStack(item));
        TestFixtures.check(helper, Arrays.equals(actual, expected),
                item + " should restore " + Arrays.toString(expected) + ", got " + Arrays.toString(actual));
    }
}
