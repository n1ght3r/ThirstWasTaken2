package com.thirstwastaken2.client.kaleidoscopetavern;

import com.thirstwastaken2.client.compat.JadeIntegration;
import com.thirstwastaken2.kaleidoscopetavern.BarrelLookup;
import com.thirstwastaken2.kaleidoscopetavern.KaleidoscopeTavernPresence;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Tells the mod's Jade plugin how to read the water in a Kaleidoscope Tavern barrel, which keeps its grade
 * on the one block entity its eight blocks share, so the line shows whichever part is looked at. The
 * overlay and its one entry in Jade's settings stay {@code client/compat/JadeIntegration}'s, beside the
 * mod's own barrel lines.
 *
 * <p>Jade loads this whether or not Kaleidoscope Tavern is installed, so it names no class of the mod's:
 * it asks the gate, then hands over through a static call into {@link BarrelLookup}, which is only loaded
 * when that call first runs.
 */
@WailaPlugin
public final class KaleidoscopeTavernJade implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (!KaleidoscopeTavernPresence.isInstalled()) return;
        JadeIntegration.addBlock(BarrelLookup::heldWater);
    }
}
