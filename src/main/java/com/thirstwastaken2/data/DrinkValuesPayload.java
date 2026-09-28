package com.thirstwastaken2.data;

import com.thirstwastaken2.ThirstWasTaken2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Every data pack thirst value the server loaded, sent to a client on join and after {@code /reload}
 * so its tooltips agree with what drinking restores. See {@link DataPackDrinks}.
 *
 * <p>Items travel as raw registry ids, so the packet is a few bytes per entry. A client without the mod
 * never receives it: each loader checks that the channel was negotiated before sending.
 */
public record DrinkValuesPayload(Map<Item, int[]> values) {
    public static final Identifier ID = ThirstWasTaken2.id("drink_values");

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(values.size());
        values.forEach((item, amounts) -> {
            buffer.writeVarInt(BuiltInRegistries.ITEM.getId(item));
            buffer.writeVarInt(amounts[0]);
            buffer.writeVarInt(amounts[1]);
        });
    }

    public static DrinkValuesPayload read(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        // Sized no larger than vanilla's own map codec would, whatever count the packet claims.
        Map<Item, int[]> values = new IdentityHashMap<>(Math.min(size, 65536));
        for (int i = 0; i < size; i++) {
            Item item = BuiltInRegistries.ITEM.byId(buffer.readVarInt());
            values.put(item, new int[]{buffer.readVarInt(), buffer.readVarInt()});
        }
        return new DrinkValuesPayload(values);
    }
}
