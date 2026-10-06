package com.thirstwastaken2.gametest;

import com.thirstwastaken2.compat.AppleSkin;
import com.thirstwastaken2.config.QuenchedOverlay;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.tooltip.ThirstTooltip;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The lines the mod contributes to an item tooltip.
 *
 * <p>Assertions look at translation keys rather than rendered text, because a dedicated server never
 * loads the mod's language files and would report every key as itself.
 *
 * <p>The droplet rows only appear alongside AppleSkin, which this server does not have, so the tests
 * about them ask for the rows outright.
 */
public final class TooltipGameTest {
    @GameTest
    public void waterContainerShowsPurity(GameTestHelper helper) {
        List<Component> lines = linesFor(bowl(WaterQuality.fresh(3)));

        TestFixtures.check(helper, hasGradeLine(lines),
                "a water container should get a purity line, got " + keys(lines));
        helper.succeed();
    }

    /**
     * Salt water is not a grade of fresh water, and it restores nothing. Its tooltip has to say that
     * once, instead of pairing a grade it does not have with droplets it does not give.
     */
    @GameTest
    public void saltWaterReplacesTheGradeAndTheDroplets(GameTestHelper helper) {
        List<Component> salty = linesFor(bowl(WaterQuality.SALT));
        List<Component> fresh = linesFor(bowl(WaterQuality.fresh(2)));

        TestFixtures.check(helper, hasKey(salty, "thirst.water.salty"),
                "salt water should get a salinity line, got " + keys(salty));
        TestFixtures.check(helper, !hasGradeLine(salty),
                "salt water has no grade to report, got " + keys(salty));
        TestFixtures.check(helper, salty.stream().allMatch(line -> line.getContents() instanceof TranslatableContents),
                "salt water restores nothing, so it should get no droplet rows, got " + keys(salty));
        TestFixtures.check(helper, hasGradeLine(fresh)
                        && !hasKey(fresh, "thirst.water.salty"),
                "fresh water should report a grade and no salinity, got " + keys(fresh));
        helper.succeed();
    }

    @GameTest
    public void theClayBowlSaysItHasToBeFired(GameTestHelper helper) {
        List<Component> clay = linesFor(new ItemStack(ThirstItems.CLAY_BOWL));
        List<Component> terracotta = linesFor(new ItemStack(ThirstItems.TERRACOTTA_BOWL));

        TestFixtures.check(helper, hasKey(clay, "tooltip.thirstwastaken2.clay_bowl"),
                "the clay bowl should say it needs firing first, got " + keys(clay));
        TestFixtures.check(helper, !hasKey(terracotta, "tooltip.thirstwastaken2.clay_bowl"),
                "the fired bowl should not still ask to be fired, got " + keys(terracotta));
        helper.succeed();
    }

    @GameTest
    public void waterskinShowsItsServings(GameTestHelper helper) {
        ItemStack empty = new ItemStack(ThirstItems.WATERSKIN);
        ItemStack filled = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(filled, WaterQuality.fresh(3), 2);

        TestFixtures.check(helper,
                hasKey(linesFor(empty), "tooltip.thirstwastaken2.waterskin.empty"),
                "an empty waterskin should say so");
        TestFixtures.check(helper,
                hasKey(linesFor(filled), "tooltip.thirstwastaken2.waterskin.servings"),
                "a filled waterskin should report its servings");
        helper.succeed();
    }

    @GameTest
    public void thirstRowsAreRendered(GameTestHelper helper) {
        // The bowl restores 4 thirst and 5 quenched in the default config, so both rows appear.
        List<Component> lines = linesFor(bowl(WaterQuality.fresh(3)));

        long droplets = lines.stream().filter(line -> !(line.getContents() instanceof TranslatableContents)).count();
        TestFixtures.check(helper, droplets >= 2,
                "a drink should get a thirst row and a quenched row, got " + keys(lines));
        helper.succeed();
    }

