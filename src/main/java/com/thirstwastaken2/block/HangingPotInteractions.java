package com.thirstwastaken2.block;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pouring water into a hanging pot, copper or iron, and drawing it back out.
 *
 * <p>The pot is the mod's own block, so unlike a cauldron nothing in vanilla handles these; everything
 * happens here, inline. A bucket is {@link HangingPotBlock#BUCKET} servings and everything else one.
 * Poured water mixes the way it does in a cauldron, keeping the worse grade, and adds its own boiling
 * time to what is left; see {@link HangingPotBlock#withPoured}.
 * Drawn water carries the pot's quality. Where water evaporates, as in the Nether, nothing can be
 * poured in at all. A sneaking player gets vanilla's usual behaviour instead,
 * except with a waterskin, whose sneak-use pours it out: over a pot it pours into the pot.
 */
public final class HangingPotInteractions {
    private static final int BLOCK_UPDATE_FLAGS = 3;

    private HangingPotInteractions() { }

    public static InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HangingPotBlock)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        boolean sneaking = player.isSecondaryUseActive();
        int servings = state.getValue(HangingPotBlock.LEVEL);
        int room = HangingPotBlock.CAPACITY - servings;

        if (WaterskinItem.is(held)) {
            int skin = WaterskinItem.servings(held);
            if (sneaking) {
                if (skin == 0 || room == 0) return InteractionResult.PASS;
                if (evaporates(level, pos)) return hissed(level);
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                int poured = Math.min(skin, room);
                WaterQuality quality = WaterPurity.quality(held);
                WaterskinItem.removeWater(held, poured);
                pour(player, level, pos, state, poured, quality, SoundEvents.BOTTLE_EMPTY);
                return InteractionResult.SUCCESS_SERVER;
            }
            if (servings == 0 || skin >= WaterskinItem.capacity(held)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            // Fills the skin in one draw, as far as the pot goes, like scooping from water.
            int drawn = Math.min(WaterskinItem.capacity(held) - skin, servings);
            WaterskinItem.addWater(held, HangingPotBlock.quality(state), drawn);
            draw(player, level, pos, state, drawn, SoundEvents.BOTTLE_FILL);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (sneaking) return InteractionResult.PASS;

        Transfer transfer = transfer(held, servings, room);
        if (transfer == null) return InteractionResult.PASS;
        if (transfer.pouring() && evaporates(level, pos)) return hissed(level);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack result;
        if (transfer.pouring()) {
            WaterQuality quality = WaterPurity.quality(held);
            result = transfer.empty();
            pour(player, level, pos, state, transfer.servings(), quality, transfer.sound());
        } else {
            result = WaterPurity.setQuality(transfer.filled(), HangingPotBlock.quality(state));
            draw(player, level, pos, state, transfer.servings(), transfer.sound());
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * What {@code held} does to a pot holding {@code servings} with {@code room} to spare, or
     * {@code null} when it does nothing. Filled stacks are built fresh on every call, since the one handed
     * out is stamped.
     */
    private static Transfer transfer(ItemStack held, int servings, int room) {
        if (held.is(Items.WATER_BUCKET)) {
            return room >= HangingPotBlock.BUCKET
                    ? Transfer.pour(HangingPotBlock.BUCKET, new ItemStack(Items.BUCKET), SoundEvents.BUCKET_EMPTY)
                    : null;
        }
        if (held.is(Items.POTION) && WaterPurity.isWaterContainer(held)) {
            return room >= 1 ? Transfer.pour(1, new ItemStack(Items.GLASS_BOTTLE), SoundEvents.BOTTLE_EMPTY) : null;
        }
        if (held.is(ThirstItems.TERRACOTTA_WATER_BOWL)) {
            return room >= 1
                    ? Transfer.pour(1, new ItemStack(ThirstItems.TERRACOTTA_BOWL), SoundEvents.BUCKET_EMPTY)
                    : null;
        }
        if (held.is(Items.BUCKET)) {
            return servings >= HangingPotBlock.BUCKET
                    ? Transfer.draw(HangingPotBlock.BUCKET, new ItemStack(Items.WATER_BUCKET), SoundEvents.BUCKET_FILL)
                    : null;
        }
        if (held.is(Items.GLASS_BOTTLE)) {
            return servings >= 1
                    ? Transfer.draw(1, Vanilla.waterBottle(), SoundEvents.BOTTLE_FILL)
                    : null;
        }
        if (held.is(ThirstItems.TERRACOTTA_BOWL)) {
            return servings >= 1
                    ? Transfer.draw(1, new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), SoundEvents.BUCKET_FILL)
                    : null;
        }
        return null;
    }

    /**
     * Whether water poured into a pot at {@code pos} boils away, as it does in the Nether. On the server it
     * hisses and smokes the way a bucket emptied there does, but the player keeps the water: the pot
     * refuses it rather than eating it. Drawing needs no such check, because a pot there never has
     * anything in it to draw.
     */
    static boolean evaporates(Level level, BlockPos pos) {
        if (!Vanilla.waterEvaporates(level, pos)) return false;
        if (level instanceof ServerLevel server) {
            server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                    2.6F + (server.getRandom().nextFloat() - server.getRandom().nextFloat()) * 0.8F);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.2, 0.1, 0.2, 0.0);
        }
        return true;
    }

    /** Ends a refused pour on each side, so the click is spent and the player swings. */
    static InteractionResult hissed(Level level) {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private static void pour(Player player, Level level, BlockPos pos, BlockState state, int servings,
                             WaterQuality poured, SoundEvent sound) {
        level.setBlock(pos, HangingPotBlock.withPoured(state, servings, poured), BLOCK_UPDATE_FLAGS);
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    private static void draw(Player player, Level level, BlockPos pos, BlockState state, int servings,
                             SoundEvent sound) {
        level.setBlock(pos, HangingPotBlock.withLess(state, servings), BLOCK_UPDATE_FLAGS);
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
    }

    /**
     * One pour or draw: how many servings move, the container the player is left holding when pouring
     * ({@code empty}) or the one that is stamped and handed out when drawing ({@code filled}).
     */
    private record Transfer(boolean pouring, int servings, ItemStack empty, ItemStack filled, SoundEvent sound) {
        static Transfer pour(int servings, ItemStack empty, SoundEvent sound) {
            return new Transfer(true, servings, empty, ItemStack.EMPTY, sound);
        }

        static Transfer draw(int servings, ItemStack filled, SoundEvent sound) {
            return new Transfer(false, servings, ItemStack.EMPTY, filled, sound);
        }
    }
}
