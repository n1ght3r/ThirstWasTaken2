package com.thirstwastaken2.croptopia.platform;

import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.item.Item;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?} else {
/*import net.minecraft.world.item.ItemStack;
*///?}
//? if >=1.21 {
import net.minecraft.world.item.crafting.CraftingInput;
//?} else {
/*import net.minecraft.world.inventory.CraftingContainer;
*///?}

/** The crafting recipe types whose shape differs between the Minecraft versions Croptopia builds for. */
public final class CroptopiaVanilla {
    private CroptopiaVanilla() { }

    /** The item a shaped or shapeless recipe makes. 26.1 holds the result as a template, earlier as a stack. */
    //? if >=26.1 {
    public static Item resultItem(ItemStackTemplate result) {
        return result.item().value();
    }
    //?} else {
    /*public static Item resultItem(ItemStack result) {
        return result.getItem();
    }
    *///?}

    /**
     * Whether a crafting grid holds sea water anywhere. 1.21 hands a recipe a {@code CraftingInput}, 1.20.1
     * the grid's container itself.
     */
    //? if >=1.21 {
    public static boolean holdsSeaWater(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            if (WaterPurity.isSalty(input.getItem(i))) return true;
        }
        return false;
    }
    //?} else {
    /*public static boolean holdsSeaWater(CraftingContainer input) {
        for (int i = 0; i < input.getContainerSize(); i++) {
            if (WaterPurity.isSalty(input.getItem(i))) return true;
        }
        return false;
    }
    *///?}
}
