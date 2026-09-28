package com.thirstwastaken2.gametest.platform;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Makes the mock player every test joins to the level, for Forge: vanilla's own, less one flaw.
 *
 * <p>Minecraft 1.20.1 joins its mock player on a connection with no Netty channel behind it, and Forge 47,
 * as the player joins, adds its packet filters to that channel's pipeline, which throws. So this builds the
 * same player the way {@code makeMockServerPlayerInLevel} does and puts the connection on an
 * {@link EmbeddedChannel} first, as later Minecraft versions do themselves. Nothing is read from the
 * channel; what is sent to it stays in its outbound buffer.
 */
public final class MockPlayers {
    private MockPlayers() { }

    public static ServerPlayer create(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
    }
}
