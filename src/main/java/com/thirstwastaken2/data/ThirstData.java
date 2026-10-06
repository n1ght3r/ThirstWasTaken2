package com.thirstwastaken2.data;

import com.thirstwastaken2.config.ThirstConfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.PlayerData;
import net.minecraft.network.FriendlyByteBuf;

public record ThirstData(int thirst, int quenched, float exhaustion, boolean enabled) {
    public static final int MAX = 20;
    /** Exhaustion that spends one point of quenched, or of thirst once quenched is empty. */
    public static final float EXHAUSTION_PER_POINT = 4.0F;

    public static final Codec<ThirstData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("thirst").forGetter(ThirstData::thirst),
            Codec.INT.fieldOf("quenched").forGetter(ThirstData::quenched),
            Codec.FLOAT.fieldOf("exhaustion").forGetter(ThirstData::exhaustion),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(ThirstData::enabled)
    ).apply(instance, ThirstData::new));

    /** Saved with the player and synced to that player's own client only. Read and write it through {@link ThirstManager}. */
    static final PlayerData<ThirstData> STORAGE = Loader.playerData(ThirstWasTaken2.id("player_data"), ThirstData::full,
            CODEC, ThirstData::write, ThirstData::read);

    /** The synced form, what {@link #read} takes back. Each loader wraps it in its own packet type. */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(thirst);
        buffer.writeVarInt(quenched);
        buffer.writeFloat(exhaustion);
        buffer.writeBoolean(enabled);
    }

    public static ThirstData read(FriendlyByteBuf buffer) {
        return new ThirstData(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readBoolean());
    }

    private static boolean registered;

    public static void register() {
        if (!registered) registered = true;
    }

    public static ThirstData full() {
        return new ThirstData(MAX, 5, 0.0F, true);
    }

    public ThirstData drink(int thirstAmount, int quenchedAmount) {
        // Thirst past a full bar is lost, from every source. It used to top quenched up instead, which
        // let a melon slice drunk at 19 build the reserve that only prepared water is meant to.
        int newThirst = Math.min(MAX, thirst + thirstAmount);
        int newQuenched = Math.min(newThirst, quenched + quenchedAmount);
        return new ThirstData(newThirst, newQuenched, exhaustion, enabled);
    }

    /** Passive thirst recovery used by the peaceful-difficulty branch. */
    public ThirstData regenerate(int amount) {
        int newThirst = Math.min(MAX, thirst + amount);
        return newThirst == thirst ? this : new ThirstData(newThirst, quenched, exhaustion, enabled);
    }

    public ThirstData addExhaustion(float amount) {
        if (amount == 0.0F) return this;
        return new ThirstData(thirst, quenched, Math.max(0.0F, exhaustion + amount), enabled);
    }

    public ThirstData consumeExhaustion(boolean peaceful) {
        if (!enabled || exhaustion <= EXHAUSTION_PER_POINT) return this;
        float nextExhaustion = exhaustion - EXHAUSTION_PER_POINT;
        if (quenched > 0) return new ThirstData(thirst, quenched - 1, nextExhaustion, true);
        return new ThirstData(peaceful ? thirst : Math.max(0, thirst - 1), 0, nextExhaustion, true);
    }

    public ThirstData withLevels(int thirst, int quenched) {
        int safeThirst = Math.max(0, Math.min(MAX, thirst));
        return new ThirstData(safeThirst, Math.max(0, Math.min(safeThirst, quenched)), exhaustion, enabled);
    }

    public ThirstData withExhaustion(float value) {
        return new ThirstData(thirst, quenched, Math.max(0.0F, value), enabled);
    }

    public ThirstData withEnabled(boolean value) {
        return new ThirstData(thirst, quenched, exhaustion, value);
    }
}
