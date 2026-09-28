package com.thirstwastaken2.dev.benchmark;

import com.google.gson.JsonObject;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Arrays;

/**
 * {@code count} players going about an ordinary session, one server tick at a time.
 *
 * <p>Every tick each player charges the exhaustion vanilla would (sprinting, swimming, jumping, fighting,
 * mining, the Hunger effect), passes the sprint gate, runs vanilla's food tick with the mod's regeneration
 * hooks and the thirst tick, and has any changed attachment encoded for sync. Every 30 seconds each player
 * also fills a bowl and, half a cycle later, drinks one. Half the players wear enchanted armour, which is
 * what makes the exhaustion modifier expensive; a tenth carry Hunger, a twentieth Nausea or Fire Resistance.
 *
 * <p>Each kind of work is timed as its own section across all players. Warm-up ticks run the same work
 * unrecorded, so the JIT has compiled it before anything counts. Upkeep that only keeps the simulation
 * going, such as topping thirst up before it runs dry, stays outside every timed section.
 */
final class TickScenario implements Stage {
    static final String[] SECTIONS = {"exhaustion", "sprint_gate", "interactions", "food_tick", "thirst_tick", "sync_encode"};
    private static final int EXHAUSTION = 0;
    private static final int SPRINT_GATE = 1;
    private static final int INTERACTIONS = 2;
    private static final int FOOD_TICK = 3;
    private static final int THIRST_TICK = 4;
    private static final int SYNC_ENCODE = 5;

    /** Sprinting at 5.6 m/s covers 28 cm a tick, and vanilla charges 0.1 per metre sprinted. */
    private static final float SPRINT = 0.1F * 28 * 0.01F;
    /** Swimming at about 4 m/s covers 20 cm a tick, and vanilla charges 0.01 per metre swum. */
    private static final float SWIM = 0.01F * 20 * 0.01F;
    private static final float SPRINT_JUMP = 0.2F;
    private static final float JUMP = 0.05F;
    private static final float ATTACK = 0.1F;
    private static final float MINE = 0.005F;
    /** What the Hunger effect charges every tick at amplifier 0. */
    private static final float HUNGER = 0.005F;
    /** Each player fills a bowl and, half a cycle later, drinks one, once per this many ticks. */
    private static final int INTERACTION_INTERVAL = 600;
    private static final int TICK_BUDGET_NANOS = 50_000_000;
    private static final WaterQuality PURIFIED = WaterQuality.fresh(WaterPurity.MAX);

    private final BenchmarkWorld world;
    private final int count;
    private final int warmupTicks;
    private final int measuredTicks;
    private final BenchmarkPlayer[] players;
    private final ThirstData[] before;
    private final int[] changed;
    private final long[] tickNanos = new long[SECTIONS.length];
    private final long[] tickBytes = new long[SECTIONS.length];
    private final long[] sectionNanos = new long[SECTIONS.length];
    private final long[] sectionBytes = new long[SECTIONS.length];
    private final Metrics.Samples totals = new Metrics.Samples();
    private FriendlyByteBuf buffer;
    private Holder<Enchantment> protection;
    private Holder<Enchantment> unbreaking;
    private Holder<Enchantment> mending;
    private int prepared;
    private int tick;
    private long changedPlayers;
    private long interactions;
    private long gcCountAtStart;
    private long gcMillisAtStart;
    /** Keeps results reachable, so the JIT cannot discard work whose result is otherwise unused. */
    private Object sink;
    private int sprintAllowed;
    private JsonObject result;

    TickScenario(BenchmarkWorld world, int count, int warmupTicks, int measuredTicks) {
        this.world = world;
        this.count = count;
        this.warmupTicks = warmupTicks;
        this.measuredTicks = measuredTicks;
        this.players = new BenchmarkPlayer[count];
        this.before = new ThirstData[count];
        this.changed = new int[count];
    }

    @Override
    public String name() {
        return "tick scenario with " + count + (count == 1 ? " player" : " players");
    }

    @Override
    public String progress() {
        if (prepared < count) return "preparing players " + prepared + "/" + count;
        return "tick " + tick + "/" + (warmupTicks + measuredTicks) + (tick < warmupTicks ? ", warming up" : "");
    }

    /** The scenario's report section, or null while it has not finished. */
    JsonObject result() {
        return result;
    }

