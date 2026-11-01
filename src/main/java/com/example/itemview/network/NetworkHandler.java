package com.example.itemview.network;

import com.example.itemview.ItemViewMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ItemViewMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, SaveExhibitPacket.class,
                SaveExhibitPacket::encode, SaveExhibitPacket::decode, SaveExhibitPacket::handle);
    }

    private NetworkHandler() {}
}
