package com.thirstwastaken2.mixin;

import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;

/**
 * On 1.21.1, tells {@code LayeredCauldronBlockMixin} which of the two layered cauldrons being built is
 * the water one, because a block cannot be identified from inside its own constructor there. See
 * {@code Vanilla.isWaterCauldron}. Empty on later versions, where the block's id answers.
 */
@Mixin(Blocks.class)
abstract class BlocksMixin {
    //? if >=1.20.5 <=1.21.1 {
    /*// Rain fills the water cauldron and snow the powder snow one; at this point nothing else tells them apart.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "<clinit>",
            at = @org.spongepowered.asm.mixin.injection.At(value = "NEW",
                    target = "(Lnet/minecraft/world/level/biome/Biome$Precipitation;Lnet/minecraft/core/cauldron/CauldronInteraction$InteractionMap;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Lnet/minecraft/world/level/block/LayeredCauldronBlock;"))
    private static net.minecraft.world.level.block.LayeredCauldronBlock thirst$markWaterCauldron(
            net.minecraft.world.level.biome.Biome.Precipitation precipitation,
            net.minecraft.core.cauldron.CauldronInteraction.InteractionMap interactions,
            net.minecraft.world.level.block.state.BlockBehaviour.Properties properties,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.level.block.LayeredCauldronBlock> original) {
        return com.thirstwastaken2.platform.Vanilla.buildingWaterCauldron(
                precipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN,
                () -> original.call(precipitation, interactions, properties));
    }
    *///?}
    //? if <1.20.5 {
    /*// Before 1.20.5 the cauldron is told which weather fills it by a predicate, RAIN for the water one.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "<clinit>",
            at = @org.spongepowered.asm.mixin.injection.At(value = "NEW",
                    target = "(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;Ljava/util/function/Predicate;Ljava/util/Map;)Lnet/minecraft/world/level/block/LayeredCauldronBlock;"))
    private static net.minecraft.world.level.block.LayeredCauldronBlock thirst$markWaterCauldron(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties properties,
            java.util.function.Predicate<net.minecraft.world.level.biome.Biome.Precipitation> fillPredicate,
            java.util.Map<net.minecraft.world.item.Item, net.minecraft.core.cauldron.CauldronInteraction> interactions,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.level.block.LayeredCauldronBlock> original) {
        return com.thirstwastaken2.platform.Vanilla.buildingWaterCauldron(
                fillPredicate == net.minecraft.world.level.block.LayeredCauldronBlock.RAIN,
                () -> original.call(properties, fillPredicate, interactions));
    }
    *///?}
}
