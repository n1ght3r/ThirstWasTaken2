package com.thirstwastaken2.purity;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Queue;

/** Purity-aware handling of the interactions vanilla performs on water blocks and cauldrons. */
public final class WaterInteractions {
    /**
     * Cauldron fills and drains are resolved by vanilla after our callback returns, so the purity
     * transfer has to be applied once the block and the resulting item have settled. Only ever
     * touched from the server thread.
     */
    private static final Queue<Runnable> END_OF_TICK = new ArrayDeque<>();
    /** Block update + client notify, matching what vanilla cauldron interactions use. */
    private static final int BLOCK_UPDATE_FLAGS = 3;
    /** As many as vanilla shows for a bottle poured on dirt. */
    private static final int SPLASH_PARTICLES = 5;

    private WaterInteractions() { }

    /**
     * Lets the terracotta bowl, waterskin, canteen and flask scoop from any water, including flowing water. A carried
     * container is filled in one go rather than a serving per click: a lake has no levels to lower.
     */
    public static InteractionResult fillFromWater(Player player, Level level, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean bowl = held.is(ThirstItems.TERRACOTTA_BOWL);
        boolean waterskin = WaterskinItem.hasRoom(held);
        if (!bowl && !waterskin) return InteractionResult.PASS;

        BlockHitResult hit = pick(player, level, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = hit.getBlockPos();
        WaterQuality quality = WaterPurity.sampleAt(level, pos);
        if (bowl) {
            ItemStack filled = WaterPurity.setQuality(
                    new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
        } else {
            WaterskinItem.addWater(held, quality, WaterskinItem.capacity(held));
        }
        level.playSound(null, player.blockPosition(), bowl ? SoundEvents.BUCKET_FILL : SoundEvents.BOTTLE_FILL,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Draws as many servings as the waterskin has room for from a water cauldron, one vanilla layer
     * each, so a full cauldron fills an empty skin in one click.
     */
    public static InteractionResult fillWaterskinFromCauldron(Player player, Level level, InteractionHand hand,
                                                               BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!WaterskinItem.hasRoom(held)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        int drawn = Math.min(WaterskinItem.capacity(held) - WaterskinItem.servings(held),
                state.getValue(LayeredCauldronBlock.LEVEL));
        // Sampled before lowering: an emptied cauldron no longer holds the purity.
        WaterskinItem.addWater(held, WaterPurity.sampleAt(level, pos), drawn);
        for (int i = 0; i < drawn; i++) LayeredCauldronBlock.lowerFillLevel(level.getBlockState(pos), level, pos);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Sneak-using a filled waterskin on a block pours away all of its stored water, with the splash,
     * droplets and sound of a water bottle poured on dirt.
     */
    public static InteractionResult emptyWaterskinOnBlock(Player player, Level level, InteractionHand hand,
                                                           BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isCrouching() || !WaterskinItem.is(held) || WaterskinItem.servings(held) == 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        WaterskinItem.removeWater(held, WaterskinItem.servings(held));
        BlockPos pos = hit.getBlockPos();
        splash(level, pos);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** What {@code PotionItem#useOn} plays and shows when a water bottle turns dirt into mud. */
    private static void splash(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            for (int i = 0; i < SPLASH_PARTICLES; i++) {
                server.sendParticles(ParticleTypes.SPLASH, pos.getX() + level.getRandom().nextDouble(), pos.getY() + 1,
                        pos.getZ() + level.getRandom().nextDouble(), 1, 0.0, 0.0, 0.0, 1.0);
            }
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * Mirrors the original {@code fillablesHandler}: pouring a container into a cauldron stores the
     * worse of the two purities, and drawing from a cauldron stamps the resulting container.
     *
     * <p>Only when vanilla, or the mod that owns the container, actually did something: the callback
     * runs first and cannot tell whether a container will be accepted. A terracotta water bowl, or Miner's
     * Delight's water cup on a full cauldron, has no cauldron interaction at all; a bottle on a full
     * cauldron or a bucket on one that is not full has one that refuses. Storing a grade then turned a
     * Pure cauldron Dirty with nothing poured, or stamped some other bottle in the inventory. The first
     * kind is skipped here; the second at the end of the tick, by {@link #interacted}.
     */
    public static InteractionResult transferCauldronPurity(Player player, Level level, InteractionHand hand,
                                                           BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        BlockState before = level.getBlockState(pos);
        boolean cauldron = before.is(Blocks.CAULDRON) || before.is(Blocks.WATER_CAULDRON);
        if (!cauldron) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        // Waterskins draw from cauldrons in fillWaterskinFromCauldron; vanilla has no matching
        // interaction that could actually pour them back, so do not schedule a phantom transfer.
        if (WaterskinItem.is(held)) return InteractionResult.PASS;
        boolean filling = WaterPurity.isWaterContainer(held);
        boolean draining = before.is(Blocks.WATER_CAULDRON) && WaterPurity.drawsFromCauldron(held);
        if (!filling && !draining) return InteractionResult.PASS;
        // A container the cauldron has no interaction for does nothing to it, whatever happens next: a
        // water cup on a full cauldron goes on to place its water in the world.
        if (!Vanilla.cauldronHasInteraction(before.is(Blocks.WATER_CAULDRON), held)) return InteractionResult.PASS;

        WaterQuality quality = filling ? WaterPurity.quality(held) : WaterPurity.sampleAt(level, pos);
        if (filling) {
            WaterQuality stored = WaterPurity.storedQuality(before);
            if (stored != null) quality = WaterQuality.worse(quality, stored);
        }

        WaterQuality transferred = quality;
        Item heldItem = held.getItem();
        int heldCount = held.getCount();
        END_OF_TICK.add(() -> {
            if (!interacted(player, hand, level, pos, before, heldItem, heldCount)) return;
            if (filling) {
                storeInCauldron(level, pos, transferred);
            } else {
                stampDrawnContainer(player, hand, transferred);
            }
        });
        return InteractionResult.PASS;
    }

    /**
     * Whether an interaction the cauldron has went through: the cauldron changed, or the stack in the
     * hand did. Every pour or draw changes one of them. Pouring resets the stored grade to unset or
     * changes the level; drawing lowers the level or empties the cauldron; and outside creative the
     * container in the hand is swapped or shrinks. Blockstates are canonical instances, so an unchanged
     * block is the same object. The hand alone would not do without asking for the interaction first: an
     * item the cauldron does nothing with can go on to its own use in the same tick, as the water cup does.
     */
    private static boolean interacted(Player player, InteractionHand hand, Level level, BlockPos pos,
                                      BlockState before, Item heldItem, int heldCount) {
        if (level.getBlockState(pos) != before) return true;
        ItemStack now = player.getItemInHand(hand);
        return !now.is(heldItem) || now.getCount() != heldCount;
    }

    /**
     * A cauldron vanilla filled from the sky. Rain is clean but open to whatever it falls through,
     * so it lands on its own configured grade rather than inheriting {@code defaultPurity}.
     */
    public static void filledByRain(BlockState before, Level level, BlockPos pos) {
        // Switched off, rain is vanilla's again: the cauldron fills, and the water carries no grade.
        if (!ThirstConfig.get().enableRainCollection) return;
        naturallyFilled(before, level, pos, WaterPurity.rainwaterPurity());
    }

    /**
     * A cauldron a pointed dripstone dripped into. The water has seeped through stone to get there,
     * which is slow, needs a build to arrange, and is the cleanest water the world hands out for
     * free. Dripstone also drips lava, which reaches no cauldron this mod grades.
     */
    public static void filledByDripstone(BlockState before, Level level, BlockPos pos, Fluid fluid) {
        // Identity, not the water tag: vanilla's own drip check compares against this instance, and
        // Fluid#is(TagKey) is deprecated.
        if (fluid == Fluids.WATER) {
            naturallyFilled(before, level, pos, WaterPurity.dripstonePurity());
        }
    }

    /**
     * Stores the grade of water that arrived on its own, with nobody pouring anything in.
     *
     * <p>Both hooks run whether or not vanilla decided to add a layer - rain only fills a cauldron
     * on a fraction of its chances, and a full one never fills - so the block having changed at all
     * is the only proof water was added. The cauldron then keeps the worse of what it already held
     * and what fell into it, exactly like pouring a container in.
     */
    private static void naturallyFilled(BlockState before, Level level, BlockPos pos, int grade) {
        if (level.isClientSide()) return;
        BlockState after = level.getBlockState(pos);
        // Blockstates are canonical instances, so an unchanged block is the same object.
        if (after == before || !after.hasProperty(WaterPurity.BLOCK_PURITY)) return;

        WaterQuality stored = WaterPurity.storedQuality(after);
        WaterQuality quality = WaterQuality.fresh(grade);
        if (stored != null) quality = WaterQuality.worse(stored, quality);
        if (quality.equals(stored)) return;
        storeInCauldron(level, pos, quality);
    }

    public static void tick(MinecraftServer server) {
        Runnable action;
        while ((action = END_OF_TICK.poll()) != null) action.run();
    }

    private static void storeInCauldron(Level level, BlockPos pos, WaterQuality quality) {
        BlockState after = level.getBlockState(pos);
        if (!after.hasProperty(WaterPurity.BLOCK_PURITY)) return;
        level.setBlock(pos, after.setValue(WaterPurity.BLOCK_PURITY, WaterPurity.storedValue(quality)),
                BLOCK_UPDATE_FLAGS);
    }

    /**
     * The filled container does not necessarily end up back in the interaction hand - a stacked
     * bottle sends the water bottle to the first free inventory slot - so the first freshly created,
     * still unstamped container wins.
     */
    private static void stampDrawnContainer(Player player, InteractionHand hand, WaterQuality quality) {
        if (stamp(player.getItemInHand(hand), quality)) return;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (stamp(inventory.getItem(slot), quality)) return;
        }
    }

    private static boolean stamp(ItemStack stack, WaterQuality quality) {
        if (stack.isEmpty() || WaterPurity.isStamped(stack) || !WaterPurity.isWaterContainer(stack)) {
            return false;
        }
        WaterPurity.setQuality(stack, quality);
        return true;
    }

    private static BlockHitResult pick(Player player, Level level, ClipContext.Fluid fluidMode) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(Vanilla.blockReach(player)));
        return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, fluidMode, player));
    }
}
