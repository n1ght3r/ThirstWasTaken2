package com.thirstwastaken2.datagen;

import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.item.ThirstItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * The copper distiller's blockstate, and the item models of the distiller and three of its parts. The
 * fourth, the copper pipe, is a flat sprite in {@code ThirstModelProvider}: as a 3D elbow it is a thin
 * line in a slot.
 *
 * <p>The models themselves, one per half and one per item, are written by
 * {@code tools/distiller/generate_distiller_model.py} and committed in {@code src/main/resources}, like
 * the hanging pot's Blockbench models. They face north; the blockstate turns them to the block's facing.
 * It is JSON handed over through {@link HangingPotModels#blockState}, for the same reason as the pot's.
 */
final class DistillerModels {
    private static final String NAME = "copper_distiller";

    private DistillerModels() { }

    static void generate(BlockModelGenerators generators) {
        JsonObject variants = new JsonObject();
        for (DistillerBlock.Part part : DistillerBlock.Part.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                JsonObject model = new JsonObject();
                model.addProperty("model", model(NAME + "_" + part.getSerializedName()).toString());
                // toYRot is an entity's yaw, south 0; a blockstate turns from north, clockwise.
                int rotation = ((int) facing.toYRot() + 180) % 360;
                if (rotation != 0) model.addProperty("y", rotation);
                variants.add(DistillerBlock.FACING.getName() + "=" + facing.getSerializedName() + ","
                        + DistillerBlock.PART.getName() + "=" + part.getSerializedName(), model);
            }
        }
        JsonObject blockState = new JsonObject();
        blockState.add("variants", variants);
        generators.blockStateOutput.accept(HangingPotModels.blockState(ThirstBlocks.COPPER_DISTILLER, blockState));
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
