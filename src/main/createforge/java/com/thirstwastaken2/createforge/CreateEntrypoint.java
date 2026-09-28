package com.thirstwastaken2.createforge;

import com.thirstwastaken2.platform.IntegrationEntrypoint;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Run by the Forge mod class while it is constructed, compiled only on the nodes that set
 * {@code deps.create}. Forge 47 takes one {@code @Mod} class per mod id, so this is an
 * {@link IntegrationEntrypoint} rather than a second one as on NeoForge. It references
 * {@link SandFilter} only after the presence check, so the classes that extend Create's are never
 * loaded without Create.
 */
@IntegrationEntrypoint
public final class CreateEntrypoint implements Runnable {
    @Override
    public void run() {
        if (CreatePresence.isPresent()) SandFilter.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
