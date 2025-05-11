package io.github.apace100.calio.network;

import io.github.apace100.calio.registry.DataObjectRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class CalioNetworkingClient {

    public static void registerReceivers() {
        ClientPlayConnectionEvents.INIT.register(((clientPlayNetworkHandler, minecraftClient) -> {
            ClientPlayNetworking.registerReceiver(
                CalioNetworking.SYNC_DATA_OBJECT_REGISTRY,
                CalioNetworkingClient::onDataObjectRegistrySync
            );
        }));
    }

    private static void onDataObjectRegistrySync(
        Minecraft minecraftClient,
        ClientPacketListener clientPlayNetworkHandler,
        FriendlyByteBuf packetByteBuf,
        PacketSender packetSender) {
        ResourceLocation registryId = packetByteBuf.readResourceLocation();
        DataObjectRegistry.getRegistry(registryId).receive(packetByteBuf,
            minecraftClient.hasSingleplayerServer() ? r -> {} : minecraftClient::execute);
        /*minecraftClient.execute(() -> {
            DataObjectRegistry.getRegistry(registryId).receive(packetByteBuf);
        });*/
    }
}
