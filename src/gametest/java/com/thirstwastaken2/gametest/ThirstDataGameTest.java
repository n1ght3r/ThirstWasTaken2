package com.thirstwastaken2.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * The player's thirst record: the arithmetic every drink and every tick is built from, and the two
 * codecs that save it with the player and send it to the client.
 *
 * <p>None of this needs a world, but the HUD and the tooltips both assume these rules, and a record
 * that loads wrongly from an old save is the kind of bug nobody notices until a player's bar resets.
 */
public final class ThirstDataGameTest {
    @GameTest
    public void aNewPlayerStartsWithAFullBar(GameTestHelper helper) {
        ThirstData data = ThirstManager.get(TestFixtures.mockPlayer(helper));

        TestFixtures.check(helper, data.equals(ThirstData.full()),
                "a player the mod has never seen should start full, got " + data);
        TestFixtures.check(helper, data.thirst() == ThirstData.MAX && data.enabled(),
                "full means a full, enabled bar, got " + data);
        helper.succeed();
    }

    @GameTest
    public void drinkingFillsBothBarsAndQuenchedNeverPassesThirst(GameTestHelper helper) {
        ThirstData drunk = new ThirstData(10, 2, 0.0F, true).drink(4, 3);
        levels(helper, drunk, 14, 5, "10/2 plus a 4/3 drink");

        ThirstData capped = new ThirstData(4, 0, 0.0F, true).drink(2, 10);
        levels(helper, capped, 6, 6, "quenched is capped at thirst, so 4/0 plus a 2/10 drink");
        helper.succeed();
    }

    @GameTest
    public void extraThirstIsLost(GameTestHelper helper) {
        levels(helper, new ThirstData(18, 0, 0.0F, true).drink(6, 0), ThirstData.MAX, 0,
                "thirst past a full bar is lost rather than turned into quenched, so 18/0 plus 6/0");
        helper.succeed();
    }

    @GameTest
    public void exhaustionSpendsQuenchedBeforeThirst(GameTestHelper helper) {
        ThirstData quenched = new ThirstData(10, 3, 5.0F, true).consumeExhaustion(false);
        levels(helper, quenched, 10, 2, "a spent point comes out of quenched first");
        TestFixtures.check(helper, quenched.exhaustion() == 1.0F,
                "spending a point takes 4 exhaustion off 5, got " + quenched.exhaustion());

        ThirstData dry = new ThirstData(10, 0, 5.0F, true).consumeExhaustion(false);
        levels(helper, dry, 9, 0, "with quenched empty the point comes out of thirst");

        ThirstData peaceful = new ThirstData(10, 0, 5.0F, true).consumeExhaustion(true);
        levels(helper, peaceful, 10, 0, "on peaceful the exhaustion is spent without costing thirst");
        TestFixtures.check(helper, peaceful.exhaustion() == 1.0F,
                "peaceful still spends the exhaustion itself, got " + peaceful.exhaustion());
        helper.succeed();
    }

    @GameTest
    public void nothingIsSpentBelowAWholePointOrWhileDisabled(GameTestHelper helper) {
        ThirstData atAPoint = new ThirstData(10, 0, ThirstData.EXHAUSTION_PER_POINT, true);
        TestFixtures.check(helper, atAPoint.consumeExhaustion(false) == atAPoint,
                "exactly one point's worth of exhaustion is not past it yet, got "
                        + atAPoint.consumeExhaustion(false));

        ThirstData disabled = new ThirstData(10, 0, 9.0F, false);
        TestFixtures.check(helper, disabled.consumeExhaustion(false) == disabled,
                "a player with thirst disabled spends nothing, got " + disabled.consumeExhaustion(false));
        helper.succeed();
    }

    @GameTest
    public void levelsStayOnTheBar(GameTestHelper helper) {
        ThirstData base = new ThirstData(10, 0, 0.0F, true);
        levels(helper, base.withLevels(99, 99), ThirstData.MAX, ThirstData.MAX, "levels above the bar");
        levels(helper, base.withLevels(-5, 3), 0, 0, "levels below the bar, quenched capped by thirst");
        levels(helper, base.withLevels(5, 9), 5, 5, "quenched above thirst");

        TestFixtures.check(helper, base.addExhaustion(-10.0F).exhaustion() == 0.0F,
                "exhaustion never goes negative, got " + base.addExhaustion(-10.0F));
        ThirstData full = ThirstData.full();
        TestFixtures.check(helper, full.regenerate(1) == full,
                "regenerating a full bar should change nothing, so that nothing is written or synced");
        TestFixtures.check(helper, new ThirstData(19, 0, 0.0F, true).regenerate(5).thirst() == ThirstData.MAX,
                "regeneration stops at a full bar");
        helper.succeed();
    }

    @GameTest
    public void theSavedFormRoundTrips(GameTestHelper helper) {
        ThirstData data = new ThirstData(7, 3, 1.5F, false);
        JsonElement saved = ThirstData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        ThirstData loaded = ThirstData.CODEC.parse(JsonOps.INSTANCE, saved).result().orElse(null);
        TestFixtures.check(helper, data.equals(loaded),
                "saving and loading should give " + data + " back, got " + loaded);

        // A player saved before thirst could be switched off has no `enabled` field, and is enabled.
        JsonObject old = new JsonObject();
        old.addProperty("thirst", 5);
        old.addProperty("quenched", 1);
        old.addProperty("exhaustion", 0.5F);
        ThirstData upgraded = ThirstData.CODEC.parse(JsonOps.INSTANCE, old).result().orElse(null);
        TestFixtures.check(helper, upgraded != null && upgraded.enabled() && upgraded.thirst() == 5,
                "a save without `enabled` should load as enabled, got " + upgraded);
        helper.succeed();
    }

