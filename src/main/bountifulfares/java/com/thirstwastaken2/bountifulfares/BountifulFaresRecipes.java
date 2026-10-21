package com.thirstwastaken2.bountifulfares;

import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/** What the two recipe mixins share: which recipes are the mod's, and whether a grid holds sea water. */
public final class BountifulFaresRecipes {
    private BountifulFaresRecipes() { }

    /** Whether a recipe making {@code result} makes an item of Bountiful Fares'. */
    public static boolean makesBountifulFares(ItemStack result) {
        return Vanilla.itemId(result.getItem()).getNamespace().equals("bountifulfares");
    }

    public static boolean holdsSeaWater(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            if (WaterPurity.isSalty(input.getItem(i))) return true;
        }
        return false;
    }
}
