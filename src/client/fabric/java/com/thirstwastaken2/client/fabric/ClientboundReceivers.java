package com.thirstwastaken2.client.fabric;

import com.thirstwastaken2.fabric.ClientboundPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Registers the client handler of every payload common code declared through
 * {@code Loader.clientboundPayload}. Registering a receiver is also what tells a server this client
 * can take the payload, so the {@code Clientbound} it returned reaches it.
 */
final class ClientboundReceivers {
    private ClientboundReceivers() { }

    static void register() {
        ClientboundPayloads.all().forEach(ClientboundReceivers::register);
    }

    private static <T extends CustomPacketPayload> void register(ClientboundPayloads.Registration<T> registration) {
        // Fabric API calls a play receiver on the client thread.
        ClientPlayNetworking.registerGlobalReceiver(registration.type(),
                (payload, context) -> registration.handler().accept(payload));
    }
}