    /**
     * The whole save path, not just the codec: thirst set on a player, written into that player's own
     * tag and read back out of it. What the codec test above cannot see is the attachment underneath
     * it — the key the value is stored under, and the {@code value} field NeoForge wraps it in after
     * 1.21.1 — which is what quitting to the title and rejoining actually exercises.
     */
    @GameTest
    public void thirstSurvivesSavingAndLoadingThePlayer(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        // Switched off as well, so no field of the record can come back right by accident.
        ThirstData saved = new ThirstData(11, 4, 1.25F, false);
        ThirstManager.set(player, saved);

        CompoundTag tag = TestFixtures.savePlayer(player);
        TestFixtures.check(helper, !tag.isEmpty(), "saving a player should write a tag");

        // Full again, so a value that came from anywhere but the tag cannot pass for a load.
        ThirstManager.set(player, ThirstData.full());
        TestFixtures.loadPlayer(player, tag);

        ThirstData loaded = ThirstManager.get(player);
        TestFixtures.check(helper, saved.equals(loaded),
                "a player saved at " + saved + " should load back at it, got " + loaded);
        helper.succeed();
    }

    /** A player attachment saved by the other loader is deliberately not mistaken for this one. */
    @GameTest
    public void aSaveFromTheOtherLoaderStartsWithAFullBar(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        ThirstManager.set(player, new ThirstData(8, 3, 1.25F, false));
        CompoundTag tag = TestFixtures.savePlayer(player);

        // Forge 47 keeps it among the player's capabilities, under ForgeCaps.
        String nativeKey = tag.contains("fabric:attachments") ? "fabric:attachments"
                : tag.contains("ForgeCaps") ? "ForgeCaps" : "neoforge:attachments";
        String foreignKey = nativeKey.equals("fabric:attachments")
                ? "neoforge:attachments" : "fabric:attachments";
        Tag attachments = tag.get(nativeKey);
        TestFixtures.check(helper, attachments != null,
                "the player save should contain " + nativeKey + ", got " + tag);
        tag.remove(nativeKey);
        tag.put(foreignKey, attachments.copy());

        ThirstManager.set(player, ThirstData.full());
        TestFixtures.loadPlayer(player, tag);
        TestFixtures.check(helper, ThirstManager.get(player).equals(ThirstData.full()),
                "an attachment under the other loader's key should be ignored");
        helper.succeed();
    }

    /** NeoForge 21.1 and later intentionally use incompatible attachment wrapper shapes. */
    @GameTest
    public void aSaveFromTheOtherNeoForgeGenerationStartsWithAFullBar(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        ThirstManager.set(player, new ThirstData(8, 3, 1.25F, false));
        CompoundTag tag = TestFixtures.savePlayer(player);
        if (!tag.contains("neoforge:attachments")) {
            helper.succeed();
            return;
        }

        Tag attachmentTag = tag.get("neoforge:attachments");
        TestFixtures.check(helper, attachmentTag instanceof CompoundTag,
                "NeoForge attachments should be a compound, got " + attachmentTag);
        CompoundTag attachments = (CompoundTag) attachmentTag;
        Tag dataTag = attachments.get("thirstwastaken2:player_data");
        TestFixtures.check(helper, dataTag instanceof CompoundTag,
                "the thirst attachment should be a compound, got " + dataTag);
        CompoundTag data = (CompoundTag) dataTag;

        // 21.1 stores the record directly; every later generation stores it under `value`.
        CompoundTag incompatible;
        if (data.contains("value")) {
            Tag unwrapped = data.get("value");
            TestFixtures.check(helper, unwrapped instanceof CompoundTag,
                    "the wrapped thirst value should be a compound, got " + unwrapped);
            incompatible = ((CompoundTag) unwrapped).copy();
        } else {
            incompatible = new CompoundTag();
            incompatible.put("value", data.copy());
        }
        attachments.put("thirstwastaken2:player_data", incompatible);

        ThirstManager.set(player, ThirstData.full());
        TestFixtures.loadPlayer(player, tag);
        TestFixtures.check(helper, ThirstManager.get(player).equals(ThirstData.full()),
                "an attachment from the other NeoForge generation should be ignored");
        helper.succeed();
    }

    @GameTest
    public void theSyncedFormRoundTrips(GameTestHelper helper) {
        ThirstData data = new ThirstData(13, 4, 2.25F, false);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            data.write(buffer);
            ThirstData received = ThirstData.read(buffer);
            TestFixtures.check(helper, data.equals(received),
                    "the client should receive " + data + ", got " + received);
            TestFixtures.check(helper, buffer.readableBytes() == 0,
                    "decoding should read the whole packet, " + buffer.readableBytes() + " bytes were left");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void levels(GameTestHelper helper, ThirstData data, int thirst, int quenched, String what) {
        TestFixtures.check(helper, data.thirst() == thirst && data.quenched() == quenched,
                what + " should be " + thirst + "/" + quenched + ", got " + data);
    }
}
