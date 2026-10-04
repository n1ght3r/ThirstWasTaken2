package com.thirstwastaken2.gametest;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * The mod's creative tab.
 *
 * <p>Fabric API renamed the builder entrypoint in 26.1, so {@code Loader.creativeTabBuilder} is one
 * of the few places where the mod compiles against a different class per version. A tab built
 * through the wrong entrypoint would still compile, which is exactly why it is worth asserting
 * rather than assuming.
 */
public final class CreativeTabGameTest {
    private static final List<Item> EXPECTED = List.of(
            ThirstItems.CLAY_BOWL,
            ThirstItems.TERRACOTTA_BOWL,
            ThirstItems.TERRACOTTA_WATER_BOWL,
            ThirstItems.WATERSKIN,
            ThirstItems.COPPER_CANTEEN,
            ThirstItems.IRON_FLASK,
            ThirstItems.COPPER_HANGING_POT,
            ThirstItems.IRON_HANGING_POT,
            ThirstItems.COPPER_DISTILLER,
            ThirstItems.COPPER_PIPE,
            ThirstItems.DISTILLER_BOILER,
            ThirstItems.COOLING_TUB,
            ThirstItems.BRICK_FIREBOX);

    @GameTest
    public void tabIconIsAWaterskin(GameTestHelper helper) {
        CreativeModeTab tab = requireTab(helper);

        ItemStack icon = tab.getIconItem();
        TestFixtures.check(helper, icon.is(ThirstItems.WATERSKIN) && WaterskinItem.servings(icon) == 0,
                "the tab icon should be an empty waterskin, got " + icon);
        helper.succeed();
    }

    @GameTest
    public void tabHoldsEveryItemTheModAdds(GameTestHelper helper) {
        Collection<ItemStack> contents = displayItems(helper, helper.getLevel());

        for (Item expected : EXPECTED) {
            TestFixtures.check(helper, contents.stream().anyMatch(stack -> stack.is(expected)),
                    "the creative tab is missing " + expected + ", it holds " + contents.size()
                            + " items");
        }
        helper.succeed();
    }

    /** A Mod Items switch takes its item out of the tab, and nothing else. */
    @GameTest
    public void tabLeavesOutASwitchedOffItem(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.enableIronFlask = false, () -> {
            Collection<ItemStack> contents = displayItems(helper, helper.getLevel());
            for (Item expected : EXPECTED) {
                boolean listed = contents.stream().anyMatch(stack -> stack.is(expected));
                TestFixtures.check(helper, listed == (expected != ThirstItems.IRON_FLASK),
                        expected + (listed ? " should not be" : " should be") + " in the tab with enableIronFlask off");
            }
        });
        helper.succeed();
    }

    private static CreativeModeTab tab() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getValue(ThirstItems.CREATIVE_TAB_KEY);
    }

    private static CreativeModeTab requireTab(GameTestHelper helper) {
        CreativeModeTab tab = tab();
        TestFixtures.check(helper, tab != null, "the creative tab is not registered");
        return tab;
    }

    /** Tab contents stay empty until they are built, which a dedicated server never does on its own. */
    private static Collection<ItemStack> displayItems(GameTestHelper helper, ServerLevel level) {
        CreativeModeTab tab = requireTab(helper);
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                level.enabledFeatures(), true, level.registryAccess()));
        return tab.getDisplayItems();
    }
}
