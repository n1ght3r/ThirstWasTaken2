package com.thirstwastaken2.block;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Filling a cooling tub with its coolant, alone or as the distiller's tub half. Any water does, sea
 * water too, and whatever grade: it only carries heat off the coil and never reaches the distillate,
 * so it costs no drinking water. It is poured once and stays. A bucket, a bottle or a terracotta bowl
 * empties into it; a waterskin, canteen or flask pours what it holds, up to a bucket. Where water
 * evaporates, as in the Nether, the tub refuses it the way a hanging pot does.
 */
public final class DistillerInteractions {
    private DistillerInteractions() { }

    public static InteractionResult fillTub(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        boolean tub = state.is(ThirstBlocks.COOLING_TUB)
                || state.is(ThirstBlocks.COPPER_DISTILLER) && state.getValue(DistillerBlock.PART) == DistillerBlock.Part.TUB;
        if (!tub || state.getValue(DistillerBlock.COOLED) || player.isSpectator()) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        Pour pour = pour(held);
        if (pour == null) return InteractionResult.PASS;
        if (HangingPotInteractions.evaporates(level, pos)) return HangingPotInteractions.hissed(level);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (pour.empty() == null) {
            WaterskinItem.removeWater(held, Math.min(WaterskinItem.servings(held), WaterskinItem.BUCKET_SERVINGS));
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, pour.empty()));
        }
        level.setBlock(pos, state.setValue(DistillerBlock.COOLED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, pour.sound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** What {@code held} pours into a tub, or {@code null} when it holds no water. */
    private static Pour pour(ItemStack held) {
        if (held.is(Items.WATER_BUCKET)) return new Pour(new ItemStack(Items.BUCKET), SoundEvents.BUCKET_EMPTY);
        if (held.is(Items.POTION) && WaterPurity.isWaterContainer(held)) {
            return new Pour(new ItemStack(Items.GLASS_BOTTLE), SoundEvents.BOTTLE_EMPTY);
        }
        if (held.is(ThirstItems.TERRACOTTA_WATER_BOWL)) {
            return new Pour(new ItemStack(ThirstItems.TERRACOTTA_BOWL), SoundEvents.BUCKET_EMPTY);
        }
        if (WaterskinItem.is(held) && WaterskinItem.servings(held) > 0) return new Pour(null, SoundEvents.BOTTLE_EMPTY);
        return null;
    }

    /** The container left once poured, {@code null} for a vessel that keeps what it does not pour. */
    private record Pour(ItemStack empty, SoundEvent sound) { }
}
