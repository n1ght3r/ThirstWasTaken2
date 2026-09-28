package com.thirstwastaken2.gametest.platform;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * A simulated player's connection that reports the mod's channel, which this loader has no way to
 * produce, so on Forge there is none.
 *
 * <p>Forge 47 asks {@code SimpleChannel.isRemotePresent}, which reads the channels negotiated during
 * login off an attribute of the connection's Netty channel. A simulated player never logs in, and
 * writing that attribute means reaching into FML's handshake data. The check this belongs to is
 * NeoForge's own sync predicate, which the NeoForge nodes make; see the Fabric copy of this class.
 */
public final class CapturingConnection {
    private CapturingConnection() { }

    /** Whether this loader can report a negotiated channel from a test connection. */
    public static boolean available() {
        return false;
    }

    /** Why it cannot, for the test to log when {@link #available} is false. */
    public static String unavailable() {
        return "Forge reads the negotiated channels off the connection's Netty channel, which only a real "
                + "client's login fills in; per-player sync is checked on the NeoForge nodes and with two "
                + "agent clients instead";
    }

    /** Never called on this loader: {@link #available} is false. */
    public static void install(ServerPlayer player, Consumer<Packet<?>> sink) {
        throw new UnsupportedOperationException(unavailable());
    }
}
