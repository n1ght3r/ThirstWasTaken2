package com.thirstwastaken2.youkaisfeasts.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
//?} else {
/*import net.minecraft.world.InteractionResult;
*///?}

/**
 * The Kettle and the Fermentation Tank take no sea water. Both pour a held container into their tank
 * through {@code FluidItemTile.addFluidOrItem}: a bucket through NeoForge's bucket wrapper (Forge's on
 * 1.20.1), which hands over plain water whatever the bucket held, and a water bottle as 250 mB of plain
 * water. Tea, sake, wine and mead all restore thirst, so without this the sea brewed a safe drink, as it
 * would in Brewin' and Chewin's keg. Fresh water of any grade still goes in: the drink is its own item.
 *
 * <p>The salty stack is refused at that call alone, with {@code CONSUME}, so it is neither poured nor put
 * in an item slot, and nothing else the blocks do by hand changes. {@code addFluidOrItem} is a static
 * method of an interface, which Forge 47's Mixin cannot inject into, hence its two callers. Named by
 * string, since nothing compiles against the mod; on 1.20.1 the method is L2 Modular Blocks' own
 * {@code onClick}, never remapped, and the descriptor names only Mojang class names, which Forge keeps.
 */
@Mixin(targets = {"dev.xkmc.youkaishomecoming.content.pot.kettle.KettleBlock",
        "dev.xkmc.youkaishomecoming.content.pot.ferment.FermentationTankBlock"}, remap = false)
abstract class FluidTileSeaWaterMixin {
    //? if >=1.20.5 {
    @Inject(method = "useItemOn", cancellable = true, at = @At(value = "INVOKE",
            target = "Ldev/xkmc/youkaishomecoming/content/pot/base/FluidItemTile;addFluidOrItem(Ldev/xkmc/youkaishomecoming/content/pot/base/FluidItemTile;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/ItemInteractionResult;"))
    private void thirst$noSeaWaterInTank(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                         InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (WaterPurity.isSalty(stack)) cir.setReturnValue(ItemInteractionResult.CONSUME);
    }
    //?} else {
    /*@Inject(method = "onClick", cancellable = true, at = @At(value = "INVOKE",
            target = "Ldev/xkmc/youkaishomecoming/content/pot/base/FluidItemTile;addFluidOrItem(Ldev/xkmc/youkaishomecoming/content/pot/base/FluidItemTile;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private void thirst$noSeaWaterInTank(BlockState state, Level level, BlockPos pos, Player player,
                                         InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (WaterPurity.isSalty(player.getItemInHand(hand))) cir.setReturnValue(InteractionResult.CONSUME);
    }
    *///?}
}
