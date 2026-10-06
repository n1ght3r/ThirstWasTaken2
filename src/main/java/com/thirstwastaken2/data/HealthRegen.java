package com.thirstwastaken2.data;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Natural healing: when food may heal, through {@code FoodDataMixin}, and the heal quenched gives on its
 * own, {@link #healWithQuenched}.
 *
 * <p>Vanilla charges food for every point it heals. Blocking a heal therefore has to refund that cost,
 * or hunger drains for a heal the player never received.
 *
 * <p>Each bar needs the other: food does not heal with the thirst bar under
 * {@code foodHealMinThirstPercent}, and quenched does not heal with the food bar under
 * {@code quenchedHealMinFoodPercent}. Upset Stomach always stops both; no switch turns that off.
 * Saturation goes first: quenched only heals once the food's saturation is spent, so the two take
 * turns rather than adding up. Diverges
 * from the original mod, which let a nearly hydrated player heal slowly and had no illness.
 */
public final class HealthRegen {
    /** Ceiling on the refund, matching the exhaustion vanilla spends per heal. */
    public static final float MAX_REFUND = 6.0F;
    /** Vanilla's saturation heal comes every this many ticks; quenched's keeps the same beat. */
    private static final int QUENCHED_HEAL_INTERVAL = 10;
    /** The most one heal draws on, as vanilla caps the saturation one heal spends at 6. */
    private static final float QUENCHED_PER_HEAL = 6.0F;

    private HealthRegen() { }

    /**
     * Whether a natural heal from food has to be withheld from {@code player}: the thirst bar under
     * {@code foodHealMinThirstPercent} while {@code dehydrationHaltsHealthRegen} is on, or Upset Stomach,
     * which has no switch. A player thirst is switched off for keeps healing.
     */
    public static boolean blocksFoodHeal(Player player) {
        ThirstConfig config = ThirstConfig.get();
        ThirstData data = ThirstManager.get(player);
        if (config.dehydrationHaltsHealthRegen && data.enabled()
                && data.thirst() * 100 < ThirstData.MAX * config.foodHealMinThirstPercent) {
            return true;
        }
        return ill(player);
    }

    /**
     * Heals the player from quenched the way vanilla heals from saturation, scaled by
     * {@code quenchedHealthRegen}, and returns the thirst exhaustion that heal costs, zero on a tick that
     * does not heal. Called once per tick by {@code ThirstManager.tickPlayer}, which adds the cost to its
     * one write. It needs a full thirst bar, the food bar at {@code quenchedHealMinFoodPercent} or more,
     * no saturation left, since saturation heals first, and no Upset Stomach.
     */
    static float healWithQuenched(ServerPlayer player, ThirstData data, ExhaustionTracker tracker) {
        ThirstConfig config = ThirstConfig.get();
        double scale = config.quenchedHealthRegen;
        if (scale <= 0.0 || data.thirst() < ThirstData.MAX || data.quenched() <= 0 || !player.isHurt()
                || player.getFoodData().getSaturationLevel() > 0.0F
                || player.getFoodData().getFoodLevel() * 100 < ThirstData.MAX * config.quenchedHealMinFoodPercent
                || !Vanilla.naturalRegeneration(player) || ill(player)) {
            tracker.quenchedHealTimer = 0;
            return 0.0F;
        }
        if (++tracker.quenchedHealTimer < QUENCHED_HEAL_INTERVAL) return 0.0F;
        tracker.quenchedHealTimer = 0;
        // Vanilla heals f / 6 for f exhaustion; scaling both keeps quenched's rate of exchange with it.
        float spent = (float) scale * Math.min(data.quenched(), QUENCHED_PER_HEAL);
        player.heal(spent / QUENCHED_PER_HEAL);
        return spent;
    }

    private static boolean ill(Player player) {
        return Vanilla.hasEffect(player, ThirstEffects.UPSET_STOMACH);
    }
}
