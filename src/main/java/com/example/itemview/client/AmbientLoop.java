package com.example.itemview.client;

import com.example.itemview.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Тихая фоновая петля: плавно нарастает при открытии экрана и затухает при закрытии. */
@OnlyIn(Dist.CLIENT)
public class AmbientLoop extends AbstractTickableSoundInstance {
    private static final float MAX = 0.32f;
    private float target = MAX;

    public AmbientLoop() {
        super(ModSounds.AMBIENT.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void revive() {
        target = MAX;
    }

    @Override
    public void tick() {
        Screen s = Minecraft.getInstance().screen;
        boolean ours = s instanceof InspectScreen || s instanceof ItemPickerScreen;
        float t = ours ? target : 0f;
        volume += (t - volume) * 0.07f;
        if (t == 0f && volume < 0.01f) stop();
    }
}
