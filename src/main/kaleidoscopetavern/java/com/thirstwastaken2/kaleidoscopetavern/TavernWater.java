package com.thirstwastaken2.kaleidoscopetavern;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.platform.SavedPositions;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.ObjIntConsumer;

/**
 * The only place that reads or writes a grade for Kaleidoscope Tavern: what goes into the barrel and what
 * it is refused, what the tap copies into a cauldron or a placed bottle, the grade a placed water bottle
 * keeps beside it, and the sea water the shaker refuses. Every one of these kept only a fluid or a block,
 * so a Dirty bucket came back out of the barrel at {@code defaultQuality}, a tap on a Dirty cauldron made
 * Clean ones, and a sea-water bottle put down and picked up again came back fresh.
 */
public final class TavernWater {
    /** One int on the barrel, {@link WaterPurity#storedValue}: 1-4 for the four grades, 5 for sea water. */
    private static final String KEY = ThirstWasTaken2.MOD_ID + ":water_quality";

    /** The {@link SavedPositions} name of the grades placed water bottles keep: one file per dimension. */
    private static final String PLACED_BOTTLES = "placed_water_bottles";

    private static final Identifier WATER_BOTTLE = Identifier.fromNamespaceAndPath("kaleidoscope_tavern", "water_bottle");
    /** The placed water bottle block, looked up once: the drop hook asks on every block broken. */
    private static volatile Block waterBottle;

    private static final int BLOCK_UPDATE_FLAGS = 3;

    private TavernWater() { }

    // The barrel.

