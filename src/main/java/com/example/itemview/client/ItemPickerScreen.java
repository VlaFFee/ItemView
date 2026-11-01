package com.example.itemview.client;

import com.example.itemview.ExhibitItem;
import com.example.itemview.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Сетка всех предметов игры с поиском по названию или id. Предметы с особым NBT берите кнопкой «Из руки». */
@OnlyIn(Dist.CLIENT)
public class ItemPickerScreen extends Screen {
    private static final int CELL = 24, GAP = 2;
    private static final int BG = 0xFF09070B;

    private record Entry(ItemStack stack, String name, String key) {}

    private final Screen parent;
    private final Consumer<ItemStack> onPick;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> view = List.of();
    private EditBox search;
    private float scroll, maxScroll;
    private int areaX, areaY, areaW, areaH, cols, gridX;

    public ItemPickerScreen(Screen parent, Consumer<ItemStack> onPick) {
        super(Component.translatable("gui.itemview.picker_title"));
        this.parent = parent;
        this.onPick = onPick;
        for (Item item : ForgeRegistries.ITEMS) {
            if (item == Items.AIR || item instanceof ExhibitItem) continue;
            ItemStack st = new ItemStack(item);
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            String idStr = id == null ? "" : id.toString();
            String nm = st.getHoverName().getString();
            all.add(new Entry(st, nm, (nm + " " + idStr).toLowerCase(Locale.ROOT)));
        }
        all.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
        view = all;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        areaX = 24;
        areaY = 56;
        areaW = width - 48;
        areaH = height - areaY - 44;
        cols = Math.max(1, (areaW + GAP) / (CELL + GAP));
        gridX = areaX + (areaW - (cols * (CELL + GAP) - GAP)) / 2;

        String prev = search == null ? "" : search.getValue();
        search = new EditBox(font, width / 2 - 110, 30, 220, 18, Component.empty());
        search.setMaxLength(60);
        search.setHint(Component.translatable("gui.itemview.picker_search"));
        search.setResponder(this::filter);
        search.setValue(prev);
        addRenderableWidget(search);
        setInitialFocus(search);

        addRenderableWidget(Button.builder(Component.translatable("gui.itemview.back"), b -> onClose())
                .bounds(width / 2 - 60, height - 30, 120, 20).build());
    }

    private void filter(String q) {
        String s = q.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            view = all;
        } else {
            List<Entry> r = new ArrayList<>();
            for (Entry e : all) if (e.key().contains(s)) r.add(e);
            view = r;
        }
        scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, BG);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFB69CFF);

        int rows = (view.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (CELL + GAP) - GAP - areaH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (view.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.itemview.picker_empty"),
                    width / 2, height / 2 - 4, 0xFF8E8798);
        }

        Entry hover = null;
        g.enableScissor(areaX, areaY, areaX + areaW, areaY + areaH);
        for (int i = 0; i < view.size(); i++) {
            int x = gridX + (i % cols) * (CELL + GAP);
            int y = areaY + (i / cols) * (CELL + GAP) - (int) scroll;
            if (y + CELL < areaY || y > areaY + areaH) continue;

            boolean over = mx >= x && mx < x + CELL && my >= Math.max(y, areaY) && my < Math.min(y + CELL, areaY + areaH);
            g.fill(x, y, x + CELL, y + CELL, over ? 0xFF2A2433 : 0xFF171320);
            if (over) {
                g.renderOutline(x, y, CELL, CELL, 0xFFB69CFF);
                hover = view.get(i);
            }
            g.renderItem(view.get(i).stack(), x + (CELL - 16) / 2, y + (CELL - 16) / 2);
        }
        g.disableScissor();
        super.render(g, mx, my, pt);
        if (hover != null) g.renderTooltip(font, hover.stack(), mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || mx < areaX || mx >= areaX + areaW || my < areaY || my >= areaY + areaH) return false;
        double rx = mx - gridX, ry = my - areaY + scroll;
        if (rx < 0 || ry < 0) return false;
        int col = (int) (rx / (CELL + GAP)), row = (int) (ry / (CELL + GAP));
        if (rx % (CELL + GAP) >= CELL || ry % (CELL + GAP) >= CELL || col >= cols) return false;
        int idx = row * cols + col;
        if (idx >= 0 && idx < view.size()) {
            ScreenAudio.play(ModSounds.PICK, 1f, 0.8f);
            onPick.accept(view.get(idx).stack().copy());
            minecraft.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = (float) Math.max(0, Math.min(maxScroll, scroll - delta * 30));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
