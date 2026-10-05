package com.thirstwastaken2.gametest;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.DistillerBlockEntity;
import com.thirstwastaken2.block.ThirstBlockEntities;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The version seams a machine with a GUI stands on, each on its own: the block entity type registers and
 * belongs to the distiller, a block entity's saved values and items come back from a save the way the
 * game loads one, a menu type builds its menu, and fuel burns as long as it does in a furnace. The
 * screen's registration is client side and is checked in a client.
 */
public final class MachineSeamsGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    @GameTest
    public void theDistillersBlockEntityTypeIsRegisteredForIt(GameTestHelper helper) {
        TestFixtures.check(helper, ThirstWasTaken2.id("copper_distiller").equals(
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ThirstBlockEntities.COPPER_DISTILLER)),
                "the distiller's block entity type should be registered as thirstwastaken2:copper_distiller, got "
                        + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ThirstBlockEntities.COPPER_DISTILLER));
        TestFixtures.check(helper, ThirstBlockEntities.COPPER_DISTILLER.isValid(ThirstBlocks.COPPER_DISTILLER.defaultBlockState()),
                "the type should belong to the distiller block");
        TestFixtures.check(helper, !ThirstBlockEntities.COPPER_DISTILLER.isValid(ThirstBlocks.COOLING_TUB.defaultBlockState()),
                "the type should not belong to a lone cooling tub");
        helper.succeed();
    }

    @GameTest
    public void aSavedBlockEntityComesBackFromItsSave(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(POS);
        BlockState state = ThirstBlocks.COPPER_DISTILLER.defaultBlockState();
        DistillerBlockEntity saved = new DistillerBlockEntity(pos, state);
        saved.pour(4, WaterQuality.fresh(1));
        saved.pour(2, WaterQuality.SALT);
        saved.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL, 5));
        saved.setItem(DistillerBlockEntity.WATER_IN, new ItemStack(Items.BUCKET));

        BlockEntity loaded = reload(helper, saved, pos, state);

        TestFixtures.check(helper, loaded instanceof DistillerBlockEntity,
                "the save should load back as a distiller's block entity, got " + loaded);
        DistillerBlockEntity distiller = (DistillerBlockEntity) loaded;
        TestFixtures.check(helper, distiller.boilerServings() == 6 && WaterQuality.SALT.equals(distiller.boilerQuality()),
                "the boiler should come back with 6 salty servings, got " + distiller.boilerServings() + " of "
                        + distiller.boilerQuality());
        TestFixtures.check(helper, ItemStack.matches(distiller.getItem(DistillerBlockEntity.FUEL), new ItemStack(Items.COAL, 5))
                        && distiller.getItem(DistillerBlockEntity.WATER_IN).is(Items.BUCKET)
                        && distiller.getItem(DistillerBlockEntity.EMPTY_IN).isEmpty(),
                "the slots should come back as they were saved, got " + distiller.getItem(DistillerBlockEntity.FUEL)
                        + " and " + distiller.getItem(DistillerBlockEntity.WATER_IN));
        helper.succeed();
    }

    @GameTest
    public void fuelBurnsAsLongAsInAFurnace(GameTestHelper helper) {
        DistillerBlockEntity distiller = new DistillerBlockEntity(helper.absolutePos(POS),
                ThirstBlocks.COPPER_DISTILLER.defaultBlockState());
        distiller.setLevel(helper.getLevel());

        int coal = Loader.burnTime(distiller, new ItemStack(Items.COAL));
        int stick = Loader.burnTime(distiller, new ItemStack(Items.STICK));
        int stone = Loader.burnTime(distiller, new ItemStack(Items.STONE));

        TestFixtures.check(helper, coal == 1600 && stick == 100,
                "coal should burn 1600 ticks and a stick 100, got " + coal + " and " + stick);
        TestFixtures.check(helper, stone == 0, "stone should not burn, got " + stone);
        helper.succeed();
    }

    @GameTest
    public void aMenuTypeBuildsItsMenu(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        @SuppressWarnings("unchecked")
        MenuType<ProbeMenu>[] type = new MenuType[1];
        type[0] = Vanilla.menuType((id, inventory) -> new ProbeMenu(type[0], id, inventory));

        ProbeMenu menu = type[0].create(7, player.getInventory());

        TestFixtures.check(helper, menu.containerId == 7 && menu.getType() == type[0] && menu.inventory == player.getInventory(),
                "the menu should be built for window 7 and the player's inventory, of its own type");
        helper.succeed();
    }

    /** Saves {@code entity} as the chunk does, its id included, and loads it back the way the game does. */
    private static BlockEntity reload(GameTestHelper helper, BlockEntity entity, BlockPos pos, BlockState state) {
        //? if >=1.20.5 {
        CompoundTag tag = entity.saveWithFullMetadata(helper.getLevel().registryAccess());
        return BlockEntity.loadStatic(pos, state, tag, helper.getLevel().registryAccess());
        //?} else {
        /*CompoundTag tag = entity.saveWithFullMetadata();
        return BlockEntity.loadStatic(pos, state, tag);
        *///?}
    }

    /** A menu with no slots, to see what the type hands its factory. */
    private static final class ProbeMenu extends AbstractContainerMenu {
        private final Inventory inventory;

        ProbeMenu(MenuType<?> type, int id, Inventory inventory) {
            super(type, id);
            this.inventory = inventory;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
