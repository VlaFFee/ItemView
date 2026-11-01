package com.example.itemview;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ItemViewMod.MODID);

    public static final RegistryObject<SoundEvent> OPEN = reg("note_open");
    public static final RegistryObject<SoundEvent> CLOSE = reg("note_close");
    public static final RegistryObject<SoundEvent> DROP = reg("photo_open");
    public static final RegistryObject<SoundEvent> SCRIBBLE = reg("scribble");
    public static final RegistryObject<SoundEvent> PICK = reg("pick");
    public static final RegistryObject<SoundEvent> AMBIENT = reg("ambient");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ItemViewMod.MODID, name)));
    }

    private ModSounds() {}
}
