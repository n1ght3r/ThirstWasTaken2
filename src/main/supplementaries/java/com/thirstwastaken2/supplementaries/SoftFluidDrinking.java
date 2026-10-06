package com.thirstwastaken2.supplementaries;

import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidTank;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

/**
 * Drinking a serving of water straight out of a Supplementaries jar or goblet.
 *
 * <p>Moonlight only lets a tank be drunk from when its fluid names a food item, and {@code
 * moonlight:water} names none, so before this a jar of water was something to look at. Water is the one
 * fluid a thirst mod has an answer for, so it answers here: a serving is drunk as the water bottle it
 * would have been poured into, through the same call a bottle finished by hand goes through, so the
 * grade decides what it restores and one roll drives the nausea and poison it may bring.
 */
public final class SoftFluidDrinking {
    /** A bottle's worth, the unit a soft fluid is counted in. */
    private static final int SERVING = 1;

    private SoftFluidDrinking() { }

    /**
     * @return whether a serving was drunk. False leaves Moonlight's own food path to answer, which for
     *     every fluid but water is still the right one.
     */
    public static boolean drink(SoftFluidTank tank, Player player, Level level) {
        SoftFluidStack fluid = tank.getFluid();
        if (!SoftFluidQuality.isWater(fluid)) return false;

        WaterQuality quality = SoftFluidQuality.quality(fluid);
        ItemStack serving = WaterPurity.setQuality(
                PotionContents.createItemStack(Items.POTION, Potions.WATER), quality);
        // Plain water follows the bottle's rule: a full bar refuses it, as it refuses the same bottle
        // drunk by hand, unless it is Clean or Pure and quenched has room. Sea water is a water bottle
        // too, so a full bar refuses that as well.
        if (WaterPurity.isPlainWaterDrink(serving) && !ThirstManager.canDrinkWater(player, serving)) return false;

        // The block both call sites sit on hands the client a success so the arm swings, and syncs its
        // tank back afterwards, so the client has nothing to do but agree.
        if (level.isClientSide()) return true;

        tank.removeFluid(SERVING, false);
        ThirstManager.drinkItem(player, serving);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        return true;
    }
}
