package com.thirstwastaken2.gametest.platform;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Makes the mock player every test joins to the level, for NeoForge: vanilla's own, as it is. The Forge copy
 * says why it cannot be vanilla's there.
 */
public final class MockPlayers {
    private MockPlayers() { }

    @SuppressWarnings("removal")
    public static ServerPlayer create(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }
}
