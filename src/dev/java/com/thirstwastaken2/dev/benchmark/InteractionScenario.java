package com.thirstwastaken2.dev.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.dev.platform.DevLoader;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterInteractions;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.tooltip.ThirstTooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Every interaction the mod adds or hooks, performed by one player over and over and measured per
 * operation.
 *
 * <p>Operations that change the world or the player are reset before each run, outside the timed window.
 * Operations that change nothing are timed in batches, because a single one is quicker than the clock can
 * resolve. Each operation checks its outcome once, and the run fails if it had no effect: a fill refused by
 * spawn protection would otherwise report an impressively fast, meaningless number.
 */
final class InteractionScenario implements Stage {
    private static final int BATCH = 64;
    /** Thirst the drink operations start from, so each one visibly hydrates. */
    private static final int THIRSTY = 10;
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;
    private static final Runnable NOTHING = () -> { };
    private static final String CREATE_FLY_OPERATIONS = "com.thirstwastaken2.dev.benchmark.CreateFlyOperations";

    private record Operation(String name, String description, boolean batched, Runnable reset, Runnable body,
                             BooleanSupplier check) { }

    private final BenchmarkWorld world;
    private final int opsPerOperation;
    private final List<Operation> operations = new ArrayList<>();
    private final JsonArray results = new JsonArray();
    private BenchmarkPlayer player;
    private int current;
    private int performed;
    private boolean checked;
    private Metrics.Samples samples = new Metrics.Samples();
    private long measuredBytes;
    private long measuredOps;
    /** Keeps results reachable, so the JIT cannot discard work whose result is otherwise unused. */
    private Object sink;

    InteractionScenario(BenchmarkWorld world, int opsPerOperation) {
        this.world = world;
        this.opsPerOperation = opsPerOperation;
    }

    @Override
    public String name() {
        return "interactions";
    }

    @Override
    public String progress() {
        if (operations.isEmpty() || current >= operations.size()) return "";
        return operations.get(current).name() + " (" + (current + 1) + "/" + operations.size() + ")";
    }

    /** One entry per finished operation; partial when the run was cancelled. */
    JsonArray results() {
        return results;
    }

    @Override
    public boolean run(long deadlineNanos) {
        if (player == null) setUp();
        int warmup = Math.max(BATCH * 4, opsPerOperation / 5);
        while (current < operations.size()) {
            Operation operation = operations.get(current);
            while (performed < warmup + opsPerOperation) {
                if (System.nanoTime() >= deadlineNanos) return false;
                boolean measuring = performed >= warmup;
                int repetitions = operation.batched() ? BATCH : 1;
                operation.reset().run();
                long bytes = Metrics.allocatedBytes();
                long start = System.nanoTime();
                for (int r = 0; r < repetitions; r++) operation.body().run();
                long elapsed = System.nanoTime() - start;
                bytes = Metrics.allocatedBytes() - bytes;
                if (!checked) {
                    if (!operation.check().getAsBoolean()) {
                        throw new IllegalStateException("Interaction '" + operation.name()
                                + "' had no effect, so its timing would be meaningless (result " + sink
                                + ", main hand " + player.getMainHandItem() + " "
                                + data(player.getMainHandItem()) + ")");
                    }
                    checked = true;
                }
                if (measuring) {
                    samples.add(elapsed / repetitions);
                    measuredBytes += bytes;
                    measuredOps += repetitions;
                }
                performed += repetitions;
            }
            results.add(summarize(operation));
            current++;
            performed = 0;
            checked = false;
            samples = new Metrics.Samples();
            measuredBytes = 0;
            measuredOps = 0;
        }
        tearDown();
        return true;
    }

    /**
     * Whether a {@code use} call was refused. Before 1.21.2 it answers a holder carrying the result
     * and the stack together rather than the result itself, so comparing the answer to
     * {@code InteractionResult.FAIL} was false on every 1.21.1 run: the guard's own check, which
     * exists so an operation that stopped exercising the mod cannot report a fast number, failed the
     * whole run instead. {@code ItemStackMixin} forks the same call for the same reason.
     */
    private static boolean refused(Object result) {
        //? if >=1.21.2 {
        return result == InteractionResult.FAIL;
        //?} else {
        /*return result instanceof net.minecraft.world.InteractionResultHolder<?> holder
                && holder.getResult() == InteractionResult.FAIL;
        *///?}
    }

    private void setUp() {
        player = world.player(0);
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.setPose(Pose.STANDING);
        world.standAboveWater(player);
        ThirstManager.tickPlayer(player);
        ThirstManager.set(player, ThirstData.full());

        ServerLevel level = world.level;
        BlockPos water = world.water();
        BlockPos cauldron = world.cauldron();
        BlockHitResult waterHit = new BlockHitResult(Vec3.atCenterOf(water), Direction.UP, water, false);
        BlockHitResult cauldronHit = new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false);
        WaterQuality acceptable = WaterQuality.fresh(2);
        WaterQuality dirty = WaterQuality.fresh(0);
        ItemStack waterBottle = WaterPurity.setQuality(Vanilla.waterBottle(), acceptable);
        ItemStack fullWaterskin = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(fullWaterskin, acceptable, WaterskinItem.CAPACITY);
        ItemStack apple = new ItemStack(Items.APPLE);
        ItemStack[] everyItem = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .map(ItemStack::new)
                .toArray(ItemStack[]::new);
        List<Component> lines = new ArrayList<>();
        Consumer<Component> collect = lines::add;
        ItemStack[] held = new ItemStack[1];
        int[] cursor = new int[1];

