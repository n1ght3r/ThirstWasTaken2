package com.thirstwastaken2.purity;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.config.WaterSourceUse;
import com.thirstwastaken2.platform.Clientbound;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Whether scooping water from the world takes the source block, as a bucket does, for a world whose
 * water is finite. Then a bottle, bowl, waterskin, canteen or flask fills only from a source it can take
 * away, the way a bucket can, and takes it; a carried vessel draws a bucket's three servings from it.
 * Off, water is drawn from any water block and the world keeps it.
 *
 * <p>The client has to agree with the server about it: both cast the same ray and decide alike, or the
 * client would show a bottle filled from flowing water that the server refused, and vanilla never sends
 * an unchanged hand back. The client cannot read the server's game rules, so the server sends the answer
 * on join and after a reload, and again whenever it changes, checked once a second.
 *
 * <p>Thirsty Bottles makes a bottle take its source already. With it installed, bottles are left to it
 * and to vanilla's source-only ray, so a source is never taken twice and flowing water is not a way
 * around it.
 */
public final class SourceDrawing {
    public static final Identifier PAYLOAD_ID = ThirstWasTaken2.id("water_source_use");
    private static final boolean THIRSTY_BOTTLES = Loader.isModLoaded("thirstybottles");
    private static final int CHECK_EVERY_TICKS = 20;

    private static Clientbound<Payload> channel;
    /** What the server last told this client. */
    private static volatile boolean clientTakes;
    /** What the server last sent everyone; only touched on the server thread. */
    private static boolean sent;
    private static int ticks;

    /** The server's answer, a single flag. */
    public record Payload(boolean takes) {
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBoolean(takes);
        }

        public static Payload read(FriendlyByteBuf buffer) {
            return new Payload(buffer.readBoolean());
        }
    }

    private SourceDrawing() { }

    /** Declares {@link Payload}, on both sides, during {@code initialize}. */
    public static void registerPayload() {
        channel = Loader.clientboundPayload(PAYLOAD_ID, Payload::write, Payload::read,
                payload -> clientTakes = payload.takes());
    }

    /** Whether scooping water in {@code level} takes the source: the config, or the server's word on a client. */
    public static boolean takesSource(Level level) {
        if (level.isClientSide()) return clientTakes;
        return takesSource(ThirstConfig.get().waterFillTakesSource, Vanilla.waterSourceConversion((ServerLevel) level));
    }

    /** The setting against the game rule. Public for the gametests. */
    public static boolean takesSource(WaterSourceUse use, boolean waterSourceConversion) {
        return switch (use) {
            case ON -> true;
            case OFF -> false;
            case AUTO -> !waterSourceConversion;
        };
    }

    /**
     * Whether {@code player} may take the water at {@code pos}: a water source in a block a bucket empties,
     * where the player may change the world. Water in kelp or seagrass is a source no bucket takes.
     */
    public static boolean canTake(Player player, Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER) && fluid.isSource()
                && level.getBlockState(pos).getBlock() instanceof BucketPickup
                && level.mayInteract(player, pos);
    }

    /** Takes the water source at {@code pos}, and whether it was there to take. */
    public static boolean take(Player player, Level level, BlockPos pos) {
        return Vanilla.pickupFluid(player, level, pos);
    }

    /** Whether a glass bottle takes its source: never while Thirsty Bottles does it. */
    private static boolean bottleTakesSource(Level level) {
        return !THIRSTY_BOTTLES && takesSource(level);
    }

    /**
     * The ray a glass bottle casts: any water, flowing too, unless the source is taken, here or by
     * Thirsty Bottles, when it is vanilla's sources only.
     */
    public static ClipContext.Fluid bottleRay(Level level) {
        return THIRSTY_BOTTLES || takesSource(level) ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.ANY;
    }

    /**
     * A bottle's hit as vanilla goes on to use it: a miss, so the bottle stays empty, on a source it
     * cannot take away. Decided alike on both sides, so the client never shows a fill the server refused.
     */
    public static BlockHitResult bottleHit(Level level, Player player, BlockHitResult hit) {
        if (!bottleTakesSource(level) || hit.getType() != HitResult.Type.BLOCK) return hit;
        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER) || canTake(player, level, pos)) return hit;
        return BlockHitResult.miss(hit.getLocation(), hit.getDirection(), pos);
    }

    /** Takes the source a glass bottle was just filled from. */
    public static void bottleFilled(Level level, Player player, BlockPos pos) {
        if (!level.isClientSide() && bottleTakesSource(level)) take(player, level, pos);
    }

    /** Tells one player the server's answer; on join and after every reload. */
    public static void sync(ServerPlayer player) {
        if (channel != null) channel.send(player, new Payload(takesSource(player.level())));
    }

    /** Tells every player again once the answer changes, from a config edit or a game rule. */
    public static void tick(MinecraftServer server) {
        if (++ticks < CHECK_EVERY_TICKS) return;
        ticks = 0;
        boolean takes = takesSource(server.overworld());
        if (takes == sent) return;
        sent = takes;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
    }
}
