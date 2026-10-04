package com.thirstwastaken2.block;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ThirstBlocks {
    // Seconds a serving takes to boil pure in each pot come from the config, 4 for copper and 6 for
    // iron by default, so a full pot takes three times as long as a bottle. A furnace takes 10 seconds a
    // bucket and raises it two grades; see docs/dev/mechanics/WATER-REFERENCE.md for how the
    // numbers were chosen. Iron carries heat worse than copper.

    /**
     * Breaks quickly by hand, so it needs no tool tag. A piston knocks
     * it loose rather than pushing a pot of water around.
     */
    public static final HangingPotBlock COPPER_HANGING_POT = Vanilla.registerBlock("copper_hanging_pot",
            properties -> new HangingPotBlock(properties, () -> ThirstConfig.get().copperHangingPotBoilSeconds),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));
    /** The same pot in dark cast iron. Iron carries heat worse than copper, so it boils slower. */
    public static final HangingPotBlock IRON_HANGING_POT = Vanilla.registerBlock("iron_hanging_pot",
            properties -> new HangingPotBlock(properties, () -> ThirstConfig.get().ironHangingPotBoilSeconds),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    /**
     * The copper distiller, two blocks wide. Slower to mine than a pot, by hand too. A piston breaks
     * it rather than pushing one half away from the other. Its firebox is always lit for now, and
     * glows like a furnace that is.
     */
    public static final DistillerBlock COPPER_DISTILLER = Vanilla.registerBlock("copper_distiller",
            DistillerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(DistillerBlock.PART) == DistillerBlock.Part.BOILER ? 13 : 0)
                    .pushReaction(PushReaction.POPPED));

    private ThirstBlocks() { }

    /**
     * Builds and registers the blocks, in this class's static initializer, the same way
     * {@code ThirstItems.register} does. Runs before the items, which place them.
     */
    public static void register() { }
}
