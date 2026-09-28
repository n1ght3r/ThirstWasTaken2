package com.thirstwastaken2.forge;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.Loader;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * Gives the water containers Forge's item fluid capability. Forge attaches capabilities to every item
 * stack as it is made, so the check is one comparison of the item, and nothing is built for any other.
 *
 * <p>Listening starts once the mod's items are registered: stacks are made long before that, and the
 * first look at {@link ThirstItems} registers them.
 */
final class WaterContainerCapabilities {
    private static final Identifier ID = ThirstWasTaken2.id("water_container");

    private WaterContainerCapabilities() { }

    static void register() {
        Loader.onRegister(Registries.ITEM, () ->
                MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, WaterContainerCapabilities::attach));
    }

    private static void attach(AttachCapabilitiesEvent<ItemStack> event) {
        Item item = event.getObject().getItem();
        if (item == ThirstItems.WATERSKIN || item == ThirstItems.COPPER_CANTEEN || item == ThirstItems.IRON_FLASK
                || item == ThirstItems.TERRACOTTA_BOWL || item == ThirstItems.TERRACOTTA_WATER_BOWL) {
            event.addCapability(ID, new Provider(event.getObject()));
        }
    }

    private static final class Provider implements ICapabilityProvider {
        private final LazyOptional<IFluidHandlerItem> handler;

        Provider(ItemStack stack) {
            handler = LazyOptional.of(() -> new WaterContainerFluidHandler(stack));
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(capability, handler);
        }
    }
}