    /** The grade of the water in {@code stack}, or {@code null} when it holds none. Read it before the call that spends it. */
    public static WaterQuality of(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack) ? WaterPurity.quality(stack) : null;
    }

    /** Whether the barrel's tank holds water, from the fluid the mod's own recipe check reads. */
    public static boolean isWater(Fluid fluid) {
        return fluid == Fluids.WATER;
    }

    /**
     * Whether the barrel refuses {@code stack} on its way in, telling {@code user} why:
     *
     * <ul>
     *     <li><b>sea water</b>, on every build. The barrel would ferment it into rum as safe as any other,
     *     where every other brewing block this mod knows refuses it or brews nothing from it;</li>
     *     <li><b>this mod's own containers on Refabricated</b>, whose barrel hands back an empty bucket for
     *     any container that is not one ({@code FluidUtils.onConsumed}) and fills a full water bucket into
     *     the hand for a serving drawn ({@code fillItem}), so a canteen poured in came back a bucket and an
     *     empty one held out came back a bucket of water. Buckets and bottles still go in.</li>
     * </ul>
     */
    public static boolean refuses(LivingEntity user, ItemStack stack) {
        if (refusesContainer(stack)) {
            tell(user, "thirstwastaken2.message.barrel_takes_buckets");
            return true;
        }
        WaterQuality poured = of(stack);
        if (poured == null || !poured.salty()) return false;
        tell(user, "thirstwastaken2.message.no_sea_water_in_barrel");
        return true;
    }

    /** Whether the barrel would turn {@code stack}, one of this mod's own containers, into a bucket; see {@link #refuses}. */
    public static boolean refusesContainer(ItemStack stack) {
        return KaleidoscopeTavernPresence.isRefabricated() && WaterContainers.handles(stack);
    }

    /**
     * What the barrel holds once {@code poured} went in on top of {@code held}: the worse of the two, as a
     * cauldron keeps, or {@code poured} into a barrel that held no water it knew the grade of. Another
     * fluid poured in, {@code null}, leaves nothing to keep.
     */
    public static WaterQuality pouredOnto(WaterQuality held, WaterQuality poured) {
        if (poured == null || held == null) return poured;
        return WaterQuality.worse(held, poured);
    }

    /**
     * Writes {@code quality} through {@code putInt}, a {@code CompoundTag}'s before 1.21.6 or a
     * {@code ValueOutput}'s from it, so the mixin's version forks differ only in their signature.
     */
    public static void save(ObjIntConsumer<String> putInt, WaterQuality quality) {
        if (quality != null) putInt.accept(KEY, WaterPurity.storedValue(quality));
    }

    /** What {@link #save} wrote, or {@code null}: a barrel filled before the integration existed has no grade. */
    public static WaterQuality load(IntReader getIntOr) {
        return fromStored(getIntOr.read(KEY, WaterPurity.BLOCK_UNSET));
    }

    /** An int under a key, or the fallback when there is none. */
    @FunctionalInterface
    public interface IntReader {
        int read(String key, int fallback);
    }

    // The tap.

    /**
     * A tap on a water cauldron has filled the cauldron or the empty bottle below it, which the mod makes
     * from nothing, unset and so read as {@code defaultQuality}. It gets the source cauldron's grade, the
     * worse of it and what the cauldron below already held.
     */
    public static void tappedCauldron(Level level, BlockPos tapPos, BlockState source, BlockState before) {
        tapped(level, tapPos.below(), before, WaterPurity.storedQuality(source));
    }

    /**
     * A tap on a waterlogged block has filled what is below it. The water is the world's, so it is sampled
     * where it lies, as a bucket filled there is: a waterlogged slab in the sea gives sea water.
     */
    public static void tappedWorld(Level level, BlockPos tapPos, BlockState tapState, BlockState before) {
        tapped(level, tapPos.below(), before, sampleSource(level, tapPos, tapState));
    }

    /**
     * The water a tap at {@code tapPos} draws from the waterlogged block behind it, sampled where it lies,
     * or {@code null} when the tap's facing cannot be read. Also how Kaleidoscope Cookery's integration
     * grades a tap into the stockpot.
     */
    public static WaterQuality sampleSource(Level level, BlockPos tapPos, BlockState tapState) {
        if (!tapState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return null;
        Direction facing = tapState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return WaterPurity.sampleAt(level, tapPos.relative(facing.getOpposite()));
    }

    private static void tapped(Level level, BlockPos below, BlockState before, WaterQuality source) {
        if (level.isClientSide()) return;
        BlockState after = level.getBlockState(below);
        // Blockstates are canonical instances, so an unchanged block is the same object: nothing was filled.
        if (after == before) return;
        if (after.is(Blocks.WATER_CAULDRON) && after.hasProperty(WaterPurity.BLOCK_PURITY)) {
            // Either side may be unset, a cauldron nothing graded was poured into; the other then decides.
            WaterQuality held = WaterPurity.storedQuality(before);
            WaterQuality quality = source == null ? held : held == null ? source : WaterQuality.worse(held, source);
            if (quality == null) return;
            level.setBlock(below, after.setValue(WaterPurity.BLOCK_PURITY, WaterPurity.storedValue(quality)), BLOCK_UPDATE_FLAGS);
        } else if (isWaterBottle(after) && level instanceof ServerLevel server) {
            keepPlaced(server, below, source);
        }
    }

    // The placed water bottle.

    /**
     * A water bottle put down as a block, by the mod's shift-use or by a tap. The block has no block entity
     * and its loot table drops a plain water bottle, so its grade is kept beside it, in the level, until it
     * drops. An unstamped bottle clears what an earlier one at the same place left there.
     */
    public static void placed(Level level, BlockPos pos, BlockState state, ItemStack bottle) {
        if (!(level instanceof ServerLevel server) || !isWaterBottle(state)) return;
        keepPlaced(server, pos, WaterPurity.isStamped(bottle) ? WaterPurity.quality(bottle) : null);
    }

    /**
     * What a placed water bottle drops, broken, blown up, pushed or picked up by hand, stamped with the grade
     * it was put down with; every other block's drops are handed back as they are. Called for every block
     * that drops anything, so the check that it is not the bottle comes first.
     */
    public static List<ItemStack> dropped(List<ItemStack> drops, BlockState state, LootParams.Builder params) {
        if (!isWaterBottle(state)) return drops;
        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) return drops;
        WaterQuality quality = fromStored(SavedPositions.of(params.getLevel(), PLACED_BOTTLES).remove(BlockPos.containing(origin)));
        if (quality == null) return drops;
        for (ItemStack drop : drops) {
            if (WaterPurity.isWaterContainer(drop)) WaterPurity.setQuality(drop, quality);
        }
        return drops;
    }

    private static void keepPlaced(ServerLevel level, BlockPos pos, WaterQuality quality) {
        SavedPositions placed = SavedPositions.of(level, PLACED_BOTTLES);
        if (quality == null) placed.remove(pos);
        else placed.put(pos, WaterPurity.storedValue(quality));
    }

    private static boolean isWaterBottle(BlockState state) {
        Block block = waterBottle;
        if (block == null) {
            // Asked only once the registries are frozen: a block is broken or placed in a world.
            block = BuiltInRegistries.BLOCK.getOptional(WATER_BOTTLE).orElse(Blocks.AIR);
            waterBottle = block;
        }
        return block != Blocks.AIR && state.is(block);
    }

    // The shaker.

    /**
     * Whether the shaker refuses {@code stack} as an ingredient, telling {@code user} why: a sea-water bottle,
     * which would come out of the shaker as a safe cocktail. Fresh water of any grade goes in, as it does into
     * any brewed drink.
     */
    public static boolean shakerRefuses(ItemStack stack, LivingEntity user) {
        WaterQuality quality = of(stack);
        if (quality == null || !quality.salty()) return false;
        tell(user, "thirstwastaken2.message.no_sea_water_in_shaker");
        return true;
    }

    private static WaterQuality fromStored(int stored) {
        if (stored == WaterPurity.BLOCK_SALT) return WaterQuality.SALT;
        return stored > WaterPurity.BLOCK_UNSET ? WaterQuality.fresh(stored - 1) : null;
    }

    /** Both sides run the barrel's and the shaker's calls; only the server's tells the player, once. */
    private static void tell(LivingEntity user, String key) {
        if (user instanceof ServerPlayer player) Vanilla.sendOverlayMessage(player, Component.translatable(key));
    }
}
