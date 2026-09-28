package com.thirstwastaken2.gametest;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * What decides how an item looks, as far as a server can see it: the custom model data the sprites
 * dispatch on, that sea water leaves vanilla's bottle and bucket alone, and the waterskin's durability
 * bar.
 *
 * <p>Whether the sprite the client then draws is the right picture is a manual check; see
 * docs/dev/MANUAL-TESTING.md. What is tested here is that the value it draws from is right.
 */
public final class ItemAppearanceGameTest {
    @GameTest
    public void aBowlsSpriteFollowsItsWater(GameTestHelper helper) {
        for (int grade = WaterPurity.MIN; grade <= WaterPurity.MAX; grade++) {
            ItemStack bowl = bowl(WaterQuality.fresh(grade));
            TestFixtures.check(helper, Vanilla.selectsModel(bowl, ThirstItems.BOWL_MODEL_INDEX, grade),
                    "a grade " + grade + " bowl should select sprite " + grade + ", got "
                            + Vanilla.modelSelectorOf(bowl));
        }
        ItemStack salty = bowl(WaterQuality.SALT);
        TestFixtures.check(helper, Vanilla.selectsModel(salty, ThirstItems.BOWL_MODEL_INDEX, WaterPurity.MAX + 1),
                "a salty bowl should select the sprite after the grades, got " + Vanilla.modelSelectorOf(salty));
        TestFixtures.check(helper, Vanilla.selectsModel(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL),
                        ThirstItems.BOWL_MODEL_INDEX, WaterPurity.MAX),
                "a bowl straight from the creative tab is pure water and should look it");
        helper.succeed();
    }

    @GameTest
    public void aWaterskinsSpriteFollowsItsServings(GameTestHelper helper) {
        ItemStack skin = new ItemStack(ThirstItems.WATERSKIN);
        TestFixtures.check(helper, Vanilla.modelSelectorOf(skin) == null,
                "an empty waterskin should carry no model data, so it falls back to the empty sprite");

        for (int servings = 1; servings <= WaterskinItem.CAPACITY; servings++) {
            WaterskinItem.addWater(skin, WaterQuality.fresh(2), 1);
            TestFixtures.check(helper, Vanilla.selectsModel(skin, ThirstItems.WATERSKIN_MODEL_INDEX, servings),
                    "a waterskin with " + servings + " servings should select sprite " + servings + ", got "
                            + Vanilla.modelSelectorOf(skin));
        }

        WaterskinItem.removeWater(skin, WaterskinItem.CAPACITY);
        TestFixtures.check(helper, Vanilla.modelSelectorOf(skin) == null,
                "emptying the waterskin should take the model data away again");
        helper.succeed();
    }

    /**
     * Sea water in a vanilla bottle or bucket keeps vanilla's sprite, the same on every version: only
     * the tooltip tells it apart. Up to 1.4 it swapped the item model on 1.21.2 and later, which left the
     * versions looking different.
     */
    @GameTest
    public void seaWaterInABottleOrBucketKeepsVanillasSprite(GameTestHelper helper) {
        ItemStack bottle = WaterPurity.setQuality(TestFixtures.waterBottle(), WaterQuality.SALT);
        ItemStack bucket = WaterPurity.setQuality(
                new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET), WaterQuality.SALT);
        TestFixtures.check(helper, WaterPurity.isSalty(bottle) && WaterPurity.isSalty(bucket),
                "the bottle and the bucket should hold sea water");
        // From 1.21.4 every item carries an item model by default, so compare with an untouched stack.
        Object vanillaBottle = Vanilla.itemModelOf(TestFixtures.waterBottle());
        Object vanillaBucket = Vanilla.itemModelOf(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        TestFixtures.check(helper, java.util.Objects.equals(vanillaBottle, Vanilla.itemModelOf(bottle)),
                "a salty bottle should keep vanilla's model " + vanillaBottle + ", got " + Vanilla.itemModelOf(bottle));
        TestFixtures.check(helper, java.util.Objects.equals(vanillaBucket, Vanilla.itemModelOf(bucket)),
                "a salty bucket should keep vanilla's model " + vanillaBucket + ", got " + Vanilla.itemModelOf(bucket));
        TestFixtures.check(helper, WaterPurity.setQuality(bottle, WaterQuality.fresh(2)).is(net.minecraft.world.item.Items.POTION),
                "changing the water should never change the item itself");
        helper.succeed();
    }

    @GameTest
    public void theWaterskinBarShowsFillAndGrade(GameTestHelper helper) {
        ItemStack skin = new ItemStack(ThirstItems.WATERSKIN);
        TestFixtures.check(helper, !skin.isBarVisible(), "an empty waterskin should show no bar");

        WaterskinItem.addWater(skin, WaterQuality.fresh(WaterPurity.MAX), 1);
        TestFixtures.check(helper, skin.isBarVisible() && skin.getBarWidth() == Math.round(13.0F / 3),
                "one serving should fill a third of the bar, got " + skin.getBarWidth());
        int pure = skin.getBarColor();

        WaterskinItem.addWater(skin, WaterQuality.fresh(WaterPurity.MAX), 2);
        TestFixtures.check(helper, skin.getBarWidth() == 13, "a full waterskin should fill the bar, got " + skin.getBarWidth());

        ItemStack dirty = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(dirty, WaterQuality.fresh(WaterPurity.MIN), 1);
        ItemStack salty = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(salty, WaterQuality.SALT, 1);
        TestFixtures.check(helper, pure != dirty.getBarColor() && pure != salty.getBarColor()
                        && dirty.getBarColor() != salty.getBarColor(),
                "pure, dirty and salty water should each colour the bar differently");
        helper.succeed();
    }

    private static ItemStack bowl(WaterQuality quality) {
        return WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
    }
}
