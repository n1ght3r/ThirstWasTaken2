package com.thirstwastaken2.dev.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Every call into the mod loader the dev tools make and {@code com.thirstwastaken2.platform.Loader}
 * does not, for Forge. The counterpart of the Fabric copy, under the same rules.
 */
public final class DevLoader {
    /** Which loader this build compiled, for the agent's {@code probe} answer. */
    public static final String LOADER = "forge";

    private DevLoader() { }

    /** The run directory: {@code run/<node>} for a server or a client, one per node. */
    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    /** The version of the mod this is loaded beside, as the loader reports it. */
    public static String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    /** Whether another mod is loaded, which the benchmark asks about Create Fly. */
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** Runs once the server is accepting commands: a dedicated one, or a client's integrated one. */
    public static void onServerStarted(Consumer<MinecraftServer> handler) {
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> handler.accept(event.getServer()));
    }

    /** Runs as the server shuts down, while the world is still there. */
    public static void onServerStopping(Consumer<MinecraftServer> handler) {
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> handler.accept(event.getServer()));
    }
}
