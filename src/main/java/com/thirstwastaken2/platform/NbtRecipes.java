package com.thirstwastaken2.platform;

/**
 * Recipe types for Minecraft 1.20.1 whose result can carry a tag: {@code thirstwastaken2:smelting},
 * {@code smoking}, {@code campfire_cooking} and {@code crafting_shapeless}. From 1.20.5 a vanilla result
 * carries components, which is how every purification recipe hands out water of its new grade; 1.20.1
 * reads a cooking result as a bare item id and refuses a tag on a crafting one. These read vanilla's JSON
 * with a {@code result} of {@code item}, {@code count} and {@code nbt}, the tag as SNBT, and build
 * vanilla's own recipe from it, so a furnace or crafting table uses it like any other and a client is sent
 * it through vanilla's serializer. On every later version this registers nothing.
 */
public final class NbtRecipes {
    private NbtRecipes() { }

    /** Registers the serializers. Called by {@link ItemWaterData#register} once the registry takes them. */
    static void register() {
        //? if <1.20.5 {
        /*cooking("smelting", net.minecraft.world.item.crafting.RecipeSerializer.SMELTING_RECIPE,
                net.minecraft.world.item.crafting.SmeltingRecipe::new, 200);
        cooking("smoking", net.minecraft.world.item.crafting.RecipeSerializer.SMOKING_RECIPE,
                net.minecraft.world.item.crafting.SmokingRecipe::new, 100);
        cooking("campfire_cooking", net.minecraft.world.item.crafting.RecipeSerializer.CAMPFIRE_COOKING_RECIPE,
                net.minecraft.world.item.crafting.CampfireCookingRecipe::new, 100);
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER,
                com.thirstwastaken2.ThirstWasTaken2.id("crafting_shapeless"), new Shapeless());
        *///?}
    }

    //? if <1.20.5 {
    /*private static <T extends net.minecraft.world.item.crafting.AbstractCookingRecipe> void cooking(
            String name, net.minecraft.world.item.crafting.RecipeSerializer<T> vanilla, Cooking.Factory<T> factory,
            int defaultTime) {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER,
                com.thirstwastaken2.ThirstWasTaken2.id(name), new Cooking<>(vanilla, factory, defaultTime));
    }

    // A result: an item, a count and a tag, the tag as SNBT or as a JSON object.
    private static net.minecraft.world.item.ItemStack result(com.google.gson.JsonObject json) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.crafting.ShapedRecipe.itemFromJson(json),
                net.minecraft.util.GsonHelper.getAsInt(json, "count", 1));
        com.google.gson.JsonElement nbt = json.get("nbt");
        if (nbt != null) {
            try {
                stack.setTag(net.minecraft.nbt.TagParser.parseTag(
                        nbt.isJsonObject() ? nbt.toString() : net.minecraft.util.GsonHelper.convertToString(nbt, "nbt")));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                throw new com.google.gson.JsonSyntaxException("Invalid result nbt: " + exception.getMessage());
            }
        }
        return stack;
    }

    // Reads a cooking recipe the way vanilla's serializer does, but for the result; the network form is
    // vanilla's, which is also what the recipe itself names.
    private record Cooking<T extends net.minecraft.world.item.crafting.AbstractCookingRecipe>(
            net.minecraft.world.item.crafting.RecipeSerializer<T> vanilla, Factory<T> factory, int defaultTime)
            implements net.minecraft.world.item.crafting.RecipeSerializer<T> {
        interface Factory<T> {
            T create(net.minecraft.resources.Identifier id, String group,
                     net.minecraft.world.item.crafting.CookingBookCategory category,
                     net.minecraft.world.item.crafting.Ingredient ingredient, net.minecraft.world.item.ItemStack result,
                     float experience, int time);
        }

        @Override
        public T fromJson(net.minecraft.resources.Identifier id, com.google.gson.JsonObject json) {
            com.google.gson.JsonElement ingredient = net.minecraft.util.GsonHelper.isArrayNode(json, "ingredient")
                    ? net.minecraft.util.GsonHelper.getAsJsonArray(json, "ingredient")
                    : net.minecraft.util.GsonHelper.getAsJsonObject(json, "ingredient");
            return factory.create(id, net.minecraft.util.GsonHelper.getAsString(json, "group", ""),
                    net.minecraft.world.item.crafting.CookingBookCategory.CODEC.byName(
                            net.minecraft.util.GsonHelper.getAsString(json, "category", null),
                            net.minecraft.world.item.crafting.CookingBookCategory.MISC),
                    net.minecraft.world.item.crafting.Ingredient.fromJson(ingredient, false),
                    result(net.minecraft.util.GsonHelper.getAsJsonObject(json, "result")),
                    net.minecraft.util.GsonHelper.getAsFloat(json, "experience", 0.0F),
                    net.minecraft.util.GsonHelper.getAsInt(json, "cookingtime", defaultTime));
        }

        @Override
        public T fromNetwork(net.minecraft.resources.Identifier id, net.minecraft.network.FriendlyByteBuf buffer) {
            return vanilla.fromNetwork(id, buffer);
        }

        @Override
        public void toNetwork(net.minecraft.network.FriendlyByteBuf buffer, T recipe) {
            vanilla.toNetwork(buffer, recipe);
        }
    }

    // A shapeless crafting recipe, read the way vanilla's serializer does but for the result.
    private static final class Shapeless
            implements net.minecraft.world.item.crafting.RecipeSerializer<net.minecraft.world.item.crafting.ShapelessRecipe> {
        @Override
        public net.minecraft.world.item.crafting.ShapelessRecipe fromJson(net.minecraft.resources.Identifier id,
                                                                          com.google.gson.JsonObject json) {
            net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> ingredients =
                    net.minecraft.core.NonNullList.create();
            for (com.google.gson.JsonElement element : net.minecraft.util.GsonHelper.getAsJsonArray(json, "ingredients")) {
                net.minecraft.world.item.crafting.Ingredient ingredient =
                        net.minecraft.world.item.crafting.Ingredient.fromJson(element, false);
                if (!ingredient.isEmpty()) ingredients.add(ingredient);
            }
            if (ingredients.isEmpty() || ingredients.size() > 9) {
                throw new com.google.gson.JsonParseException("A shapeless recipe takes one to nine ingredients");
            }
            return new net.minecraft.world.item.crafting.ShapelessRecipe(id,
                    net.minecraft.util.GsonHelper.getAsString(json, "group", ""),
                    net.minecraft.world.item.crafting.CraftingBookCategory.CODEC.byName(
                            net.minecraft.util.GsonHelper.getAsString(json, "category", null),
                            net.minecraft.world.item.crafting.CraftingBookCategory.MISC),
                    result(net.minecraft.util.GsonHelper.getAsJsonObject(json, "result")), ingredients);
        }

        @Override
        public net.minecraft.world.item.crafting.ShapelessRecipe fromNetwork(net.minecraft.resources.Identifier id,
                                                                             net.minecraft.network.FriendlyByteBuf buffer) {
            return net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buffer);
        }

        @Override
        public void toNetwork(net.minecraft.network.FriendlyByteBuf buffer,
                              net.minecraft.world.item.crafting.ShapelessRecipe recipe) {
            net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer, recipe);
        }
    }
    *///?}
}
