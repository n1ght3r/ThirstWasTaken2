package com.thirstwastaken2.block;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ThirstBlocks {
    // Seconds a serving takes to boil Clean in each pot, and how many servings it holds, come from the
    // config: 3 seconds and 3 servings for copper, 4 and 6 for iron by default. A furnace takes 24 seconds
    // a bucket; see docs/dev/mechanics/WATER-REFERENCE.md for how the numbers were chosen. Iron carries
    // heat worse than copper, and holds more.

    /**
     * Breaks quickly by hand, so it needs no tool tag. A piston knocks
     * it loose rather than pushing a pot of water around.
     */
    public static final HangingPotBlock COPPER_HANGING_POT = Vanilla.registerBlock("copper_hanging_pot",
            properties -> new HangingPotBlock(properties, () -> ThirstConfig.get().copperHangingPotBoilSeconds,
                    () -> ThirstConfig.get().copperHangingPotCapacity),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));
    /** The same pot in dark cast iron. Iron carries heat worse than copper, so it boils slower. */
    public static final HangingPotBlock IRON_HANGING_POT = Vanilla.registerBlock("iron_hanging_pot",
            properties -> new HangingPotBlock(properties, () -> ThirstConfig.get().ironHangingPotBoilSeconds,
                    () -> ThirstConfig.get().ironHangingPotCapacity),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    /**
     * The copper distiller, two blocks wide. Slower to mine than a pot, by hand too. A piston breaks
     * it rather than pushing one half away from the other. While its fire burns it glows like a lit
     * furnace; cold, and a boiler on a firebox not yet piped is always cold, it gives no light.
     */
    public static final DistillerBlock COPPER_DISTILLER = Vanilla.registerBlock("copper_distiller",
            DistillerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .lightLevel(state -> DistillerBlock.burning(state) ? 13 : 0)
                    .pushReaction(PushReaction.POPPED));
    // The distiller's parts, each placeable alone and mined like the stuff it is made of. A boiler set
    // on a firebox becomes the distiller's boiler half; see DistillerBlock.
    public static final DistillerPartBlock BRICK_FIREBOX = Vanilla.registerBlock("brick_firebox",
            properties -> new DistillerPartBlock(properties, DistillerPartBlock.FIREBOX_BOXES),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion());
    public static final DistillerPartBlock DISTILLER_BOILER = Vanilla.registerBlock("distiller_boiler",
            properties -> new DistillerPartBlock(properties, DistillerPartBlock.BOILER_BOXES),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion());
    public static final CoolingTubBlock COOLING_TUB = Vanilla.registerBlock("cooling_tub",
            CoolingTubBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    private ThirstBlocks() { }

    /**
     * Builds and registers the blocks, in this class's static initializer, the same way
     * {@code ThirstItems.register} does. Runs before the items, which place them.
     */
    public static void register() { }
}