        batched("sample_water", "WaterPurity.sampleAt: biome, temperature, altitude and the 5x3x5 neighbourhood scan",
                world::refillWater,
                () -> sink = WaterPurity.sampleAt(level, water),
                () -> sink instanceof WaterQuality);
        single("fill_bottle", "Glass bottle used on water: vanilla BottleItem#use plus the purity capture and stamp",
                () -> {
                    world.refillWater();
                    standing();
                    player.setItemInHand(HAND, new ItemStack(Items.GLASS_BOTTLE));
                },
                () -> sink = player.gameMode.useItem(player, level, player.getMainHandItem(), HAND),
                () -> ItemWaterData.hasGrade(player.getMainHandItem()));
        single("fill_bucket", "Bucket used on water: vanilla BucketItem#use plus the purity capture and stamp",
                () -> {
                    world.refillWater();
                    standing();
                    player.setItemInHand(HAND, new ItemStack(Items.BUCKET));
                },
                () -> sink = player.gameMode.useItem(player, level, player.getMainHandItem(), HAND),
                () -> player.getMainHandItem().is(Items.WATER_BUCKET)
                        && ItemWaterData.hasGrade(player.getMainHandItem()));
        single("fill_bowl", "Terracotta bowl scooping water (WaterInteractions.fillFromWater)",
                () -> {
                    world.refillWater();
                    standing();
                    player.setItemInHand(HAND, new ItemStack(ThirstItems.TERRACOTTA_BOWL));
                },
                () -> sink = WaterInteractions.fillFromWater(player, level, HAND),
                () -> player.getMainHandItem().is(ThirstItems.TERRACOTTA_WATER_BOWL));
        single("fill_waterskin", "Waterskin filling up from water and mixing its quality",
                () -> {
                    world.refillWater();
                    standing();
                    ItemStack waterskin = new ItemStack(ThirstItems.WATERSKIN);
                    WaterskinItem.addWater(waterskin, dirty, 1);
                    player.setItemInHand(HAND, waterskin);
                },
                () -> sink = WaterInteractions.fillFromWater(player, level, HAND),
                // Scooping from water fills a waterskin to capacity, not one serving at a time.
                () -> WaterskinItem.servings(player.getMainHandItem()) == WaterskinItem.CAPACITY);
        single("drink_water_bottle", "Finishing a water bottle: vanilla consumption, thirst gain and the purity roll",
                () -> {
                    thirsty();
                    held[0] = waterBottle.copy();
                },
                () -> sink = held[0].finishUsingItem(level, player),
                () -> ThirstManager.get(player).thirst() > THIRSTY);
        single("drink_waterskin", "One serving from a full waterskin",
                () -> {
                    thirsty();
                    held[0] = fullWaterskin.copy();
                },
                () -> sink = held[0].finishUsingItem(level, player),
                () -> ThirstManager.get(player).thirst() > THIRSTY);
        single("drink_by_hand", "Crouching with an empty hand on water (ThirstManager.drinkByHand)",
                () -> {
                    world.refillWater();
                    thirsty();
                    player.setItemInHand(HAND, ItemStack.EMPTY);
                    player.setPose(Pose.CROUCHING);
                },
                () -> sink = ThirstManager.drinkByHand(player, level, HAND, waterHit),
                () -> ThirstManager.get(player).thirst() > THIRSTY);
        single("cauldron_pour", "Pouring a bowl into a cauldron, including the deferred purity transfer",
                () -> {
                    standing();
                    world.resetCauldron();
                    player.setItemInHand(HAND, WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), dirty));
                },
                () -> {
                    sink = WaterInteractions.transferCauldronPurity(player, level, HAND, cauldronHit);
                    WaterInteractions.tick(world.server);
                },
                () -> level.getBlockState(cauldron).getValue(WaterPurity.BLOCK_PURITY) > 0);
        batched("full_bar_guard", "Using plain water at a full thirst bar, refused by the ItemStack#use hook",
                () -> {
                    standing();
                    ThirstManager.set(player, ThirstData.full());
                    player.setItemInHand(HAND, waterBottle.copy());
                },
                () -> sink = player.getMainHandItem().use(level, player, HAND),
                () -> refused(sink));
        // The droplet rows are asked for outright: the benchmark server has no AppleSkin, and a player who
        // has it pays for them on every hovered frame.
        batched("tooltip_water_bottle", "ThirstTooltip.appendTo for a water bottle: purity line and droplet rows",
                NOTHING,
                () -> {
                    lines.clear();
                    ThirstTooltip.appendTo(waterBottle, collect, true);
                },
                () -> !lines.isEmpty());
        batched("tooltip_waterskin", "ThirstTooltip.appendTo for a full waterskin: servings, purity and droplet rows",
                NOTHING,
                () -> {
                    lines.clear();
                    ThirstTooltip.appendTo(fullWaterskin, collect, true);
                },
                () -> !lines.isEmpty());
        batched("tooltip_food", "ThirstTooltip.appendTo for an apple: droplet rows only",
                NOTHING,
                () -> {
                    lines.clear();
                    ThirstTooltip.appendTo(apple, collect, true);
                },
                () -> !lines.isEmpty());
        batched("thirst_lookup", "ThirstApi.thirstValues cycling through every registered item",
                NOTHING,
                () -> {
                    sink = ThirstApi.thirstValues(everyItem[cursor[0]]);
                    cursor[0] = (cursor[0] + 1) % everyItem.length;
                },
                () -> everyItem.length > 0);
        batched("water_quality_read", "WaterPurity.quality on a stamped stack",
                NOTHING,
                () -> sink = WaterPurity.quality(waterBottle),
                () -> sink instanceof WaterQuality);
        single("waterskin_mix", "WaterskinItem.addWater mixing one dirty serving into clean water",
                () -> {
                    held[0] = new ItemStack(ThirstItems.WATERSKIN);
                    WaterskinItem.addWater(held[0], acceptable, 1);
                },
                () -> WaterskinItem.addWater(held[0], dirty, 1),
                () -> WaterskinItem.servings(held[0]) == 2);
        batched("exhaustion_mirror", "Player#causeFoodExhaustion through the mixin, buffered for the thirst tick",
                () -> {
                    standing();
                    ThirstManager.tickPlayer(player);
                    ThirstManager.set(player, ThirstData.full());
                },
                () -> player.causeFoodExhaustion(0.028F),
                () -> true);
        batched("thirst_tick_idle", "ThirstManager.tickPlayer for a player with nothing buffered",
                () -> {
                    ThirstManager.tickPlayer(player);
                    ThirstManager.set(player, ThirstData.full());
                },
                () -> ThirstManager.tickPlayer(player),
                () -> true);
        addCreateFly();
    }

    /**
     * The Create Fly operations, when Create Fly is installed and this build compiled them. They live in
     * {@code src/dev/createfly}, which only the nodes with {@code deps.create_fly} compile, so they are
     * reached by name.
     */
    private void addCreateFly() {
        if (!DevLoader.isModLoaded("create")) return;
        try {
            Class.forName(CREATE_FLY_OPERATIONS)
                    .getDeclaredMethod("add", InteractionScenario.class, BenchmarkWorld.class, BenchmarkPlayer.class)
                    .invoke(null, this, world, player);
        } catch (ClassNotFoundException missing) {
            BenchmarkRunner.LOGGER.warn("[ThirstBenchmark] Create is installed, but this build has no Create Fly operations");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not add the Create Fly operations", error);
        }
    }

    /** Drops the player and every operation, whose lambdas hold item stacks and the player. */
    void release() {
        operations.clear();
        player = null;
        sink = null;
    }

    private void tearDown() {
        ThirstManager.tickPlayer(player);
        ThirstManager.set(player, ThirstData.full());
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.setPose(Pose.STANDING);
        world.refillWater();
        world.resetCauldron();
        sink = null;
    }

    private void standing() {
        player.setPose(Pose.STANDING);
    }

    private void thirsty() {
        player.removeAllEffects();
        player.getInventory().clearContent();
        standing();
        ThirstManager.set(player, ThirstData.full().withLevels(THIRSTY, 0));
    }

    void batched(String name, String description, Runnable reset, Runnable body, BooleanSupplier check) {
        operations.add(new Operation(name, description, true, reset, body, check));
    }

    void single(String name, String description, Runnable reset, Runnable body, BooleanSupplier check) {
        operations.add(new Operation(name, description, false, reset, body, check));
    }

    private JsonObject summarize(Operation operation) {
        JsonObject json = new JsonObject();
        json.addProperty("name", operation.name());
        json.addProperty("description", operation.description());
        json.addProperty("timing", operation.batched() ? "mean of batches of " + BATCH : "each operation");
        json.addProperty("ops", measuredOps);
        json.add("microsPerOp", samples.summary(1_000.0));
        json.addProperty("bytesPerOp", Metrics.round(measuredOps == 0 ? 0.0 : (double) measuredBytes / measuredOps));
        double meanNanos = samples.mean();
        json.addProperty("opsPerMillisecond", Metrics.round(meanNanos <= 0.0 ? 0.0 : 1_000_000.0 / meanNanos));
        return json;
    }

    /** What a stack carries beyond its item, for a failure message: its components, or before 1.20.5 its tag. */
    private static Object data(net.minecraft.world.item.ItemStack stack) {
        //? if >=1.20.5 {
        return stack.getComponentsPatch();
        //?} else {
        /*return stack.getTag();
        *///?}
    }
}
