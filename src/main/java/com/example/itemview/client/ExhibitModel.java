package com.example.itemview.client;

import com.example.itemview.ExhibitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.Nullable;

/** Если в экспонате записан предмет — в инвентаре, руке и на земле рисуется модель ЭТОГО предмета. */
@OnlyIn(Dist.CLIENT)
public class ExhibitModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides = new ItemOverrides() {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            ItemStack shown = ExhibitItem.getShown(stack);
            if (shown.isEmpty()) return model;
            return Minecraft.getInstance().getItemRenderer().getModel(shown, level, entity, seed);
        }
    };

    public ExhibitModel(BakedModel original) {
        super(original);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
