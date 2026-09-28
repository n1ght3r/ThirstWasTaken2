package com.thirstwastaken2.client.forge;

import com.thirstwastaken2.client.ThirstWasTaken2Client;
import com.thirstwastaken2.client.config.ThirstConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * The Forge client half, the counterpart of the Fabric {@code client} entrypoint, started by the mod
 * class on a client only. It also hands Forge's mods list the config screen, which Mod Menu finds
 * through its own entrypoint on Fabric.
 */
public final class ThirstWasTaken2ForgeClient {
    private ThirstWasTaken2ForgeClient() { }

    public static void initialize() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ThirstConfigScreen(parent)));
        ThirstWasTaken2Client.initialize();
    }
}
