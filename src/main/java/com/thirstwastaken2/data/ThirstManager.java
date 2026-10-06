package com.thirstwastaken2.data;

import com.thirstwastaken2.advancement.ThirstAdvancements;
import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.api.ThirstEvents;
import com.thirstwastaken2.compat.FarmersDelight;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.damage.ThirstDamageTypes;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.effect.UpsetStomach;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.ToDoubleFunction;

public final class ThirstManager {
    /** Matches the original mod's syncTimer cadence for peaceful regeneration. */
    private static final int SLOW_TICK_INTERVAL = 11;
    private static final int DAMAGE_INTERVAL = 40;
    /**
     * Thirst exhaustion every survival player spends per tick, whatever they do: one point of thirst a
     * minute at four exhaustion a point, before climate. Not in the original, where a player standing
     * still never got thirsty. Mining, building and waiting in a cave all cost water now.
     */
    private static final float BASELINE_EXHAUSTION = 4.0F / 1200.0F;
    /**
     * The climate curve: a factor of 0.8 at a temperate 0.5, a quarter more for every degree of the
     * biome's temperature, 0.15 more where it does not rain, held between the bounds. Monotonic, so a
     * slightly warmer biome never drains less, which the original's curve did past its fold at 1.
     */
    private static final float CLIMATE_BASE = 0.8F;
    private static final float CLIMATE_PER_DEGREE = 0.25F;
    private static final float CLIMATE_TEMPERATE = 0.5F;
    private static final float CLIMATE_DRY = 0.15F;
    private static final float CLIMATE_MIN = 0.65F;
    private static final float CLIMATE_MAX = 1.35F;
    /** The bounds the season's factor may push the climate's to. */
    private static final float SEASONAL_MIN = 0.6F;
    private static final float SEASONAL_MAX = 1.5F;
    /** Nausea's extra drain per tick, from the original DEPLETES_WHEN_NAUSEA branch. */
    private static final float NAUSEA_EXHAUSTION = 0.06F;
    /** What {@code HungerMobEffect#applyEffectTick} charges per amplifier level, every tick. */
    private static final float HUNGER_EXHAUSTION = 0.005F;
    /**
     * Parched's drain per tick per level, the thirst counterpart of {@link #HUNGER_EXHAUSTION}: four points
     * of thirst a minute at I and eight at II. It is charged as illness, so the climate modifier does not
     * scale it; only a {@code ThirstEvents.EXHAUSTION} listener can. Over the 30 seconds sea water gives,
     * Parched II costs 4 thirst.
     */
    private static final float PARCHED_EXHAUSTION = 16.0F / 1200.0F;
    /**
     * How long a computed exhaustion modifier is reused. Climate, armour and Fire Resistance change far
     * more slowly than vanilla charges exhaustion, and it takes seconds of exhaustion to spend a single
     * point, so a lag of one second cannot be seen.
     */
    private static final int MODIFIER_REFRESH_TICKS = 20;
    /**
     * The tick only writes exhaustion, and so only syncs it, once it has moved into another step of this size
     * or spent a point. The client draws exhaustion as the HUD's partly drained droplet, which changes at 0 and
     * 2, and as AppleSkin's 81 px strip; a quarter point keeps both readable while a sprinting player costs
     * about two sync packets a second instead of one every tick. The unwritten remainder stays on the
     * player's {@link ExhaustionTracker}, so the drain itself is exact.
     *
     * <p>Diverges from the original mod, which saved every change: a player who leaves loses less than one
     * step of exhaustion, a sixteenth of a point.
     */
    private static final float SYNC_STEP = 0.25F;
    /** Vanilla's sprint gate is foodLevel > 6; the original mod applied the same cut-off to thirst. */
    private static final int SPRINT_THIRST_THRESHOLD = 6;
    /** Droplets a sip by hand throws up, a few less than a bottle poured out. */
    private static final int HAND_DRINK_SPLASHES = 4;
    /**
     * Thirst a sip by hand restores: three, the original's one being worth less than the click. Its
     * quenched is cut by the water's grade, so bad water by hand still does not last.
     */
    private static final int HAND_DRINK_THIRST = 3;
    private static final int HAND_DRINK_QUENCHED = 2;
    /** The original's default multiplier where water evaporates, the Nether and anywhere like it. */
    private static final float SCORCHING_MODIFIER = 3.0F;
    /** Fire Resistance halves dehydration, as the original's default did. */
    private static final float FIRE_RESISTANCE_MODIFIER = 0.5F;
    /**
     * Where the climate's temperature comes from when an integration knows it better than the biome
     * does, in the biome's units, or NaN where it does not know. Null without one. Cold Sweat's sets it
     * at init. Not API: no other mod may rely on it, and nothing but the main thread's init writes it.
     */
    private static ToDoubleFunction<Player> climateTemperature;
    /** The season's part in the climate, or null without a seasons mod. Serene Seasons' sets it at init. */
    private static SeasonalClimate seasonalClimate;

