package com.example.itemview.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.RegistryObject;

@OnlyIn(Dist.CLIENT)
public final class ScreenAudio {
    private static AmbientLoop loop;

    public static void play(RegistryObject<SoundEvent> ev, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev.get(), pitch, volume));
    }

    public static void ensureAmbient() {
        if (loop != null && !loop.isStopped()) {
            loop.revive();
            return;
        }
        loop = new AmbientLoop();
        Minecraft.getInstance().getSoundManager().play(loop);
    }

    private ScreenAudio() {}
}
