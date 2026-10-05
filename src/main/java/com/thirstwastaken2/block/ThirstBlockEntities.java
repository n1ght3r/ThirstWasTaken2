package com.thirstwastaken2.block;

import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ThirstBlockEntities {
    /** The distiller's machine, on its boiler half. */
    public static final BlockEntityType<DistillerBlockEntity> COPPER_DISTILLER = Vanilla.registerBlockEntity(
            "copper_distiller", DistillerBlockEntity::new, ThirstBlocks.COPPER_DISTILLER);

    private ThirstBlockEntities() { }

    /**
     * Builds and registers the block entity types, in this class's static initializer, the same way
     * {@code ThirstBlocks.register} does. Runs after the blocks, which the types name.
     */
    public static void register() { }
}
