package com.thirstwastaken2.meadow;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.ItemStack;
import net.satisfy.meadow.core.util.WoodenCauldronBehavior;

/**
 * Meadow's wooden cauldron, which core's cauldron code passes by: the wooden water cauldron is a
 * {@code LayeredCauldronBlock} Meadow builds itself, without the {@code purity} property, and its
 * interactions are Meadow's own maps. So it holds standing water, as Farm & Charm's Water Trough does:
 * what is drawn from it is {@link #STANDING} whatever went in, and sea water is refused on the way in,
 * since it would come out fresh.
 *
 * <p>Loaded only from the mixins, which only apply with Meadow installed.
 */
public final class WoodenCauldron {
    /**
     * What the wooden water cauldron gives: Murky, the grade of Farm & Charm's standing water
     * ({@code StandingWater.STANDING}), so the Let's Do mods agree. An open wooden tub, filled by rain or
     * by hand, is not clean water, and nothing gets better by going through it.
     */
    public static final WaterQuality STANDING = WaterQuality.fresh(1);

    /** Set on the server while a wooden water cauldron is used, so the container drawn is stamped. */
    private static final ThreadLocal<Boolean> DRAWING = new ThreadLocal<>();

    private WoodenCauldron() { }

    /**
     * Whether a cauldron with these interactions is one of Meadow's three: empty, water, powder snow. The
     * maps are what tells them apart, since the water one is a plain {@code LayeredCauldronBlock}.
     */
    public static boolean isWooden(CauldronInteraction.InteractionMap interactions) {
        return interactions == WoodenCauldronBehavior.EMPTY || interactions == WoodenCauldronBehavior.WATER
                || interactions == WoodenCauldronBehavior.POWDER_SNOW;
    }

    /** Whether {@code interactions} are the wooden water cauldron's, the one water is drawn from. */
    public static boolean holdsWater(CauldronInteraction.InteractionMap interactions) {
        return interactions == WoodenCauldronBehavior.WATER;
    }

    public static void startDrawing() {
        DRAWING.set(Boolean.TRUE);
    }

    public static void stopDrawing() {
        DRAWING.remove();
    }

    /** Stamps {@code stack} with {@link #STANDING} while a wooden water cauldron is used, if it is a water container. */
    public static ItemStack stampDrawn(ItemStack stack) {
        if (DRAWING.get() != null && WaterPurity.isWaterContainer(stack)) WaterPurity.setQuality(stack, STANDING);
        return stack;
    }
}
