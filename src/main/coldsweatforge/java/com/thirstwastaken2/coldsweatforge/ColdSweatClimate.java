package com.thirstwastaken2.coldsweatforge;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.data.ThirstManager;
import net.minecraft.world.entity.player.Player;

/**
 * Cold Sweat's world temperature in place of the biome's in the thirst drain, as on NeoForge: what Cold
 * Sweat measures around the player, in the same units as {@code Biome#getBaseTemperature}, so the climate
 * curve takes it unchanged.
 *
 * <p>Read at most once a second per player: the drain caches its modifier for 20 ticks.
 */
public final class ColdSweatClimate {
    private ColdSweatClimate() { }

    static void install() {
        ThirstManager.setClimateTemperature(ColdSweatClimate::worldTemperature);
        ThirstWasTaken2.LOGGER.info("Cold Sweat found, thirst follows its world temperature");
    }

    /**
     * NaN for a player Cold Sweat keeps no temperature for, who then drains by the biome as without the
     * mod. {@code Temperature.get} would answer 0 there, which reads as freezing. On Forge 47 the
     * capability is a {@code LazyOptional}.
     */
    private static double worldTemperature(Player player) {
        return EntityTempManager.getTemperatureCap(player)
                .map(cap -> cap.getTrait(Temperature.Trait.WORLD))
                .orElse(Double.NaN);
    }
}
