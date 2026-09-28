package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.gametest.platform.MockPlayers;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
//? if >=1.21 {
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
//?}
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Scaffolding shared by the gametests: readable assertions, a water source, and a player aimed at it. */
final class TestFixtures {
    /** Relative position of the water source every water test uses. */
    static final BlockPos WATER = new BlockPos(2, 2, 2);

    private TestFixtures() { }

    /**
     * Asserts with a plain message. {@link GameTestHelper#assertTrue} takes a {@link Component} from
     * 1.21.5 and a string before it, so this keeps the call sites the same on every version.
     */
    static void check(GameTestHelper helper, boolean condition, String message) {
        //? if >=1.21.5 {
        helper.assertTrue(condition, Component.literal(message));
        //?} else
        //helper.assertTrue(condition, message);
    }

    /**
     * Places a stone floor with one water source on top of it and returns the source's absolute
     * position.
     *
     * <p>Fluid spreading is scheduled rather than immediate, and these tests run inside a single
     * tick, so the source cannot flow away before it is sampled.
     */
    static BlockPos water(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, WATER.getY() - 1, z), Blocks.STONE);
            }
        }
        helper.setBlock(WATER, Blocks.WATER);
        return helper.absolutePos(WATER);
    }

    /**
     * A mock server player that has joined the test level.
     *
     * <p>{@code makeMockServerPlayerInLevel} is deprecated for removal, but it is the only call
     * that exists on every supported version and the only one that puts the player through the
     * player list, which the tests rely on. 26.2 added {@code makeMockServerPlayer(GameType)},
     * which builds a player without joining it to the level, so it is not a drop-in replacement,
     * and 1.21.11 and 26.1 do not have it at all. Funnelling every test through here suppresses
     * the warning once and makes the eventual migration a single edit. Each loader's
     * {@code MockPlayers} makes the call, since Forge 47 cannot take vanilla's player as it is.
     */
    static ServerPlayer mockPlayer(GameTestHelper helper) {
        return MockPlayers.create(helper);
    }

    /** A survival player with a full hunger bar, so vanilla allows sprinting and charges exhaustion. */
    static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(20);
        return player;
    }

    /**
     * Whether {@code player} passes the food check vanilla's client asks before sprinting, with the
     * mod's thirst gate applied. That check is protected, so it is called reflectively; dev runs use
     * Mojang's names on every version. On 1.21.1 the check is private to the client's LocalPlayer, which
     * a server test cannot reach, so there it answers from vanilla's rule and the mod's gate directly.
     */
    static boolean canSprint(ServerPlayer player) {
        //? if >1.21.1 {
        try {
            java.lang.reflect.Method check = net.minecraft.world.entity.player.Player.class
                    .getDeclaredMethod("hasEnoughFoodToDoExhaustiveManoeuvres");
            check.setAccessible(true);
            return (boolean) check.invoke(player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Player#hasEnoughFoodToDoExhaustiveManoeuvres is missing", exception);
        }
        //?} else {
        /*return player.getFoodData().getFoodLevel() > 6
                && com.thirstwastaken2.data.ThirstManager.allowsSprinting(player);
        *///?}
    }

    /** A vanilla water bottle: a potion whose contents are plain water. */
    static ItemStack waterBottle() {
        return Vanilla.waterBottle();
    }

    /**
     * Changes the live config, commits it the way the config screen does, runs {@code checks}, and
     * puts the config back however the checks ended.
     *
     * <p>Committing re-sanitises, bumps the generation that per-item caches watch, and saves the file,
     * so the original is restored by writing the file back and loading it again.
     */
    static void withConfig(java.util.function.Consumer<ThirstConfig> change, Runnable checks) {
        Path path = ThirstConfig.path();
        byte[] original;
        try {
            original = Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        try {
            change.accept(ThirstConfig.get());
            ThirstConfig.commit();
            checks.run();
        } finally {
            try {
                Files.write(path, original);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            ThirstConfig.load();
        }
    }

    // What a furnace or campfire makes of a stack, and what a crafting grid of the given size makes of
    // its items in order, or an empty stack when no recipe takes them. 1.21 gave recipes inputs of their
    // own in place of containers, and 26.1 dropped the registry lookup assembling a result took.
    //? if >=26.1 {
    static <T extends Recipe<SingleRecipeInput>> ItemStack cook(GameTestHelper helper, RecipeType<T> type, ItemStack stack) {
        return craft(helper, type, new SingleRecipeInput(stack));
    }

    static ItemStack craftGrid(GameTestHelper helper, int width, int height, java.util.List<ItemStack> items) {
        return craft(helper, RecipeType.CRAFTING, net.minecraft.world.item.crafting.CraftingInput.of(width, height, items));
    }

    private static <I extends RecipeInput, T extends Recipe<I>> ItemStack craft(GameTestHelper helper, RecipeType<T> type, I input) {
        return helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(type, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }
    //?} elif >=1.21 {
    /*static <T extends Recipe<SingleRecipeInput>> ItemStack cook(GameTestHelper helper, RecipeType<T> type, ItemStack stack) {
        return craft(helper, type, new SingleRecipeInput(stack));
    }

    static ItemStack craftGrid(GameTestHelper helper, int width, int height, java.util.List<ItemStack> items) {
        return craft(helper, RecipeType.CRAFTING, net.minecraft.world.item.crafting.CraftingInput.of(width, height, items));
    }

    private static <I extends RecipeInput, T extends Recipe<I>> ItemStack craft(GameTestHelper helper, RecipeType<T> type, I input) {
        return helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(type, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
    *///?} else {
    /*static <T extends Recipe<net.minecraft.world.Container>> ItemStack cook(GameTestHelper helper, RecipeType<T> type,
                                                                       ItemStack stack) {
        return craft(helper, type, new net.minecraft.world.SimpleContainer(stack));
    }

    static ItemStack craftGrid(GameTestHelper helper, int width, int height, java.util.List<ItemStack> items) {
        return craft(helper, RecipeType.CRAFTING, new Grid(width, height, items));
    }

    private static <C extends net.minecraft.world.Container, T extends Recipe<C>> ItemStack craft(
            GameTestHelper helper, RecipeType<T> type, C input) {
        return helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(type, input, helper.getLevel())
                .map(recipe -> recipe.assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    // A crafting grid with no menu behind it, which is what 1.21's CraftingInput is.
    private static final class Grid extends net.minecraft.world.SimpleContainer
            implements net.minecraft.world.inventory.CraftingContainer {
        private final int width;
        private final int height;

        Grid(int width, int height, java.util.List<ItemStack> items) {
            super(items.toArray(new ItemStack[0]));
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public java.util.List<ItemStack> getItems() {
            java.util.List<ItemStack> items = new java.util.ArrayList<>();
            for (int slot = 0; slot < getContainerSize(); slot++) items.add(getItem(slot));
            return items;
        }
    }
    *///?}

    // A loot table by id. From 1.20.5 loot tables are a reloadable registry; before it the server's loot
    // data holds them by id.
    static net.minecraft.world.level.storage.loot.LootTable lootTable(GameTestHelper helper, net.minecraft.resources.Identifier id) {
        //? if >=1.20.5 {
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE, id));
        //?} else {
        /*return helper.getLevel().getServer().getLootData().getLootTable(id);
        *///?}
    }

    /**
     * A piglin entity type, for bartering loot. The constants moved from {@code EntityType} to
     * {@code EntityTypes} in 26.2.
     */
    static EntityType<?> piglinType() {
        //? if >=26.2 {
        return net.minecraft.world.entity.EntityTypes.PIGLIN;
        //?} else {
        /*return net.minecraft.world.entity.EntityType.PIGLIN;
        *///?}
    }

    /**
     * A survival player standing above the water and looking straight down at it.
     *
     * <p>Survival matters: {@code ItemUtils.createFilledResult} hands the filled container back
     * differently once the player has infinite materials, which would make the filling tests measure
     * the wrong thing. Gravity is off because nothing ticks during a test body.
     */
    static ServerPlayer playerAboveWater(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        BlockPos water = helper.absolutePos(WATER);
        player.snapTo(water.getX() + 0.5, water.getY() + 2.0, water.getZ() + 0.5, 0.0F, 90.0F);
        return player;
    }

    /**
     * A rideable entity type. The constants moved from {@code EntityType} to {@code EntityTypes} in
     * 26.2, and this is the only place the tests need one.
     */
    static EntityType<?> mountType() {
        //? if >=26.2 {
        return net.minecraft.world.entity.EntityTypes.PIG;
        //?} else {
        /*return net.minecraft.world.entity.EntityType.PIG;
        *///?}
    }

    /**
     * The first stack the player is carrying that has a sampled water quality on it. The filled
     * container does not always come back in the interaction hand, so the whole inventory is
     * searched.
     */
    static ItemStack findSampledWater(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (WaterPurity.isStamped(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * The player's own save tag, the one a disconnect writes into {@code playerdata}. Both loaders
     * hang their attachments off {@code Entity.saveWithoutId}, so this is the same path a quit takes.
     * The call takes a {@code ValueOutput} from 1.21.6 and a {@link CompoundTag} before it.
     */
    static CompoundTag savePlayer(ServerPlayer player) {
        //? if >=1.21.6 {
        net.minecraft.world.level.storage.TagValueOutput output =
                net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                        net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(output);
        return output.buildResult();
        //?} else {
        /*return player.saveWithoutId(new CompoundTag());
        *///?}
    }

    /** Reads a save tag back into {@code player}, the way a rejoin does. */
    static void loadPlayer(ServerPlayer player, CompoundTag tag) {
        //? if >=1.21.6 {
        player.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess(), tag));
        //?} else {
        /*player.load(tag);
        *///?}
    }
}
