package com.thirstwastaken2.coldsweatforge;

import com.thirstwastaken2.platform.IntegrationEntrypoint;

/**
 * Run by the Forge mod class while it is constructed, compiled only on the Forge nodes that set
 * {@code deps.cold_sweat}. Forge 47 takes one {@code @Mod} class per mod id, so this is an
 * {@link IntegrationEntrypoint} rather than a second one as on NeoForge. It names no Cold Sweat class and
 * hands over to {@link ColdSweatClimate} only after the presence check, so nothing of Cold Sweat's is
 * loaded without it.
 */
@IntegrationEntrypoint
public final class ColdSweatEntrypoint implements Runnable {
    @Override
    public void run() {
        if (ColdSweatPresence.isPresent()) ColdSweatClimate.install();
    }
}
