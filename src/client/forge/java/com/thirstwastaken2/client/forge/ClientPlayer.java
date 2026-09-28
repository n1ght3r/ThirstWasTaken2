package com.thirstwastaken2.client.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * The client's own player, for {@code ForgeNetworking}'s clientbound handlers, which are declared in
 * common code. Only a handler running on a client calls it, so a dedicated server never loads it.
 */
public final class ClientPlayer {
    private ClientPlayer() { }

    public static Player get() {
        return Minecraft.getInstance().player;
    }
}
