package com.thirstwastaken2.datagen;

// Villager trades became data in 26.1. Before it they are code, added by compat/TradeIntegration through
// Loader.addVillagerTrade, so there is nothing for this provider to write and on those versions the file
// holds no class at all. Comments inside the block stay line comments: a disabled branch is itself one
// block comment.
//? if >=26.1 {
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.compat.TradeIntegration;
import com.thirstwastaken2.compat.TradeIntegration.Trade;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

// The trades in TradeIntegration.TRADES, as data: one data/thirstwastaken2/villager_trade/ file each, and
// an entry in vanilla's tag for its profession and level, data/minecraft/tags/villager_trade/, which is
// the pool the level's trade set draws from.
//
// The JSON is assembled here rather than encoded with VillagerTrade's codec, whose constructors changed in
// 26.3; only the item is encoded, with ItemStackTemplate's codec, so the components come out in each
// version's format. Constant numbers read the same on every 26 version. A trade for one of the mod's own
// items carries the item_enabled condition, as its recipes do, and its tag entry is optional so that a
// switched-off item takes only its own trade out of the pool.
public final class ThirstVillagerTradeProvider implements DataProvider {
    private final PackOutput.PathProvider trades;
    private final PackOutput.PathProvider tags;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ThirstVillagerTradeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.trades = output.createPathProvider(PackOutput.Target.DATA_PACK, "villager_trade");
        this.tags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/villager_trade");
        this.registries = registries;
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Villager Trades";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(lookup -> {
            DynamicOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);
            List<CompletableFuture<?>> writes = new ArrayList<>();
            Map<Identifier, JsonArray> pools = new TreeMap<>();
            for (Trade trade : TradeIntegration.TRADES) {
                writes.add(DataProvider.saveStable(cache, trade(trade, ops), trades.json(trade.id())));
                JsonObject entry = new JsonObject();
                entry.addProperty("id", trade.id().toString());
                entry.addProperty("required", false);
                pools.computeIfAbsent(pool(trade), id -> new JsonArray()).add(entry);
            }
            pools.forEach((pool, values) -> {
                JsonObject tag = new JsonObject();
                tag.add("values", values);
                writes.add(DataProvider.saveStable(cache, tag, tags.json(pool)));
            });
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    private static JsonObject trade(Trade trade, DynamicOps<JsonElement> ops) {
        Item item = trade.item().get();
        JsonObject json = new JsonObject();
        if (Vanilla.itemId(item).getNamespace().equals(ThirstWasTaken2.MOD_ID)) {
            json.add(ResourceConditions.CONDITIONS_KEY, ResourceCondition.LIST_CODEC
                    .encodeStart(ops, List.of(ThirstRecipeProvider.itemEnabled(item))).getOrThrow());
        }
        json.add("gives", ItemStackTemplate.CODEC.encodeStart(ops, gives(trade, item)).getOrThrow());
        json.addProperty("max_uses", trade.maxUses());
        json.addProperty("reputation_discount", TradeIntegration.PRICE_MULTIPLIER);
        JsonObject wants = new JsonObject();
        if (trade.emeralds() != 1) wants.addProperty("count", trade.emeralds());
        wants.addProperty("id", Vanilla.itemId(Items.EMERALD).toString());
        json.add("wants", wants);
        json.addProperty("xp", trade.xp());
        return json;
    }

    // What the trade gives, built the way TradeIntegration.Trade.gives builds the stack. A water bottle is
    // the purification recipes' bottle result, which is the same plain water stamped fresh.
    private static ItemStackTemplate gives(Trade trade, Item item) {
        if (trade.grade() == TradeIntegration.NO_WATER) return new ItemStackTemplate(item);
        if (item != Items.POTION) throw new IllegalStateException(trade.id() + ": only a water bottle can carry a grade");
        return ThirstRecipeProvider.Recipes.purifyResult(ThirstRecipeProvider.Container.BOTTLE, trade.grade());
    }

    // Vanilla's tag for a profession's level: minecraft:leatherworker/level_1 and so on.
    private static Identifier pool(Trade trade) {
        return Identifier.fromNamespaceAndPath(trade.profession().getNamespace(),
                trade.profession().getPath() + "/level_" + trade.level());
    }
}
//?}