    @GameTest
    public void dropletRowsRoundHalvesUp(GameTestHelper helper) {
        // Two units per droplet, and a leftover half unit still gets a droplet of its own.
        TestFixtures.check(helper, ThirstTooltip.thirst(0) == null,
                "an item that restores nothing should get no row");
        TestFixtures.check(helper, length(ThirstTooltip.thirst(4)) == 2,
                "4 units is 2 droplets, got " + length(ThirstTooltip.thirst(4)));
        TestFixtures.check(helper, length(ThirstTooltip.thirst(5)) == 3,
                "5 units rounds up to 3 droplets, got " + length(ThirstTooltip.thirst(5)));
        TestFixtures.check(helper, length(ThirstTooltip.quenched(40, QuenchedOverlay.DIAMOND)) == 10,
                "the row is capped at 10 droplets, got " + length(ThirstTooltip.quenched(40, QuenchedOverlay.DIAMOND)));
        helper.succeed();
    }

    /**
     * Without AppleSkin the droplet rows are left out, the way vanilla says nothing about what food
     * restores. The item's own lines and the water grade are not part of that and stay.
     */
    @GameTest
    public void dropletRowsNeedAppleSkin(GameTestHelper helper) {
        TestFixtures.check(helper, !AppleSkin.isLoaded(),
                "this test expects the gametest server to run without AppleSkin");
        List<Component> lines = new ArrayList<>();
        ThirstTooltip.appendTo(bowl(WaterQuality.fresh(3)), lines::add);

        TestFixtures.check(helper, hasGradeLine(lines),
                "the grade does not depend on AppleSkin, got " + keys(lines));
        TestFixtures.check(helper, lines.stream().allMatch(line -> line.getContents() instanceof TranslatableContents),
                "without AppleSkin there should be no droplet rows, got " + keys(lines));
        helper.succeed();
    }

    @GameTest
    public void eachQuenchedOverlayHasItsOwnGlyphs(GameTestHelper helper) {
        Set<String> rows = new HashSet<>();
        for (QuenchedOverlay overlay : QuenchedOverlay.values()) {
            rows.add(ThirstTooltip.quenched(3, overlay).getString());
        }

        TestFixtures.check(helper, rows.size() == QuenchedOverlay.values().length,
                "every quenched overlay should draw its own droplets, got " + rows.size() + " distinct rows");
        TestFixtures.check(helper, !rows.contains(ThirstTooltip.thirst(3).getString()),
                "a quenched row should never look like the thirst row");
        helper.succeed();
    }

    @GameTest
    public void cachedLinesAreHandedOutAsCopies(GameTestHelper helper) {
        // Lines are built once and copied out. Restyling a returned line in place, as other mods are
        // free to do, must not leak into the next tooltip.
        ((MutableComponent) WaterPurity.tooltip(3)).withStyle(ChatFormatting.OBFUSCATED);
        ((MutableComponent) ThirstTooltip.thirst(4)).withStyle(ChatFormatting.OBFUSCATED);

        TestFixtures.check(helper, !WaterPurity.tooltip(3).getStyle().isObfuscated(),
                "restyling one purity line should not change the next one");
        TestFixtures.check(helper, !ThirstTooltip.thirst(4).getStyle().isObfuscated(),
                "restyling one droplet row should not change the next one");
        helper.succeed();
    }

    @GameTest
    public void plainItemsGetNoLines(GameTestHelper helper) {
        List<Component> lines = linesFor(new ItemStack(net.minecraft.world.item.Items.STONE));

        TestFixtures.check(helper, lines.isEmpty(),
                "a stone block should get no thirst lines, got " + keys(lines));
        helper.succeed();
    }

    private static List<Component> linesFor(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        ThirstTooltip.appendTo(stack, lines::add, true);
        return lines;
    }

    private static ItemStack bowl(WaterQuality quality) {
        return WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
    }

    private static int length(Component row) {
        return row == null ? 0 : row.getString().length();
    }

    private static boolean hasKey(List<Component> lines, String key) {
        return lines.stream().anyMatch(line -> keyOf(line).equals(key));
    }

    /** Whether one of the lines names a fresh water grade. Salt water's line shares the prefix, not the key. */
    private static boolean hasGradeLine(List<Component> lines) {
        return hasKey(lines, "thirst.water.dirty") || hasKey(lines, "thirst.water.murky")
                || hasKey(lines, "thirst.water.clean") || hasKey(lines, "thirst.water.pure");
    }

    private static String keyOf(Component line) {
        return line.getContents() instanceof TranslatableContents translatable
                ? translatable.getKey()
                : "";
    }

    private static String keys(List<Component> lines) {
        return lines.stream().map(line -> keyOf(line).isEmpty() ? "<droplets>" : keyOf(line)).toList().toString();
    }
}
