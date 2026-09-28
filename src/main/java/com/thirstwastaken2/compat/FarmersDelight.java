package com.thirstwastaken2.compat;

import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Farmer's Delight, reached by registry id only, so none of its classes is ever loaded.
 *
 * <p>Its drinks and meals are ordinary entries in {@code ThirstConfig}, and the Cooking Pot recipes
 * are data files with a load condition. What is left here is the Nourishment effect, which keeps a
 * player fed and, as in the original mod, keeps them hydrated too.
 */
public final class FarmersDelight {
    public static final String MOD_ID = "farmersdelight";
    private static final boolean LOADED = Loader.isModLoaded(MOD_ID);
    private static final Identifier NOURISHMENT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "nourishment");
    /** Looked up on first use: registries are frozen before any player ticks, but not at class load. */
    private static volatile Holder<MobEffect> nourishment;

    private FarmersDelight() { }

    public static boolean isLoaded() {
        return LOADED;
    }

    /**
     * Whether the player has Nourishment. Farmer's Delight cancels food exhaustion under it in a way
     * that changed between its versions, so thirst cannot rely on that and asks for the effect itself.
     */
    public static boolean isNourished(Player player) {
        if (!LOADED) return false;
        Holder<MobEffect> effect = nourishment;
        if (effect == null) {
            effect = Vanilla.mobEffect(NOURISHMENT_ID);
            if (effect == null) return false;
            nourishment = effect;
        }
        return Vanilla.hasEffect(player, effect);
    }
}
