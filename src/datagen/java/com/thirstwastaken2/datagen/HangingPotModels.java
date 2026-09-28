package com.thirstwastaken2.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.HangingPotBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The hanging pots' blockstates and the models they assemble.
 *
 * <p>A pot is a multipart of three pieces. The copper pot itself, {@code models/block/copper_hanging_pot},
 * and the frame template it hangs from are Blockbench models and stay hand-written in
 * {@code src/main/resources}, as does the item's template, which is both of them in one model with its
 * own display transforms. What is written here is what differs by version or is repetitive: the frame
 * and the item, which name vanilla's chain texture, renamed in 1.21.9, and one flat water surface per
 * fill level and water quality, each with its own texture from {@code tools/generate_pot_water.py}.
 *
 * <p>The iron pot is the copper one retextured: its texture keeps the copper one's layout, so each of
 * its models is a child of the copper model that only swaps the texture. Both pots share the water
 * surfaces, which do not depend on the pot around them.
 *
 * <p>Everything is assembled as JSON, the one shape that has not changed. The blockstate's builder
 * classes changed twice across the supported versions, so it is parsed into whatever each version's
 * model output takes instead.
 */
final class HangingPotModels {
    private static final String NAME = "copper_hanging_pot";
    private static final String IRON = "iron_hanging_pot";
    /**
     * What breaking the iron pot scatters. The pot's own texture is a sheet with wood, empty space and
     * the texture artist's mark on it, and a particle is a random piece of it; vanilla's cauldron is the
     * same cast iron in one clean tile. The copper models name vanilla's copper block the same way.
     */
    private static final String IRON_PARTICLE = "minecraft:block/cauldron_side";
    private static final Identifier POT = model(NAME);
    private static final Identifier FRAME_TEMPLATE = model("template_" + NAME + "_frame");
    private static final Identifier ITEM_TEMPLATE = model("template_" + NAME + "_item");
    //? if >=1.21.9 {
    private static final String CHAIN_TEXTURE = "minecraft:block/iron_chain";
    //?} else
    //private static final String CHAIN_TEXTURE = "minecraft:block/chain";

    private HangingPotModels() { }

    static void generate(BlockModelGenerators generators) {
        for (int level = 1; level <= HangingPotBlock.CAPACITY; level++) {
            for (String water : waters().values()) {
                JsonElement json = surfaceJson(level, water);
                generators.modelOutput.accept(surface(level, water), () -> json);
            }
        }

        pot(generators, ThirstBlocks.COPPER_HANGING_POT, NAME, POT);

        JsonObject ironPot = retextured(POT, IRON, "pot");
        generators.modelOutput.accept(model(IRON), () -> ironPot);
        pot(generators, ThirstBlocks.IRON_HANGING_POT, IRON, model(IRON));
    }

    /** One pot's frame and blockstate, hanging {@code pot} from the frame and filling it with water. */
    private static void pot(BlockModelGenerators generators, Block block, String name, Identifier pot) {
        Identifier frame = model(name + "_frame");
        JsonObject frameJson = withChain(FRAME_TEMPLATE, name);
        generators.modelOutput.accept(frame, () -> frameJson);

        JsonArray parts = new JsonArray();
        for (Direction.Axis axis : new Direction.Axis[] { Direction.Axis.Z, Direction.Axis.X }) {
            // The models are drawn with the crossbar along Z.
            int rotation = axis == Direction.Axis.X ? 90 : 0;
            JsonObject hanging = when(HangingPotBlock.AXIS.getName(), axis.getSerializedName());
            hanging.addProperty(HangingPotBlock.HANGING.getName(), "true");
            parts.add(part(hanging, frame, rotation));
            parts.add(part(when(HangingPotBlock.AXIS.getName(), axis.getSerializedName()), pot, rotation));
        }

        for (int level = 1; level <= HangingPotBlock.CAPACITY; level++) {
            for (Map.Entry<String, String> water : waters().entrySet()) {
                JsonObject condition = when(HangingPotBlock.LEVEL.getName(), Integer.toString(level));
                condition.addProperty(WaterPurity.BLOCK_PURITY.getName(), water.getKey());
                parts.add(part(condition, surface(level, water.getValue()), 0));
            }
        }

        JsonObject blockState = new JsonObject();
        blockState.add("multipart", parts);
        generators.blockStateOutput.accept(blockState(block, blockState));
    }

    /**
     * The item shows the pot on its frame in 3D, the way a chest or a crafting table does, rather than a
     * flat sprite. Before 1.21.4 the item's own model is what the game reads; from 1.21.4 a definition
     * points at it.
     */
    static void item(net.minecraft.client.data.models.ItemModelGenerators generators) {
        item(generators, com.thirstwastaken2.item.ThirstItems.COPPER_HANGING_POT, NAME);
        item(generators, com.thirstwastaken2.item.ThirstItems.IRON_HANGING_POT, IRON);
    }

