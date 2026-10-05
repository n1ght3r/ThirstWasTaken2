package com.thirstwastaken2.datagen;

import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.CoolingTubBlock;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.DistillerPartBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.item.ThirstItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * The blockstates of the copper distiller and of its three placeable parts, and the item models of the
 * distiller and those three parts. The fourth, the copper pipe, is a flat sprite in
 * {@code ThirstModelProvider}: as a 3D elbow it is a thin line in a slot.
 *
 * <p>The models themselves, one per half and variant, part and item, are written by
 * {@code tools/distiller/generate_distiller_model.py} and committed in {@code src/main/resources}, like
 * the hanging pot's Blockbench models. They face north; the blockstate turns them to the block's facing.
 * It is JSON handed over through {@link HangingPotModels#blockState}, for the same reason as the pot's.
 */
final class DistillerModels {
    private static final String NAME = "copper_distiller";

    private DistillerModels() { }

    /**
     * The distiller's blockstate and its three parts'. A piped boiler half is the machine's, its fire
     * burning or cold; an unpiped one is the boiler on its firebox before the pipe, always cold. A tub
     * half is drawn with its coolant or dry. A tub half that is not piped never exists, but every state
     * needs a model, so it is drawn as the lone tub. Only the boiler half reads {@code lit}, and only the
     * tub half {@code cooled}.
     */
    static void generate(BlockModelGenerators generators) {
        JsonObject variants = new JsonObject();
        for (DistillerBlock.Part part : DistillerBlock.Part.values()) {
            for (boolean piped : new boolean[] { true, false }) {
                for (boolean cooled : new boolean[] { true, false }) {
                    for (boolean lit : new boolean[] { true, false }) {
                        String model;
                        if (part == DistillerBlock.Part.BOILER) {
                            model = NAME + "_boiler" + (!piped ? "_unpiped" : lit ? "" : "_cold");
                        } else {
                            model = (piped ? NAME + "_tub" : "cooling_tub") + (cooled ? "" : "_empty");
                        }
                        String key = DistillerBlock.PART.getName() + "=" + part.getSerializedName() + ","
                                + DistillerBlock.PIPED.getName() + "=" + piped + ","
                                + DistillerBlock.COOLED.getName() + "=" + cooled + ","
                                + DistillerBlock.LIT.getName() + "=" + lit;
                        facings(variants, key, model);
                    }
                }
            }
        }
        accept(generators, ThirstBlocks.COPPER_DISTILLER, variants);

        for (DistillerPartBlock part : java.util.List.of(ThirstBlocks.BRICK_FIREBOX, ThirstBlocks.DISTILLER_BOILER)) {
            JsonObject partVariants = new JsonObject();
            facings(partVariants, "", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(part).getPath());
            accept(generators, part, partVariants);
        }
        JsonObject tub = new JsonObject();
        for (boolean cooled : new boolean[] { true, false }) {
            facings(tub, CoolingTubBlock.COOLED.getName() + "=" + cooled, "cooling_tub" + (cooled ? "" : "_empty"));
        }
        accept(generators, ThirstBlocks.COOLING_TUB, tub);
    }

    /** One variant per facing of the state {@code key} names, each turning {@code model} to face that way. */
    private static void facings(JsonObject variants, String key, String model) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            JsonObject variant = new JsonObject();
            variant.addProperty("model", model(model).toString());
            // toYRot is an entity's yaw, south 0; a blockstate turns from north, clockwise.
            int rotation = ((int) facing.toYRot() + 180) % 360;
            if (rotation != 0) variant.addProperty("y", rotation);
            variants.add(DistillerBlock.FACING.getName() + "=" + facing.getSerializedName()
                    + (key.isEmpty() ? "" : "," + key), variant);
        }
    }

    private static void accept(BlockModelGenerators generators, net.minecraft.world.level.block.Block block,
                               JsonObject variants) {
        JsonObject blockState = new JsonObject();
        blockState.add("variants", variants);
        generators.blockStateOutput.accept(HangingPotModels.blockState(block, blockState));
    }

    /**
     * Each item shows in 3D, the way a block's item does: the distiller whole, and each part as the piece
     * of the machine it is, built from the same elements and vanilla textures. Their models are committed
     * as {@code models/item/<name>_3d}, a name datagen never writes. Before 1.21.4 the item's own model
     * is what the game reads, so it is a child of that one; from 1.21.4 a definition points at it.
     */
    static void item(net.minecraft.client.data.models.ItemModelGenerators generators) {
        for (net.minecraft.world.item.Item item : java.util.List.of(ThirstItems.COPPER_DISTILLER,
                ThirstItems.DISTILLER_BOILER, ThirstItems.COOLING_TUB, ThirstItems.BRICK_FIREBOX)) {
            String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
            Identifier model = ThirstWasTaken2.id("item/" + name + "_3d");
            //? if >=1.21.4 {
            generators.itemModelOutput.accept(item,
                    net.minecraft.client.data.models.model.ItemModelUtils.plainModel(model));
            //?} else {
            /*JsonObject json = new JsonObject();
            json.addProperty("parent", model.toString());
            generators.output.accept(ThirstWasTaken2.id("item/" + name), () -> json);
            *///?}
        }
    }

    private static Identifier model(String name) {
        return ThirstWasTaken2.id("block/" + name);
    }
}