    private ThirstManager() { }

    public static ThirstData get(Player player) {
        return ThirstData.STORAGE.get(player);
    }

    public static void set(Player player, ThirstData data) {
        ThirstData.STORAGE.set(player, data);
    }

    /** Applies exhaustion straight away, for one-off sources such as drinking salt water. */
    public static void addExhaustion(Player player, float amount) {
        if (!drains(player, amount)) return;
        ThirstData data = get(player);
        if (!data.enabled()) return;
        set(player, data.addExhaustion(amount * exhaustionModifier(player)));
    }

    /**
     * Mirrors vanilla food exhaustion. Vanilla charges it on nearly every tick a player sprints, swims,
     * jumps or fights, sometimes more than once a tick, and every attachment write sends a sync packet.
     * The raw amount is therefore only collected here and applied once per tick by {@link #tick}.
     */
    public static void mirrorExhaustion(Player player, float amount) {
        if (drains(player, amount)) ExhaustionTracker.of(player).pending += amount;
    }

    /** Riding a mount does not dehydrate you, matching the original's isSitting guard. */
    private static boolean drains(Player player, float amount) {
        return amount != 0.0F && !player.level().isClientSide()
                && !player.getAbilities().invulnerable && !player.isPassenger();
    }

    public static void drink(Player player, int thirst, int quenched) {
        if (!player.level().isClientSide()) set(player, get(player).drink(thirst, quenched));
    }

    /**
     * Whether thirst lets {@code player} sprint, mirroring vanilla's food cut-off of more than 6. Vanilla's
     * own food check is only asked when this holds, so a player needs both. The client decides sprinting,
     * so this runs on the synced state of the local player.
     */
    public static boolean allowsSprinting(Player player) {
        if (!ThirstConfig.get().preventSprintingWhenThirsty) return true;
        ThirstData data = get(player);
        return !data.enabled() || data.thirst() > SPRINT_THIRST_THRESHOLD;
    }

    /**
     * Plain water follows vanilla food rules: it cannot be consumed while the visible bar is full.
     *
     * @deprecated asks about no stack, so it cannot allow the Clean and Pure top-up; use
     *     {@link #canDrinkWater(Player, ItemStack)}
     */
    @Deprecated
    public static boolean canDrinkWater(Player player) {
        ThirstData data = get(player);
        return !data.enabled() || player.getAbilities().invulnerable || data.thirst() < ThirstData.MAX;
    }

    /**
     * Whether {@code player} may drink the plain water in {@code stack}. Vanilla's food rule, nothing
     * while the bar is full, with one exception: Clean or Pure water may still be drunk at full thirst
     * while quenched is below full, so a player can build a reserve before a trip or after a heal spent
     * it. Dirty and Murky water, and sea water, follow the plain rule. Diverges from the original, where
     * a full bar refused all water.
     */
    public static boolean canDrinkWater(Player player, ItemStack stack) {
        ThirstData data = get(player);
        if (!data.enabled() || player.getAbilities().invulnerable || data.thirst() < ThirstData.MAX) return true;
        return data.quenched() < ThirstData.MAX && WaterPurity.isWaterContainer(stack)
                && WaterPurity.quality(stack) instanceof WaterQuality.Fresh fresh && fresh.purity() >= WaterPurity.BOILED;
    }

    public static void drinkItem(Player player, ItemStack stack) {
        int[] value = ThirstApi.thirstValues(stack);
        if (value == null) return;
        boolean quenches = WaterPurity.applyEffects(player, stack);
        int quenched = value[1];
        // Salt water is drunk without quenching anything, and still counts as having been drunk.
        if (WaterPurity.isWaterContainer(stack)) {
            WaterQuality quality = WaterPurity.quality(stack);
            ThirstAdvancements.drank(player, quality);
            quenched = WaterPurity.quenched(quality, quenched);
        }
        if (quenches) drinkThroughEvent(player, stack, value[0], quenched);
    }

