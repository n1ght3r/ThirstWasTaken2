package com.thirstwastaken2.purity;

import net.minecraft.util.Mth;

/**
 * What a container holds.
 *
 * <p>Sealed on purpose. Salt water is a different kind of water, not a low purity tier: cooking
 * cannot improve it, one salty serving spoils a whole batch, and it never hydrates. Making it its
 * own case means every consumer - tooltips, sickness, mixing, sprites - has to say what it does
 * with salt water instead of quietly treating it as a grade.
 */
public sealed interface WaterQuality {
    /** Sea water is stateless, so one instance serves every caller. */
    WaterQuality SALT = new Salt();

    /**
     * Fresh water of {@code purity}, clamped to the grades. One instance per grade, made once: a grade is
     * asked for on every tooltip frame and every drink, and the record holds nothing else.
     */
    static WaterQuality fresh(int purity) {
        return Fresh.GRADES[Mth.clamp(purity, WaterPurity.MIN, WaterPurity.MAX)];
    }

    /**
     * What a block holds after {@code right} is poured into {@code left}. Unlike the waterskin, a block
     * cannot average two grades, because its blockstate only has room for one, so it keeps the worse.
     * Any salt makes the whole batch salty.
     */
    static WaterQuality worse(WaterQuality left, WaterQuality right) {
        if (left instanceof Fresh held && right instanceof Fresh poured) {
            return fresh(Math.min(held.purity(), poured.purity()));
        }
        return SALT;
    }

    default boolean salty() {
        return this instanceof Salt;
    }

    /** Drinkable water, graded {@code 0..3}: dirty, murky, clean, pure. */
    record Fresh(int purity) implements WaterQuality {
        private static final Fresh[] GRADES = {new Fresh(0), new Fresh(1), new Fresh(2), new Fresh(3)};

        public Fresh {
            purity = Mth.clamp(purity, WaterPurity.MIN, WaterPurity.MAX);
        }
    }

    /** Sea water. It carries no grade, because no grade of it can be drunk. */
    record Salt() implements WaterQuality { }
}
