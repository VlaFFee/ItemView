package com.example.itemview;

import com.example.itemview.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Предмет-«экспонат». В NBT хранит название, описание и ПОЛНЫЙ стек предмета, который показывается в 3D.
 * Выглядит в инвентаре/руке/на земле как тот предмет, который в него записан (см. client/ExhibitModel).
 */
public class ExhibitItem extends Item {
    public static final String TAG_TITLE = "Title";
    public static final String TAG_DESC = "Desc";
    public static final String TAG_SHOWN = "Shown";

    public ExhibitItem(Properties props) {
        super(props);
    }

    /** Предмет, записанный в экспонат (копия, count = 1). EMPTY, если ничего не выбрано. */
    public static ItemStack getShown(ItemStack stack) {
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains(TAG_SHOWN, 10)) return ItemStack.EMPTY;
        ItemStack s = ItemStack.of(t.getCompound(TAG_SHOWN));
        if (s.isEmpty() || s.getItem() instanceof ExhibitItem) return ItemStack.EMPTY;
        return s;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openInspect(hand, stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag t = stack.getTag();
        if (t != null && !t.getString(TAG_TITLE).isEmpty()) {
            return Component.literal(t.getString(TAG_TITLE));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.itemview.open").withStyle(ChatFormatting.GRAY));
    }
}
