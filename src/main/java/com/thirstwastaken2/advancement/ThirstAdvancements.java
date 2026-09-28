package com.thirstwastaken2.advancement;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The advancements the mod awards itself.
 *
 * <p>Every one of them is a JSON advancement whose only criterion is {@code minecraft:impossible},
 * granted from here by name. A custom {@code CriterionTrigger} would be the idiomatic answer, but
 * the trigger classes have moved package twice across the three supported Minecraft versions and
 * their codecs differ with them, which would spread version branches through a package that has
 * none. Awarding by id uses one API that is identical on every version, at the cost of datapacks
 * not being able to write their own conditions against these events.
 *
 * <p>{@code boil_water} is the exception: a furnace credits the player who takes the result, so
 * vanilla's own {@code recipe_crafted} trigger can see it and that advancement needs nothing here.
 */
public final class ThirstAdvancements {
    /** The criterion name every awarded advancement in this mod uses. */
    private static final String CRITERION = "thirst";

    private static final Identifier FIRST_DRINK = ThirstWasTaken2.id("first_drink");
    private static final Identifier DIRTY_WATER = ThirstWasTaken2.id("dirty_water");
    private static final Identifier PURIFIED_WATER = ThirstWasTaken2.id("purified_water");
    private static final Identifier SEA_WATER = ThirstWasTaken2.id("sea_water");
    private static final Identifier NETHER_DRINK = ThirstWasTaken2.id("nether_drink");

    private ThirstAdvancements() { }

    /**
     * One drink of water of a known quality, however it was drunk: from a container, or by hand
     * straight from the world. Drinks that are not water never reach this.
     */
    public static void drank(Player player, WaterQuality quality) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        award(serverPlayer, FIRST_DRINK);
        if (quality instanceof WaterQuality.Fresh fresh) {
            if (fresh.purity() == WaterPurity.MIN) award(serverPlayer, DIRTY_WATER);
            if (fresh.purity() == WaterPurity.MAX) award(serverPlayer, PURIFIED_WATER);
        } else {
            award(serverPlayer, SEA_WATER);
        }
        // Where water evaporates, i.e. the Nether and any dimension like it: the same test the
        // exhaustion modifier uses, so the advancement tracks the hardship it is named after.
        if (Vanilla.waterEvaporates(serverPlayer.level(), serverPlayer.blockPosition())) {
            award(serverPlayer, NETHER_DRINK);
        }
    }

    /** Awards one advancement, or does nothing if a datapack has removed it. */
    private static void award(ServerPlayer player, Identifier id) {
        Vanilla.awardAdvancement(player, id, CRITERION);
    }
}
