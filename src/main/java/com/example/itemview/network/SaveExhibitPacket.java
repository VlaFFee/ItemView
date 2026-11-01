package com.example.itemview.network;

import com.example.itemview.ExhibitItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: записать данные в экспонат. Принимается ТОЛЬКО от игрока в креативе. */
public record SaveExhibitPacket(byte hand, String title, String desc, ItemStack shown) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(hand);
        buf.writeUtf(title, 128);
        buf.writeUtf(desc, 8000);
        buf.writeItem(shown);
    }

    public static SaveExhibitPacket decode(FriendlyByteBuf buf) {
        return new SaveExhibitPacket(buf.readByte(), buf.readUtf(128), buf.readUtf(8000), buf.readItem());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player == null || !player.isCreative()) return; // правка только в креативе

            InteractionHand h = hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(h);
            if (!(stack.getItem() instanceof ExhibitItem)) return;

            CompoundTag t = stack.getOrCreateTag();
            t.putString(ExhibitItem.TAG_TITLE, title);
            t.putString(ExhibitItem.TAG_DESC, desc);

            if (shown.isEmpty() || shown.getItem() instanceof ExhibitItem) {
                t.remove(ExhibitItem.TAG_SHOWN);
            } else {
                ItemStack copy = shown.copy();
                copy.setCount(1);
                CompoundTag saved = copy.save(new CompoundTag());
                if (saved.toString().length() > 30000) return; // защита от гигантского NBT
                t.put(ExhibitItem.TAG_SHOWN, saved);
            }
        });
        c.setPacketHandled(true);
    }
}
