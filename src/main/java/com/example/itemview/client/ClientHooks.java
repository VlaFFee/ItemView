package com.example.itemview.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientHooks {
    public static void openInspect(InteractionHand hand, ItemStack stack) {
        Minecraft.getInstance().setScreen(new InspectScreen(hand, stack));
    }

    private ClientHooks() {}
}
