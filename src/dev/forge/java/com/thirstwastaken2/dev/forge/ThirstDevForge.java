package com.thirstwastaken2.dev.forge;

import com.thirstwastaken2.dev.ThirstDev;
import com.thirstwastaken2.dev.ThirstDevClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * The dev tools mod on Forge: the agent's queue, polled on the server tick on a dedicated server and on
 * the client tick on a client, and {@code /thirst benchmark} on either. Forge 47 has one mod class per
 * mod rather than one per side, so this asks which side it is on. Both tools are installed from
 * {@link ThirstDev}, which names no loader.
 */
@Mod(ThirstDevForge.MOD_ID)
public final class ThirstDevForge {
    /** Forge mod ids cannot contain a hyphen, so this is not the Fabric mod's {@code -dev} spelling. */
    public static final String MOD_ID = "thirstwastaken2_dev";

    public ThirstDevForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) ThirstDevClient.initialize();
        else ThirstDev.initializeServer();
        ThirstDev.installBenchmark();
    }
}