    @Override
    public boolean run(long deadlineNanos) {
        if (buffer == null) {
            buffer = new FriendlyByteBuf(Unpooled.buffer(64));
            // Enchantments became data in 1.21; before it they are registered objects, and Protection
            // had a longer name.
            //? if >=1.21 {
            protection = world.enchantment(Enchantments.PROTECTION);
            unbreaking = world.enchantment(Enchantments.UNBREAKING);
            mending = world.enchantment(Enchantments.MENDING);
            //?} else {
            /*protection = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.wrapAsHolder(Enchantments.ALL_DAMAGE_PROTECTION);
            unbreaking = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.wrapAsHolder(Enchantments.UNBREAKING);
            mending = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.wrapAsHolder(Enchantments.MENDING);
            *///?}
        }
        while (prepared < count) {
            if (System.nanoTime() >= deadlineNanos) return false;
            players[prepared] = prepare(prepared);
            prepared++;
        }
        while (System.nanoTime() < deadlineNanos) {
            if (tick == warmupTicks) {
                gcCountAtStart = Metrics.gcCount();
                gcMillisAtStart = Metrics.gcMillis();
            }
            simulate(tick >= warmupTicks);
            if (++tick >= warmupTicks + measuredTicks) {
                result = summarize();
                release();
                return true;
            }
        }
        return false;
    }

    /** Drops every reference to players and buffers. Safe to call more than once, and on an unfinished scenario. */
    void release() {
        Arrays.fill(players, null);
        Arrays.fill(before, null);
        if (buffer != null && buffer.refCnt() > 0) buffer.release();
        sink = null;
    }

