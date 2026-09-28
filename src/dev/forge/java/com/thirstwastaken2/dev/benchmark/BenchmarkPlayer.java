package com.thirstwastaken2.dev.benchmark;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A server player that never joins: it is not in the player list or the level, never ticks on its own,
 * cannot be hurt, and every packet sent to it is dropped by Forge's fake connection. It still runs every
 * server-side path the mod hooks, which is all the benchmark needs. The counterpart of the Fabric copy,
 * under the same rules; the class the two extend is what differs, and nothing else in {@code benchmark/}
 * names a loader.
 *
 * <p>Constructed directly rather than through {@code FakePlayerFactory.get}, which caches instances, so each
 * run starts from a fresh entity instead of one still carrying the previous run's effects and attachments.
 * The UUID is derived from the index, so the stats and advancement objects vanilla caches per UUID are
 * reused by the next run instead of piling up until the server stops.
 *
 * <p>Forge, like NeoForge, may hand a fake player a throwaway {@code PlayerAdvancements} or cache a real
 * one per UUID, so {@code BenchmarkWorld.releasePlayers} still has work to do here: it drops
 * whatever ended up in the player list's maps, and drops nothing when nothing did.
 */
final class BenchmarkPlayer extends FakePlayer {
    BenchmarkPlayer(ServerLevel level, int index) {
        super(level, new GameProfile(uuid(index), "thirstbench" + index));
    }

    /** The same derivation on both loaders, so a report from either names the same simulated players. */
    private static UUID uuid(int index) {
        return UUID.nameUUIDFromBytes(("thirstwastaken2-benchmark:" + index).getBytes(StandardCharsets.UTF_8));
    }
}
