package com.example.itemview.client;

import com.example.itemview.ItemViewMod;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@OnlyIn(Dist.CLIENT)
public final class ClientInit {
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientInit::onModifyBaking);
    }

    /** Подменяем модель экспоната: он выглядит как тот предмет, который в него записан. */
    private static void onModifyBaking(ModelEvent.ModifyBakingResult e) {
        ModelResourceLocation key = new ModelResourceLocation(new ResourceLocation(ItemViewMod.MODID, "exhibit"), "inventory");
        BakedModel original = e.getModels().get(key);
        if (original != null) e.getModels().put(key, new ExhibitModel(original));
    }

    private ClientInit() {}
}
