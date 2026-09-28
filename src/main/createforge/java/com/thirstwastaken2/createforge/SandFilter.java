package com.thirstwastaken2.createforge;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.ThirstItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

/**
 * The Sand Filter's block, item, block entity type and creative tab entry. Only registered alongside
 * Create. The fluid capability is the block entity's own, as every Forge 47 block entity's is.
 */
public final class SandFilter {
    private static final String NAME = "sand_filter";

    private static Block block;
    private static Item item;
    private static BlockEntityType<SandFilterBlockEntity> blockEntity;

    private SandFilter() { }

    public static Block block() {
        return block;
    }

    public static BlockEntityType<SandFilterBlockEntity> blockEntity() {
        return blockEntity;
    }

    static void register(IEventBus modBus) {
        modBus.addListener(SandFilter::onRegister);
        modBus.addListener(SandFilter::onCreativeTab);
    }

    private static void onRegister(RegisterEvent event) {
        // Forge fires blocks first, then items and block entity types, so each one can use the last.
        event.register(Registries.BLOCK, helper -> {
            // Copper like the original, which took Create's copper metal properties.
            block = new SandFilterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER)
                    .noOcclusion());
            helper.register(ThirstWasTaken2.id(NAME), block);
        });
        event.register(Registries.ITEM, helper -> {
            item = new BlockItem(block, new Item.Properties());
            helper.register(ThirstWasTaken2.id(NAME), item);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            blockEntity = BlockEntityType.Builder.of(SandFilterBlockEntity::new, block).build(null);
            helper.register(ThirstWasTaken2.id(NAME), blockEntity);
            ThirstWasTaken2.LOGGER.info("Create found, registered the Sand Filter");
        });
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ThirstItems.CREATIVE_TAB_KEY)) event.accept(item);
    }
}