    /**
     * Restores what a drink restores after {@link ThirstEvents#DRINK} has had its say. Every drink of an
     * item or of water by hand ends here; with no listener it is {@link #drink} and nothing else.
     */
    private static void drinkThroughEvent(Player player, ItemStack stack, int thirst, int quenched) {
        if (player.level().isClientSide()) return;
        if (ThirstEvents.DRINK.hasListeners()) {
            ThirstEvents.DrinkAmounts amounts = new ThirstEvents.DrinkAmounts(thirst, quenched);
            ThirstEvents.DRINK.invoker().onDrink(player, stack, amounts);
            if (amounts.isCancelled()) return;
            thirst = amounts.thirst();
            quenched = amounts.quenched();
        }
        drink(player, thirst, quenched);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player);
    }

    /**
     * One player's share of {@link #tick}. Public for the dev benchmark, which ticks simulated players
     * that are not in the player list.
     */
    public static void tickPlayer(ServerPlayer player) {
        ExhaustionTracker tracker = ExhaustionTracker.of(player);
        float mirrored = tracker.pending;
        float unsynced = tracker.unsynced;
        tracker.pending = 0.0F;
        tracker.unsynced = 0.0F;

        ThirstData data = get(player);
        if (!data.enabled() || player.getAbilities().invulnerable) return;

        UpsetStomach.tick(player);

        ThirstConfig config = ThirstConfig.get();
        Difficulty difficulty = player.level().getDifficulty();
        boolean peaceful = difficulty == Difficulty.PEACEFUL && !config.thirstDepletionInPeaceful;
        boolean slowTick = player.tickCount % SLOW_TICK_INTERVAL == 0;

        // The drain comes in three parts kept apart, because they scale differently. Activity is what
        // the player did, mirrored from vanilla's food exhaustion: the climate and fire relief scale it,
        // Nourishment cancels it, a listener sees it. The baseline is the minute's point of thirst every
        // survival player spends: climate and fire relief scale it, nothing cancels it and no listener
        // sees it, since it is nothing the player did. Illness is the effects' own drain: nothing scales
        // or cancels it, and a listener sees it with the activity.
        float activity = mirrored;
        // The Hunger effect already routes through causeFoodExhaustion; the original cancels that
        // contribution back out so poisoned food does not double as dehydration. Both sides are raw
        // amounts from the same tick, so they cancel exactly instead of leaving float noise behind that
        // would still cost a sync packet.
        MobEffectInstance hunger = player.getEffect(MobEffects.HUNGER);
        if (hunger != null) activity -= HUNGER_EXHAUSTION * (hunger.getAmplifier() + 1);
        // Nourishment stops the activity draining thirst the way it stops hunger, as in the original
        // mod. Everything the player did is dropped, including the negative amounts Farmer's Delight uses
        // to cancel food exhaustion from 1.21.11 on, so the Hunger refund above cannot turn into a refill
        // either. Unlike the original it leaves the baseline and illness alone.
        if (FarmersDelight.isNourished(player)) activity = 0.0F;

        float illness = player.hasEffect(MobEffects.NAUSEA) ? NAUSEA_EXHAUSTION : 0.0F;
        MobEffectInstance parched = Vanilla.getEffect(player, ThirstEffects.PARCHED);
        if (parched != null) illness += PARCHED_EXHAUSTION * (parched.getAmplifier() + 1);

        // Once per tick, on the activity and illness together, so a listener sees what the player did
        // and what ails them rather than each of the several vanilla charges a tick can hold. Its answer
        // is split back between the two in the same proportion. Nothing is built unless someone listens.
        float seen = activity + illness;
        if (seen != 0.0F && ThirstEvents.EXHAUSTION.hasListeners()) {
            float answer = ThirstEvents.EXHAUSTION.invoker().onExhaustion(player, seen);
            float scale = answer / seen;
            activity *= scale;
            illness *= scale;
        }

        // The baseline is not spent where thirst refills, on peaceful without depletion.
        float scaled = peaceful ? activity : activity + BASELINE_EXHAUSTION;
        // On peaceful, exhaustion never reaches thirst, so once quenched is empty it has nothing left to
        // spend and is dropped. Kept, it would sit below a point forever, a drain the refill can never
        // top up, and AppleSkin's exhaustion strip would stay stuck part way.
        boolean discards = peaceful && data.quenched() == 0;
        // What a heal from quenched costs is its own price, like vanilla's heal cost to food, so the
        // climate does not scale it and a listener does not see it as something the player did.
        float healCost = HealthRegen.healWithQuenched(player, data, tracker);
        float added = discards ? -data.exhaustion()
                : unsynced + healCost + illness + (scaled == 0.0F ? 0.0F : scaled * exhaustionModifier(player));
        boolean regenerates = peaceful && slowTick && data.thirst() < ThirstData.MAX;
        // The same clamp ThirstData#addExhaustion applies.
        float exhaustion = Math.max(0.0F, data.exhaustion() + added);

        int thirst = data.thirst();
        if (!regenerates && !discards && exhaustion <= ThirstData.EXHAUSTION_PER_POINT
                && sameSyncStep(data.exhaustion(), exhaustion)) {
            // Nothing the client would draw differently: carry it rather than build a record and send it.
            tracker.unsynced = exhaustion - data.exhaustion();
        } else {
            ThirstData updated = data.addExhaustion(added).consumeExhaustion(peaceful);
            if (peaceful && slowTick) updated = updated.regenerate(1);
            if (!updated.equals(data)) set(player, updated);
            thirst = updated.thirst();
        }

        if (thirst <= 0 && player.tickCount % DAMAGE_INTERVAL == 0) {
            float health = player.getHealth();
            if (health > 10.0F || difficulty == Difficulty.HARD
                    || (health > 0.0F && difficulty == Difficulty.NORMAL)) {
                ServerLevel level = Vanilla.level(player);
                Vanilla.hurt(player, ThirstDamageTypes.dehydration(level), 1.0F);
            }
        }
    }

    /** A sip of the water the player crouches at with an empty hand, one per click. Server only. */
    public static InteractionResult drinkByHand(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.PASS;
        BlockPos pos = handDrinkingWater(player, level, hand, hit);
        if (pos == null) return InteractionResult.PASS;

        WaterQuality quality = WaterPurity.sampleAt(level, pos);
        ItemStack sample = WaterPurity.setQuality(
                new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
        if (WaterPurity.applyEffects(player, sample)) {
            // No item was drunk, so listeners get an empty stack rather than the sample bowl above.
            drinkThroughEvent(player, ItemStack.EMPTY, HAND_DRINK_THIRST,
                    WaterPurity.quenched(quality, HAND_DRINK_QUENCHED));
        }
        ThirstAdvancements.drank(player, quality);
        // Player#playSound routes through Level#playSound with itself as the excluded listener, so a
        // server-side call is heard by everyone *except* the drinker. Vanilla gets away with it
        // because consumption effects also run client-side; hand drinking is server-only, so the
        // sound has to be broadcast with no exclusion. Volume and pitch match
        // LivingEntity#triggerItemUseEffects, i.e. the potion drinking sound.
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                Vanilla.drinkSound(), SoundSource.PLAYERS,
                0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    HAND_DRINK_SPLASHES, 0.2, 0.0, 0.2, 0.0);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** The water block a hand drink would take from, or {@code null} when this click is not one. */
    private static BlockPos handDrinkingWater(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ThirstConfig config = ThirstConfig.get();
        ThirstData data = get(player);
        if (!config.canDrinkByHand || !player.isCrouching() || player.getAbilities().invulnerable
                || !data.enabled() || data.thirst() >= ThirstData.MAX) {
            return null;
        }
        if (!player.getItemInHand(hand).isEmpty()) return null;
        // A click the client does not handle itself is sent once per hand, main hand first, so with
        // both hands empty the off hand's copy would be a second sip from the same click.
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().isEmpty()) return null;

        BlockPos pos = hit.getBlockPos();
        if (level.getFluidState(pos).is(FluidTags.WATER)) return pos;
        // The crosshair usually lands on the block beneath the surface when looking at water.
        pos = pos.relative(hit.getDirection());
        return level.getFluidState(pos).is(FluidTags.WATER) ? pos : null;
    }

    /** Whether two exhaustion values fall in the same {@link #SYNC_STEP}, so the client would draw them alike. */
    private static boolean sameSyncStep(float a, float b) {
        return Math.floor(a / SYNC_STEP) == Math.floor(b / SYNC_STEP);
    }

    /**
     * The exhaustion modifier, reused for {@link #MODIFIER_REFRESH_TICKS}. Reading armour protection
     * builds a loot context for every enchantment on every equipped item, and this runs on every tick a
     * player moves. Changing dimension or config recomputes it straight away.
     */
    private static float exhaustionModifier(Player player) {
        ExhaustionTracker tracker = ExhaustionTracker.of(player);
        Level level = player.level();
        int generation = ThirstConfig.generation();
        if (tracker.modifierLevel != level || tracker.modifierGeneration != generation
                || player.tickCount >= tracker.modifierExpiresAt) {
            tracker.modifier = computeExhaustionModifier(player);
            tracker.modifierLevel = level;
            tracker.modifierGeneration = generation;
            tracker.modifierExpiresAt = player.tickCount + MODIFIER_REFRESH_TICKS;
        }
        return tracker.modifier;
    }

    /**
     * Combined climate, fire-protection and fire-resistance multiplier applied to the activity and the
     * baseline, mirroring {@code ThirstHelper#getExhaustionBiomeModifier} and friends from the original
     * mod. {@code thirstDepletionModifier} multiplies the Nether's factor as much as the Overworld's,
     * which the original did not.
     */
    private static float computeExhaustionModifier(Player player) {
        ThirstConfig config = ThirstConfig.get();
        boolean scorching = Vanilla.waterEvaporates(player.level(), player.blockPosition());
        float modifier = (float) config.thirstDepletionModifier
                * (scorching ? SCORCHING_MODIFIER : climateModifier(player, config));

        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            modifier *= FIRE_RESISTANCE_MODIFIER;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            // getDamageProtection returns twice the enchantment level total, and the original scales
            // it by 0.0625 * 0.75 per level.
            float protection = Vanilla.damageProtection(serverPlayer, serverPlayer.damageSources().onFire());
            modifier *= Math.max(0.25F, 1.0F - protection * 0.0234375F);
        }
        return modifier;
    }

    /**
     * Replaces the biome's base temperature in the climate modifier, for an integration that measures
     * the player's surroundings. The source answers NaN for a player it cannot measure, who then drains
     * by the biome. Called once, at init.
     */
    public static void setClimateTemperature(ToDoubleFunction<Player> source) {
        climateTemperature = source;
    }

    /**
     * Sets the season's part in the climate modifier, for an integration that keeps a calendar. Called
     * once, at init.
     */
    public static void setSeasonalClimate(SeasonalClimate seasons) {
        seasonalClimate = seasons;
    }

    /**
     * The climate's factor on the drain, before {@code thirstDepletionModifier}: warmer and drier
     * drains faster, along one straight line held between {@link #CLIMATE_MIN} and {@link #CLIMATE_MAX}.
     * Diverges from the original's curve, which halved anything above a temperature of 1 and so drained
     * less in a slightly warmer biome; a season's factor follows, then the wider seasonal bounds.
     */
    private static float climateModifier(Player player, ThirstConfig config) {
        BlockPos pos = player.blockPosition();
        Holder<Biome> holder = player.level().getBiome(pos);
        Biome biome = holder.value();
        SeasonalClimate seasons = config.sereneSeasonsClimate ? seasonalClimate : null;

        // The original used Biome#getDownfall, which no longer exists; hasPrecipitation is the same
        // dry/wet split. Not in the original: a tropical biome's dry season counts as dry.
        boolean wet = biome.hasPrecipitation();
        if (seasons != null) wet = seasons.hasPrecipitation(player, holder, wet);

        float measured = measuredTemperature(player, config);
        float factor = climateFactor(Float.isNaN(measured) ? biome.getBaseTemperature() : measured, wet);

        // Not in the original. The season is a factor on the curve rather than a temperature. Cold
        // Sweat's world temperature already follows Serene Seasons, so a measured temperature takes no
        // second season.
        if (seasons != null && Float.isNaN(measured)) {
            factor = clamp(factor * seasons.drainMultiplier(player, holder), SEASONAL_MIN, SEASONAL_MAX);
        }
        return factor;
    }

    /**
     * The climate's factor for a biome {@code temperature}, in the biome's own units, with or without
     * rain. Public for the gametests, which check that it never falls as the temperature rises.
     */
    public static float climateFactor(float temperature, boolean wet) {
        return clamp(CLIMATE_BASE + CLIMATE_PER_DEGREE * (temperature - CLIMATE_TEMPERATE)
                + (wet ? 0.0F : CLIMATE_DRY), CLIMATE_MIN, CLIMATE_MAX);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * What an integration measured in place of the biome's base temperature, or NaN where none did. Not
     * in the original, which only knew the biome: a hearth in the tundra or a desert night now counts.
     */
    private static float measuredTemperature(Player player, ThirstConfig config) {
        ToDoubleFunction<Player> source = climateTemperature;
        if (source != null && config.coldSweatClimate) {
            double measured = source.applyAsDouble(player);
            if (Double.isFinite(measured)) return (float) measured;
        }
        return Float.NaN;
    }
}
