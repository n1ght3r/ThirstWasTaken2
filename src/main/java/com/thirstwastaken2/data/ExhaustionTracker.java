package com.thirstwastaken2.data;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Per-player scratch state for the exhaustion path and the tick, added to every {@link Player} by
 * {@code PlayerMixin}. None of it is persisted or synced: it lives on the entity so that the exhaustion
 * hook neither writes the attachment nor recomputes the modifier every time vanilla charges exhaustion.
 *
 * <p>Server thread only. A respawn creates a new player and so a fresh tracker, which loses at most one
 * tick of pending exhaustion.
 */
public final class ExhaustionTracker {
    /** Raw vanilla exhaustion mirrored since the last tick, before the modifier is applied. */
    float pending;
    /**
     * Exhaustion already earned but not yet written to the attachment, because it has not crossed a sync
     * step since the last write. Lost when the player leaves, which is less than one step.
     */
    float unsynced;
    /** Ticks since the last heal quenched paid for, counted like vanilla's saturation heal timer. */
    int quenchedHealTimer;
    /** Ticks until Upset Stomach's next cramp, 0 while none is waiting; see {@code UpsetStomach.Step}. */
    int crampCountdown;
    float modifier;
    int modifierExpiresAt;
    int modifierGeneration;
    Level modifierLevel;

    /** Implemented by {@code PlayerMixin}; the member is prefixed because it lands on a vanilla class. */
    public interface Holder {
        ExhaustionTracker thirst$exhaustionTracker();
    }

    static ExhaustionTracker of(Player player) {
        return ((Holder) player).thirst$exhaustionTracker();
    }
}
