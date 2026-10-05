package com.thirstwastaken2.item;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.DrinkItem;
import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ThirstItems {
    /** Custom model data index the filled bowl's sprite dispatches on: its grade, with salt one past. */
    public static final int BOWL_MODEL_INDEX = 1;
    /** Custom model data index the waterskin's sprite dispatches on: servings left. */
    public static final int WATERSKIN_MODEL_INDEX = 0;
    // Seconds a serving takes to boil over a campfire come from the config, per vessel: 3 s and 4 s by
    // default. Quicker than the hanging pots' 4 s and 6 s, because the player stands holding use the
    // whole time. See docs/dev/mechanics/WATER-REFERENCE.md. Capacity is the config's too.

    public static final Item CLAY_BOWL = Vanilla.registerItem("clay_bowl", Item::new, new Item.Properties().stacksTo(64));
    public static final Item TERRACOTTA_BOWL = Vanilla.registerItem("terracotta_bowl", Item::new, new Item.Properties().stacksTo(64));
    /**
     * A filled bowl does not stack, matching every vanilla drink container. Stacking it would put
     * dozens of drinks in one slot and leave the waterskin, which holds three, with no purpose.
     */
    public static final Item TERRACOTTA_WATER_BOWL = Vanilla.registerItem("terracotta_water_bowl",
            properties -> new DrinkItem(properties, TERRACOTTA_BOWL),
            Vanilla.modelSelectorByDefault(ItemWaterData.freshByDefault(new Item.Properties().stacksTo(1), 3),
                    BOWL_MODEL_INDEX, 3));
    public static final Item WATERSKIN = Vanilla.registerItem("waterskin", WaterskinItem::new,
            ItemWaterData.emptyByDefault(new Item.Properties().stacksTo(1)));
    /**
     * Crafted from scratch rather than upgraded from a waterskin. Copper carries heat well, so it boils
     * fastest; it has no furnace recipe, which is the iron flask's.
     */
    public static final Item COPPER_CANTEEN = Vanilla.registerItem("copper_canteen",
            properties -> new WaterskinItem(properties, () -> ThirstConfig.get().copperCanteenCapacity,
                    () -> ThirstConfig.get().copperCanteenBoilSeconds * 20, false),
            ItemWaterData.emptyByDefault(new Item.Properties().stacksTo(1)));
    /** Holds the most and boils slower than copper, but also cleans its water in a furnace. */
    public static final Item IRON_FLASK = Vanilla.registerItem("iron_flask",
            properties -> new WaterskinItem(properties, () -> ThirstConfig.get().ironFlaskCapacity,
                    () -> ThirstConfig.get().ironFlaskBoilSeconds * 20, false),
            ItemWaterData.emptyByDefault(new Item.Properties().stacksTo(1)));
    public static final Item COPPER_HANGING_POT = Vanilla.registerBlockItem(ThirstBlocks.COPPER_HANGING_POT,
            properties -> new HangingPotItem(ThirstBlocks.COPPER_HANGING_POT, properties), new Item.Properties());
    public static final Item IRON_HANGING_POT = Vanilla.registerBlockItem(ThirstBlocks.IRON_HANGING_POT,
            properties -> new HangingPotItem(ThirstBlocks.IRON_HANGING_POT, properties), new Item.Properties());
    /**
     * The copper distiller, crafted from the four parts below, each one piece of the machine: the pipe
     * its swan neck and coil, the boiler, the cooling tub, and the brick firebox the boiler sits on. The
     * three that are not the pipe also place as blocks, and built that way in the world, the boiler on
     * the firebox, the tub beside it and the pipe joining them, they make the same distiller.
     */
    public static final Item COPPER_DISTILLER = Vanilla.registerBlockItem(ThirstBlocks.COPPER_DISTILLER,
            new Item.Properties());
    public static final Item COPPER_PIPE = Vanilla.registerItem("copper_pipe", CopperPipeItem::new,
            new Item.Properties());
    public static final Item DISTILLER_BOILER = Vanilla.registerBlockItem(ThirstBlocks.DISTILLER_BOILER,
            properties -> new DistillerBoilerItem(ThirstBlocks.DISTILLER_BOILER, properties), new Item.Properties());
    public static final Item COOLING_TUB = Vanilla.registerBlockItem(ThirstBlocks.COOLING_TUB, new Item.Properties());
    public static final Item BRICK_FIREBOX = Vanilla.registerBlockItem(ThirstBlocks.BRICK_FIREBOX,
            new Item.Properties());
    public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, ThirstWasTaken2.id("thirstwastaken2"));

    private ThirstItems() { }

    /**
     * Builds and registers the items, which happens in this class's static initializer: calling it is
     * what triggers that. Nothing may touch the fields before this runs, because on a loader that
     * freezes the registries early the items cannot be built any sooner.
     */
    public static void register() { }

    /**
     * Registers the creative tab. Runs after {@link #register}. An item the config switches off is left
     * out; the tab is filled when the client builds it, so it follows the client's own config, and a
     * change shows once the tab is built again, on the next join.
     */
    public static void registerCreativeTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB_KEY,
                Loader.creativeTabBuilder()
                        .title(Component.translatable("itemGroup.thirstwastaken2"))
                        .icon(() -> new ItemStack(WATERSKIN))
                        .displayItems((parameters, entries) -> {
                            ThirstConfig config = ThirstConfig.get();
                            for (Item item : List.of(CLAY_BOWL, TERRACOTTA_BOWL, TERRACOTTA_WATER_BOWL, WATERSKIN,
                                    COPPER_CANTEEN, IRON_FLASK, COPPER_HANGING_POT, IRON_HANGING_POT, COPPER_DISTILLER,
                                    COPPER_PIPE, DISTILLER_BOILER, COOLING_TUB, BRICK_FIREBOX)) {
                                if (config.isItemEnabled(Vanilla.itemId(item).toString())) entries.accept(item);
                            }
                        })
                        .build());
    }
}