    private BenchmarkPlayer prepare(int index) {
        BenchmarkPlayer player = world.player(index);
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.setPose(Pose.STANDING);
        world.standInField(player, index);
        boolean armoured = index % 2 == 0;
        player.setItemSlot(EquipmentSlot.HEAD, armoured ? armour(Items.DIAMOND_HELMET) : ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, armoured ? armour(Items.DIAMOND_CHESTPLATE) : ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.LEGS, armoured ? armour(Items.DIAMOND_LEGGINGS) : ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.FEET, armoured ? armour(Items.DIAMOND_BOOTS) : ItemStack.EMPTY);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.setHealth(16.0F);
        ThirstManager.set(player, ThirstData.full().withLevels(8 + index % 13, index % 6));
        if (hungry(index)) player.addEffect(new MobEffectInstance(MobEffects.HUNGER, MobEffectInstance.INFINITE_DURATION, 0));
        if (index % 20 == 7) player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, MobEffectInstance.INFINITE_DURATION, 0));
        if (index % 20 == 11) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0));
        }
        return player;
    }

    private ItemStack armour(Item item) {
        ItemStack stack = new ItemStack(item);
        //? if >=1.21 {
        stack.enchant(protection, 4);
        stack.enchant(unbreaking, 3);
        stack.enchant(mending, 1);
        //?} else {
        /*stack.enchant(protection.value(), 4);
        stack.enchant(unbreaking.value(), 3);
        stack.enchant(mending.value(), 1);
        *///?}
        return stack;
    }

    private void simulate(boolean measuring) {
        int t = tick;
        for (int i = 0; i < count; i++) upkeep(players[i], i, t);
        for (int i = 0; i < count; i++) before[i] = ThirstManager.get(players[i]);

        long bytes = Metrics.allocatedBytes();
        long nanos = System.nanoTime();
        for (int i = 0; i < count; i++) exhaust(players[i], i, t);
        mark(EXHAUSTION, nanos, bytes);

        bytes = Metrics.allocatedBytes();
        nanos = System.nanoTime();
        for (int i = 0; i < count; i++) sprintAllowed += players[i].canSprint() ? 1 : 0;
        mark(SPRINT_GATE, nanos, bytes);

        bytes = Metrics.allocatedBytes();
        nanos = System.nanoTime();
        int interactionsThisTick = 0;
        for (int i = 0; i < count; i++) {
            int phase = Math.floorMod(t + i * 7, INTERACTION_INTERVAL);
            if (phase == 0) {
                sink = WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL),
                        WaterPurity.sampleAt(world.level, world.water()));
                interactionsThisTick++;
            } else if (phase == INTERACTION_INTERVAL / 2) {
                sink = WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), PURIFIED)
                        .finishUsingItem(world.level, players[i]);
                interactionsThisTick++;
            }
        }
        mark(INTERACTIONS, nanos, bytes);

        bytes = Metrics.allocatedBytes();
        nanos = System.nanoTime();
        for (int i = 0; i < count; i++) players[i].getFoodData().tick(players[i]);
        mark(FOOD_TICK, nanos, bytes);

        bytes = Metrics.allocatedBytes();
        nanos = System.nanoTime();
        for (int i = 0; i < count; i++) ThirstManager.tickPlayer(players[i]);
        mark(THIRST_TICK, nanos, bytes);

        // A changed attachment is what Fabric turns into a sync packet for a real client.
        int changedCount = 0;
        for (int i = 0; i < count; i++) {
            if (ThirstManager.get(players[i]) != before[i]) changed[changedCount++] = i;
        }

        bytes = Metrics.allocatedBytes();
        nanos = System.nanoTime();
        for (int k = 0; k < changedCount; k++) {
            buffer.clear();
            ThirstManager.get(players[changed[k]]).write(buffer);
        }
        mark(SYNC_ENCODE, nanos, bytes);

        if (!measuring) return;
        long total = 0;
        for (int s = 0; s < SECTIONS.length; s++) {
            total += tickNanos[s];
            sectionNanos[s] += tickNanos[s];
            sectionBytes[s] += tickBytes[s];
        }
        totals.add(total);
        changedPlayers += changedCount;
        interactions += interactionsThisTick;
    }

    /** Closes a timed section. The allocation counter is read outside the timed window. */
    private void mark(int section, long startNanos, long startBytes) {
        tickNanos[section] = System.nanoTime() - startNanos;
        tickBytes[section] = Metrics.allocatedBytes() - startBytes;
    }

    /** Keeps the simulation going without touching anything that is measured. */
    private static void upkeep(BenchmarkPlayer player, int index, int tick) {
        ThirstData data = ThirstManager.get(player);
        if (data.thirst() <= 6) ThirstManager.set(player, data.withLevels(ThirstData.MAX, 6));
        if ((tick + index) % 40 == 0) {
            // Keeps natural regeneration, and with it the mod's heal hooks, firing.
            player.setHealth(16.0F);
            player.getFoodData().setFoodLevel(20);
        }
    }

    /** The exhaustion vanilla charges this player this tick, each call going through the mod's mixin. */
    private static void exhaust(BenchmarkPlayer player, int index, int tick) {
        int kind = index % 10;
        int beat = tick + index;
        if (kind < 6) {
            player.causeFoodExhaustion(SPRINT);
            if (beat % 12 == 0) player.causeFoodExhaustion(SPRINT_JUMP);
        } else if (kind < 8) {
            player.causeFoodExhaustion(SWIM);
        } else if (beat % 40 == 0) {
            player.causeFoodExhaustion(JUMP);
        }
        if (kind == 3 && beat % 15 == 0) player.causeFoodExhaustion(ATTACK);
        if (kind == 4 && beat % 8 == 0) player.causeFoodExhaustion(MINE);
        if (hungry(index)) player.causeFoodExhaustion(HUNGER);
    }

    private static boolean hungry(int index) {
        return index % 10 == 5;
    }

    private JsonObject summarize() {
        JsonObject json = new JsonObject();
        json.addProperty("players", count);
        json.addProperty("warmupTicks", warmupTicks);
        json.addProperty("measuredTicks", measuredTicks);
        double meanNanos = totals.mean();
        json.add("msPerTick", totals.summary(1_000_000.0));
        json.addProperty("tickBudgetPercent", Metrics.round(meanNanos / TICK_BUDGET_NANOS * 100.0));
        json.addProperty("microsPerPlayerPerTick", Metrics.round(meanNanos / count / 1_000.0));
        long bytes = sum(sectionBytes);
        json.addProperty("allocatedBytesPerTick", Metrics.round((double) bytes / measuredTicks));
        json.addProperty("allocatedBytesPerPlayerPerTick", Metrics.round((double) bytes / measuredTicks / count));
        json.addProperty("syncPacketsPerPlayerPerSecond", Metrics.round((double) changedPlayers / measuredTicks / count * 20.0));
        json.addProperty("interactionsPerTick", Metrics.round((double) interactions / measuredTicks));

        JsonObject sections = new JsonObject();
        long nanos = sum(sectionNanos);
        for (int s = 0; s < SECTIONS.length; s++) {
            JsonObject section = new JsonObject();
            section.addProperty("microsPerTick", Metrics.round(sectionNanos[s] / (double) measuredTicks / 1_000.0));
            section.addProperty("bytesPerTick", Metrics.round(sectionBytes[s] / (double) measuredTicks));
            section.addProperty("sharePercent", nanos == 0 ? 0.0 : Metrics.round(100.0 * sectionNanos[s] / nanos));
            sections.add(SECTIONS[s], section);
        }
        json.add("sections", sections);

        JsonObject gc = new JsonObject();
        gc.addProperty("collections", Metrics.gcCount() - gcCountAtStart);
        gc.addProperty("millis", Metrics.gcMillis() - gcMillisAtStart);
        json.add("gcDuringMeasurement", gc);
        return json;
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long value : values) total += value;
        return total;
    }
}