    private static void item(net.minecraft.client.data.models.ItemModelGenerators generators,
                             net.minecraft.world.item.Item pot, String name) {
        Identifier id = ThirstWasTaken2.id("item/" + name);
        JsonObject item = withChain(ITEM_TEMPLATE, name);
        //? if >=1.21.4 {
        generators.modelOutput.accept(id, () -> item);
        generators.itemModelOutput.accept(pot, net.minecraft.client.data.models.model.ItemModelUtils.plainModel(id));
        //?} else
        //generators.output.accept(id, () -> item);
    }

    /**
     * A model that fills {@code template}'s chain slot with this version's chain texture, and, for any
     * pot but the copper one the templates are drawn with, its frame slot with that pot's texture.
     */
    private static JsonObject withChain(Identifier template, String name) {
        JsonObject json = name.equals(NAME) ? new JsonObject() : retextured(template, name, "frame");
        json.addProperty("parent", template.toString());
        JsonObject textures = json.has("textures") ? json.getAsJsonObject("textures") : new JsonObject();
        textures.addProperty("chain", CHAIN_TEXTURE);
        json.add("textures", textures);
        // NeoForge before 26.1 reads the render type off the model; everything else ignores the key.
        json.addProperty("render_type", "minecraft:cutout");
        return json;
    }

    /** A child of {@code parent} that draws its {@code slot} with {@code name}'s texture, and iron particles. */
    private static JsonObject retextured(Identifier parent, String name, String slot) {
        String texture = model(name).toString();
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent.toString());
        JsonObject textures = new JsonObject();
        textures.addProperty(slot, texture);
        textures.addProperty("particle", IRON_PARTICLE);
        json.add("textures", textures);
        return json;
    }

    // The blockstate generator became a pair of block and parsed definition in 1.21.5, and the
    // definition class was replaced in 26.1. Before 1.21.5 it hands over the JSON itself.
    //? if >=26.1 {
    private static net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator blockState(Block block, JsonObject json) {
        var definition = net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        return new net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator() {
            @Override
            public Block block() {
                return block;
            }

            @Override
            public net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher create() {
                return definition;
            }
        };
    }
    //?}
    //? if >=1.21.5 <26.1 {
    /*private static net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator blockState(Block block, JsonObject json) {
        var definition = net.minecraft.client.renderer.block.model.BlockModelDefinition.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        return new net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator() {
            @Override
            public Block block() {
                return block;
            }

            @Override
            public net.minecraft.client.renderer.block.model.BlockModelDefinition create() {
                return definition;
            }
        };
    }
    *///?}
    //? if <1.21.5 {
    /*private static net.minecraft.client.data.models.blockstates.BlockStateGenerator blockState(Block block, JsonObject json) {
        return new net.minecraft.client.data.models.blockstates.BlockStateGenerator() {
            @Override
            public Block getBlock() {
                return block;
            }

            @Override
            public JsonElement get() {
                return json;
            }
        };
    }
    *///?}

    /**
     * The stored-quality values each water texture is drawn for, and the texture's suffix, the same
     * suffixes the filled bowl uses. A pot whose quality was never set holds the default grade, which
     * the client cannot read from the server's config, so it shows clean water, the default default.
     */
    private static Map<String, String> waters() {
        Map<String, String> waters = new LinkedHashMap<>();
        for (int purity = WaterPurity.MIN; purity <= WaterPurity.MAX; purity++) {
            int stored = purity + 1;
            String values = purity == 2 ? WaterPurity.BLOCK_UNSET + "|" + stored : Integer.toString(stored);
            waters.put(values, "purity_" + purity);
        }
        waters.put(Integer.toString(WaterPurity.BLOCK_SALT), "salty");
        return waters;
    }

    /** The water surface model for {@code level} servings of {@code water}, shared by every pot. */
    private static Identifier surface(int level, String water) {
        return model(NAME + "_water_" + level + "_" + water);
    }

    /** One water surface filling the inside of the pot, at the height the block gives its servings. */
    private static JsonElement surfaceJson(int level, String water) {
        double height = HangingPotBlock.surfaceHeight(level);
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("water", ThirstWasTaken2.id("block/" + NAME + "_water_" + water).toString());
        textures.addProperty("particle", "#water");
        json.add("textures", textures);

        JsonObject face = new JsonObject();
        face.add("uv", numbers(4, 4, 12, 12));
        face.addProperty("texture", "#water");
        JsonObject faces = new JsonObject();
        faces.add("up", face);
        JsonObject element = new JsonObject();
        element.add("from", numbers(4, height, 4));
        element.add("to", numbers(12, height, 12));
        element.add("faces", faces);
        JsonArray elements = new JsonArray();
        elements.add(element);
        json.add("elements", elements);
        return json;
    }

    private static JsonObject part(JsonObject when, Identifier model, int rotation) {
        JsonObject apply = new JsonObject();
        apply.addProperty("model", model.toString());
        if (rotation != 0) apply.addProperty("y", rotation);
        JsonObject part = new JsonObject();
        part.add("when", when);
        part.add("apply", apply);
        return part;
    }

    private static JsonObject when(String property, String value) {
        JsonObject when = new JsonObject();
        when.addProperty(property, value);
        return when;
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            if (value == Math.rint(value)) array.add((int) value);
            else array.add(value);
        }
        return array;
    }

    private static Identifier model(String name) {
        return ThirstWasTaken2.id("block/" + name);
    }
}
