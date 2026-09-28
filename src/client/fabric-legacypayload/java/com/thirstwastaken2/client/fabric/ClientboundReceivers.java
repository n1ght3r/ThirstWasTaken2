package com.thirstwastaken2.client.fabric;

import com.thirstwastaken2.platform.FabricNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Registers the client handler of every channel common code declared through {@code Loader.playerData}
 * and {@code Loader.clientboundPayload}, on Minecraft 1.20.1. Registering a receiver is also what tells
 * a server this client takes the channel. See {@link FabricNetworking}.
 */
final class ClientboundReceivers {
    private ClientboundReceivers() { }

    static void register() {
        FabricNetworking.receivers().forEach(ClientboundReceivers::register);
    }

    private static <T> void register(FabricNetworking.Receiver<T> receiver) {
        // Before 1.20.5 Fabric API calls a play receiver on the network thread: the buffer is read there,
        // and what it holds is handed over on the client thread.
        ClientPlayNetworking.registerGlobalReceiver(receiver.id(), (client, handler, buffer, sender) -> {
            T value = receiver.read().apply(buffer);
            client.execute(() -> {
                if (client.player != null) receiver.handler().accept(client.player, value);
            });
        });
    }
}
