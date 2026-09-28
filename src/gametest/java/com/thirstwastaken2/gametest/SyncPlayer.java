package com.thirstwastaken2.gametest;

import com.mojang.authlib.GameProfile;
import com.thirstwastaken2.gametest.platform.CapturingConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player that is really in the level and is really tracked, but whose connection goes nowhere: every
 * packet sent to it is kept in a list instead.
 *
 * <p>This is not the benchmark's simulated player. That one is deliberately outside the level and
 * untracked, which is right for measuring server cost and wrong for anything synced: the loader works
 * out who to tell from who is watching, so a player nobody watches can never show that the mod told
 * the wrong person. These are added to the level so that they do watch each other, which is what makes
 * "only the owner is told" a claim a test can fail.
 *
 * <p>They cost what a real player costs, so a test makes a handful and removes them again.
 */
final class SyncPlayer extends ServerPlayer {
    /** A chunk section's height: how far one of these is moved to make the chunk map look at it again. */
    private static final int SECTION = 16;

    private final List<Packet<?>> captured = new ArrayList<>();

    private SyncPlayer(ServerLevel level, int index) {
        //? if >=1.20.5 {
        super(level.getServer(), level, profile(index), net.minecraft.server.level.ClientInformation.createDefault());
        //?} else {
        /*super(level.getServer(), level, profile(index));
        *///?}
        CapturingConnection.install(this, captured::add);
    }

    /** Placed at {@code at} and added to the level, so the chunk map starts tracking them. */
    static List<SyncPlayer> place(ServerLevel level, BlockPos at, int count) {
        List<SyncPlayer> players = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            SyncPlayer player = new SyncPlayer(level, index);
            player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
            level.addNewPlayer(player);
            players.add(player);
        }
        return players;
    }

    static void remove(ServerLevel level, List<SyncPlayer> players) {
        for (SyncPlayer player : players) {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * Does for these players, once a tick, the two things the server does for the players in its player
     * list — which these are not in — and that together decide whether anyone ends up watching anyone.
     *
     * <p>The first is sending the chunks their tracking view is waiting on, and acknowledging the batch
     * the way a real client would. Until those are sent, {@code ChunkMap.isChunkTracked} says no, so
     * every {@code updatePlayer} refuses.
     *
     * <p>The second is moving. {@code ChunkMap.addEntity} does ask, once, who can see a new player, but
     * that happens before a single chunk has been sent, so the answer is nobody; afterwards
     * {@code ChunkMap.tick} only asks again for an entity whose section has changed since the last
     * tick. Three players standing still therefore never enter each other's {@code seenBy}, the sync
     * list stays {@code [self]} whatever the mod's predicate says, and a test built on them proves
     * nothing. So each one is moved a section up and back down again, one step a tick: vertical, so the
     * chunk they are in — and hence what has been sent to them — never changes, while the section this
     * is read from does.
     */
    static void settle(List<SyncPlayer> players, BlockPos at, int step) {
        double y = at.getY() + (step % 2 == 0 ? 0 : SECTION);
        for (SyncPlayer player : players) {
            // Before 1.20.2 chunks are sent as the chunk map loads them, with no batches to acknowledge.
            //? if >=1.20.5 {
            player.connection.chunkSender.sendNextChunks(player);
            player.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
            //?}
            player.snapTo(at.getX() + 0.5, y, at.getZ() + 0.5, 0.0F, 0.0F);
        }
    }

    /**
     * Whether the tracking the fixture depends on has actually happened, asked through the mechanism the
     * sync itself uses: a level broadcast reaches the entity and everyone watching it, so a broadcast
     * about one of these players that reaches all of them means each is in the others' {@code seenBy}.
     * Fewer than that and the fixture cannot show an over-broad sync, and has to fail rather than pass.
     */
    static int reachedByBroadcastAbout(ServerLevel level, List<SyncPlayer> players, SyncPlayer about) {
        players.forEach(SyncPlayer::clear);
        // Any clientbound event would do; 35 is the totem animation, and nothing here has a client to
        // play it. What is read is who the packet was handed to, never what it says.
        level.broadcastEntityEvent(about, (byte) 35);
        int reached = 0;
        for (SyncPlayer player : players) {
            if (!player.captured.isEmpty()) reached++;
        }
        players.forEach(SyncPlayer::clear);
        return reached;
    }

    /**
     * How many of {@code players} the chunk map has watching {@code at}. The loader builds its sync
     * list from exactly this, so a test that reads fewer than it placed is not testing anything and
     * has to say so. Only these players are counted: a gametest server has mock players of other
     * tests wandering about, and they are not the fixture.
     */
    static int watching(ServerLevel level, BlockPos at, List<SyncPlayer> players) {
        List<ServerPlayer> watchers =
                level.getChunkSource().chunkMap.getPlayers(level.getChunkAt(at).getPos(), false);
        int counted = 0;
        for (SyncPlayer player : players) {
            if (watchers.contains(player)) counted++;
        }
        return counted;
    }

    /** The custom payloads sent to this player since the last {@link #clear}. */
    List<String> payloads() {
        List<String> payloads = new ArrayList<>();
        for (Packet<?> packet : captured) {
            //? if >=1.20.5 {
            if (packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket payload) {
                payloads.add(payload.payload().type().id().toString());
            }
            //?} else {
            /*if (packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket payload) {
                // The channel is the first thing the packet writes.
                net.minecraft.network.FriendlyByteBuf buffer =
                        new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                payload.write(buffer);
                payloads.add(buffer.readIdentifier().toString());
            }
            *///?}
        }
        return payloads;
    }

    /** The payload ids sent to this player, for a failure message that names what actually arrived. */
    String payloadIds() {
        List<String> ids = payloads();
        return ids.isEmpty() ? "nothing" : String.join(", ", ids);
    }

    void clear() {
        captured.clear();
    }

    String who() {
        return getScoreboardName();
    }

    /** Never ticked: these exist to be sent to, not to play. */
    @Override
    public void tick() {
    }

    //? if >=1.20.5 {
    @Override
    public void updateOptions(net.minecraft.server.level.ClientInformation settings) {
    }
    //?}

    /** Derived from the index, so a run reuses the previous run's stats and advancements rather than piling up. */
    private static GameProfile profile(int index) {
        UUID uuid = UUID.nameUUIDFromBytes(("thirstwastaken2-sync:" + index).getBytes(StandardCharsets.UTF_8));
        return new GameProfile(uuid, "thirstsync" + index);
    }
}
