package com.thirstwastaken2.compat;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;
import java.util.function.Supplier;

/**
 * Villager trades that give a village a part in finding water: leatherworkers sell waterskins and
 * clerics sell clean water, so emeralds buy drink for a player who cannot boil yet. Not in upstream.
 *
 * <p>Each trade joins its profession's pool at its level, where vanilla draws two of the level's
 * trades for each villager, so not every leatherworker or cleric offers it.
 *
 * <p>Up to 1.21.11 trades are code, added through {@link Loader#addVillagerTrade}. From 26.1 they are
 * data, and datagen writes {@link #TRADES} as {@code villager_trade} files and entries in vanilla's
 * per-level tags, so on those versions {@link #register} adds nothing. One table serves both.
 */
public final class TradeIntegration {
    /** How far reputation and demand move a price, vanilla's own for nearly every trade. */
    public static final float PRICE_MULTIPLIER = 0.05F;

    /** {@link Trade#grade} for a trade whose item holds no water. */
    public static final int NO_WATER = -1;

    /**
     * One trade, {@code emeralds} for one {@code item}, from {@code level} (1, novice, to 5, master) of
     * {@code profession}. A water bottle holds plain water stamped {@code grade}, fresh. {@code name} is
     * its file name from 26.1.
     *
     * <p>What it gives is described rather than held as a stack, because datagen, which writes it from
     * 26.1, runs before items can make one.
     */
    public record Trade(String name, Identifier profession, int level, int emeralds, Supplier<Item> item, int grade,
                        int maxUses, int xp) {
        /** Its id from 26.1, beside vanilla's own: {@code thirstwastaken2:<profession>/<level>/<name>}. */
        public Identifier id() {
            return ThirstWasTaken2.id(profession.getPath() + "/" + level + "/" + name);
        }

        /** The stack it gives. */
        public ItemStack gives() {
            Item given = item.get();
            ItemStack stack = given == Items.POTION ? Vanilla.waterBottle() : new ItemStack(given);
            return grade == NO_WATER ? stack : WaterPurity.set(stack, grade);
        }

        /** The offer, or {@code null}, which offers nothing, while the config switches its item off. */
        public MerchantOffer offer() {
            if (!ThirstConfig.get().isItemEnabled(Vanilla.itemId(item.get()).toString())) return null;
            return Vanilla.emeraldOffer(emeralds, gives(), maxUses, xp, PRICE_MULTIPLIER);
        }
    }

    /** Novice trades, so a village helps from the first visit. Prices sit beside vanilla's novice ones. */
    public static final List<Trade> TRADES = List.of(
            new Trade("emerald_waterskin", Identifier.withDefaultNamespace("leatherworker"), 1,
                    3, () -> ThirstItems.WATERSKIN, NO_WATER, 12, 1),
            new Trade("emerald_clean_water", Identifier.withDefaultNamespace("cleric"), 1,
                    1, () -> Items.POTION, 2, 16, 1));

    private TradeIntegration() { }

    public static void register() {
        for (Trade trade : TRADES) {
            Loader.addVillagerTrade(trade.profession(), trade.level(), trade::offer);
        }
    }
}
