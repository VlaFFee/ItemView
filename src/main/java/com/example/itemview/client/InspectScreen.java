package com.example.itemview.client;

import com.example.itemview.ExhibitItem;
import com.example.itemview.ModSounds;
import com.example.itemview.network.NetworkHandler;
import com.example.itemview.network.SaveExhibitPacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Экран «Осмотр предмета»: слева 3D-предмет (ЛКМ — вращать, колесо — масштаб, R — сброс),
 * справа панель с названием и описанием. В креативе есть режим редактирования.
 */
@OnlyIn(Dist.CLIENT)
public class InspectScreen extends Screen {
    private static final int BG = 0x09070B;
    private static final int PURPLE = 0xA070FF;
    private static final int PANEL = 0x1B1726;
    private static final int EDGE = 0x4F4962;
    private static final int INSET = 0x120F1A;
    private static final int INSET_EDGE = 0x3A3548;
    private static final float TITLE_SCALE = 2.2f;
    private static final float BODY_SCALE = 1.1f;
    private static final float BODY_LINE = 12f * BODY_SCALE;

    private final InteractionHand hand;
    private final boolean creative;
    private final ScreenAnim anim = new ScreenAnim();
    private final List<AbstractWidget> mine = new ArrayList<>();

    private String name, desc;              // текущие значения
    private ItemStack shown;
    private String sName, sDesc;            // последние сохранённые
    private ItemStack sShown;
    private boolean editing;

    // вид предмета
    private float yaw, pitch, zoom = 1f, zoomTarget = 1f;
    private boolean dragging;

    // прокрутка описания
    private float scroll, scrollTarget, maxScroll;
    private long lastFrame = Util.getMillis();

    // раскладка правой панели
    private int px, py, pw, ph, pad;
    private int titleY, divY, labelY, boxY, boxH;
    private List<FormattedCharSequence> titleLines = List.of();
    private List<FormattedCharSequence> descLines = List.of();

    private EditBox nameBox;
    private MultiLineEditBox descBox;
    private String notice = "";
    private long noticeUntil;

    public InspectScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("gui.itemview.title"));
        this.hand = hand;
        CompoundTag t = stack.getTag();
        sName = t != null ? t.getString(ExhibitItem.TAG_TITLE) : "";
        sDesc = t != null ? t.getString(ExhibitItem.TAG_DESC) : "";
        sShown = ExhibitItem.getShown(stack);
        var p = Minecraft.getInstance().player;
        creative = p != null && p.isCreative();
        revert();
        editing = creative && sName.isEmpty() && sDesc.isEmpty() && sShown.isEmpty();
        ScreenAudio.play(ModSounds.OPEN, 0.9f, 1f);
        ScreenAudio.ensureAmbient();
    }

    private void revert() {
        name = sName;
        desc = sDesc;
        shown = sShown;
    }

    private void sync() {
        if (editing && nameBox != null && descBox != null) {
            name = nameBox.getValue();
            desc = descBox.getValue();
        }
    }

    private <T extends AbstractWidget> T add(T w) {
        mine.add(w);
        return addRenderableWidget(w);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        sync();
        super.resize(mc, w, h);
    }

    // ------------------------------------------------------------------ раскладка и виджеты

    private void layout() {
        pw = (int) (width * 0.337f);
        px = (int) (width * 0.64f);
        if (px + pw > width - 8) px = width - 8 - pw;
        pad = Math.max(10, (int) (width * 0.012f));

        if (editing) {
            py = (int) (height * 0.12f);
            ph = (int) (height * 0.76f);
            return;
        }

        int innerW = pw - pad * 2;
        titleLines = font.split(Component.literal(name.isEmpty() ? " " : name), (int) (innerW / TITLE_SCALE));
        int titleH = (int) (Math.max(1, titleLines.size()) * 10 * TITLE_SCALE);
        descLines = font.split(Component.literal(desc), (int) ((innerW - 16) / BODY_SCALE));

        int contentH = (int) (descLines.size() * BODY_LINE) + 16;
        int fixed = 61 + titleH;                                   // всё, кроме поля текста
        int maxBox = (int) (height * 0.68f) - fixed;
        boxH = Math.max(40, Math.min(contentH, maxBox));
        ph = fixed + boxH;
        py = (height - ph) / 2;

        titleY = py + 14;
        divY = titleY + titleH + 7;
        labelY = divY + 12;
        boxY = labelY + 14;
        maxScroll = Math.max(0, descLines.size() * BODY_LINE - (boxH - 16));
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    @Override
    protected void init() {
        mine.clear();
        layout();
        int ix = px + pad, iw = pw - pad * 2;

        if (editing) {
            nameBox = new EditBox(font, ix, py + 26, iw, 20, Component.empty());
            nameBox.setMaxLength(100);
            nameBox.setValue(name);
            nameBox.setHint(Component.translatable("gui.itemview.title_hint"));
            add(nameBox);

            int gap = 4, bh = 20;
            int row2 = py + ph - 12 - bh;
            int row1 = row2 - gap - bh;
            int descTop = py + 66;
            descBox = new MultiLineEditBox(font, ix, descTop, iw, Math.max(40, row1 - 8 - descTop),
                    Component.translatable("gui.itemview.desc_hint"), Component.empty());
            descBox.setValue(desc);
            add(descBox);

            int bw = (iw - gap) / 2;
            add(Button.builder(Component.translatable("gui.itemview.from_hand"), b -> fromHand())
                    .bounds(ix, row1, bw, bh).build());
            add(Button.builder(Component.translatable("gui.itemview.pick"), b -> {
                sync();
                minecraft.setScreen(new ItemPickerScreen(this, s -> shown = s));
            }).bounds(ix + bw + gap, row1, bw, bh).build());
            add(Button.builder(Component.translatable("gui.itemview.save"), b -> save())
                    .bounds(ix, row2, bw, bh).build());
            add(Button.builder(Component.translatable("gui.itemview.cancel"), b -> {
                revert();
                editing = false;
                scroll = scrollTarget = 0;
                rebuildWidgets();
            }).bounds(ix + bw + gap, row2, bw, bh).build());
            setInitialFocus(nameBox);
        } else if (creative) {
            int by = Math.min(py + ph + 10, height - 28);
            add(Button.builder(Component.translatable("gui.itemview.edit"), b -> {
                editing = true;
                rebuildWidgets();
            }).bounds(px, by, 130, 20).build());
        }
    }

    private void fromHand() {
        var p = minecraft.player;
        if (p == null) return;
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack s = p.getItemInHand(other);
        if (s.isEmpty()) {
            notice(Component.translatable("gui.itemview.hand_empty").getString());
        } else if (s.getItem() instanceof ExhibitItem) {
            notice(Component.translatable("gui.itemview.hand_self").getString());
        } else {
            ItemStack c = s.copy();
            c.setCount(1);
            shown = c;
            ScreenAudio.play(ModSounds.PICK, 1f, 0.8f);
        }
    }

    private void notice(String s) {
        notice = s;
        noticeUntil = Util.getMillis() + 3000;
    }

    private void save() {
        sync();
        name = cut(name, 128);
        desc = cut(desc, 6000);
        NetworkHandler.CHANNEL.sendToServer(new SaveExhibitPacket(
                (byte) (hand == InteractionHand.MAIN_HAND ? 0 : 1), name, desc, shown));
        sName = name;
        sDesc = desc;
        sShown = shown;
        ScreenAudio.play(ModSounds.SCRIBBLE, 1f, 0.8f);
        editing = false;
        scroll = scrollTarget = 0;
        rebuildWidgets();
    }

    private static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    @Override
    public void onClose() {
        if (anim.closing()) return;
        anim.startClose();
        ScreenAudio.play(ModSounds.CLOSE, 0.8f, 0.9f);
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (anim.done()) {
            minecraft.setScreen(null);
            return;
        }
        float a = anim.value();
        float ta = Mth.clamp(a, 0f, 1f);

        long now = Util.getMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float t = (now % 1000000L) / 1000f;
        scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f);
        zoom += (zoomTarget - zoom) * Math.min(1f, dt * 12f);

        for (AbstractWidget w : mine) w.visible = ta > 0.7f;

        g.fill(0, 0, width, height, GuiUtil.rgba(BG, ta * 0.90f));
        drawStage(g, ta);
        drawHeader(g, ta);
        drawRings(g, ta, t);
        drawItem(g, ta, t);
        drawPanel(g, ta);
        super.render(g, mx, my, pt);
        drawHints(g, ta);

        if (!notice.isEmpty() && now < noticeUntil) {
            g.drawCenteredString(font, notice, (int) (width * 0.318f), (int) (height * 0.86f),
                    GuiUtil.rgba(0xFF8A8A, ta));
        }
    }

    /** Уголки рамки видоискателя вокруг сцены. */
    private void drawStage(GuiGraphics g, float ta) {
        int x0 = (int) (width * 0.022f), y0 = (int) (height * 0.087f);
        int x1 = (int) (width * 0.617f), y1 = (int) (height * 0.90f);
        int len = Math.max(14, (int) (width * 0.022f));
        int c = GuiUtil.rgba(0xD8D4E4, ta * 0.75f);
        corner(g, x0, y0, 1, 1, len, 2, c);
        corner(g, x1, y0, -1, 1, len, 2, c);
        corner(g, x0, y1, 1, -1, len, 2, c);
        corner(g, x1, y1, -1, -1, len, 2, c);
    }

    private void drawHeader(GuiGraphics g, float ta) {
        int cy = (int) (height * 0.043f);
        float x = width * 0.026f;
        GuiUtil.diamond(g, x, cy, 9f, GuiUtil.rgba(0xFFFFFF, ta));
        String text = Component.translatable("gui.itemview.header").getString().toUpperCase(Locale.ROOT);
        float tx = width * 0.040f;
        int tw = GuiUtil.spaced(g, font, text, tx, cy - 6, 1.35f, 1.6f, GuiUtil.rgba(0xEDE9F5, ta));
        int lx = (int) tx + tw + 16;
        int rx = (int) (width * 0.60f);
        if (rx > lx) g.fill(lx, cy, rx, cy + 1, GuiUtil.rgba(0xFFFFFF, ta * 0.25f));
    }

    /** Концентрические пунктирные кольца и яркие дуги вокруг предмета. */
    private void drawRings(GuiGraphics g, float ta, float t) {
        float cx = width * 0.318f, cy = height * 0.493f, r = height * 0.35f;
        float s = t * 4f;
        Hud.Batch b = new Hud.Batch(g);

        b.dashed(cx, cy, r, 1.2f, t * 3f, 1.6f, 2.4f, GuiUtil.rgba(0xFFFFFF, ta * 0.30f));
        b.dashed(cx, cy, r * 0.885f, 1.0f, -t * 5f, 3f, 5f, GuiUtil.rgba(0xFFFFFF, ta * 0.16f));
        b.dashed(cx, cy, r * 0.77f, 1.0f, t * 4f, 1f, 6f, GuiUtil.rgba(0xFFFFFF, ta * 0.12f));
        b.dashed(cx, cy, r * 1.05f, 1.6f, 0f, 0.7f, 9f, GuiUtil.rgba(0xFFFFFF, ta * 0.22f));

        int bold = GuiUtil.rgba(0xEDE9F5, ta * 0.85f);
        b.arc(cx, cy, r * 0.925f, 2.4f, 219f + s, 253f + s, bold);
        b.arc(cx, cy, r * 0.925f, 2.4f, -21f + s, 13f + s, bold);
        b.arc(cx, cy, r * 0.925f, 2.4f, 98f + s, 132f + s, bold);
        int thin = GuiUtil.rgba(0xFFFFFF, ta * 0.35f);
        b.arc(cx, cy, r * 0.78f, 1.2f, 212f - s * 1.5f, 246f - s * 1.5f, thin);
        b.arc(cx, cy, r * 0.78f, 1.2f, 40f - s * 1.5f, 60f - s * 1.5f, thin);

        for (int i = 0; i < 26; i++) {
            float ang = (float) Math.toRadians((GuiUtil.hashUnit(i, 1) + 1f) * 180f);
            float rr = r * (0.50f + (GuiUtil.hashUnit(i, 2) + 1f) * 0.5f * 0.75f);
            float dx = cx + (float) Math.cos(ang) * rr, dy = cy + (float) Math.sin(ang) * rr;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 1.3f + i * 1.7f));
            b.rect(dx, dy, dx + 2, dy + 2, GuiUtil.rgba(0xB7B2C8, ta * 0.55f * tw));
        }
        b.end();
    }

    /** Предмет в 3D: вращение мышью, масштаб колесом, лёгкое покачивание. */
    private void drawItem(GuiGraphics g, float ta, float t) {
        float cx = width * 0.318f, cy = height * 0.493f;
        if (shown.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.itemview.no_item"), (int) cx, (int) cy - 4,
                    GuiUtil.rgba(0x8E8798, ta));
            return;
        }
        ItemRenderer ir = minecraft.getItemRenderer();
        BakedModel model = ir.getModel(shown, minecraft.level, null, 0);

        float size = height * 0.30f * zoom * (0.82f + 0.18f * ta);
        float sway = dragging ? 0f : (float) Math.sin(t * 0.8f) * 5f;
        float introSpin = (1f - ta) * 70f;

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 200f);
        ps.mulPoseMatrix(new Matrix4f().scaling(1f, -1f, 1f));
        ps.scale(size, size, size);
        ps.mulPose(Axis.XP.rotationDegrees(pitch + (dragging ? 0f : (float) Math.sin(t * 0.6f) * 2f)));
        ps.mulPose(Axis.YP.rotationDegrees(yaw + sway + introSpin));

        boolean flat = !model.usesBlockLight();
        if (flat) Lighting.setupForFlatItems();
        RenderSystem.enableDepthTest();
        ir.render(shown, ItemDisplayContext.GUI, false, ps, g.bufferSource(),
                15728880, OverlayTexture.NO_OVERLAY, model);
        g.flush();
        if (flat) Lighting.setupFor3DItems();
        ps.popPose();
    }

    // ------------------------------------------------------------------ правая панель

    private void drawPanel(GuiGraphics g, float ta) {
        PoseStack ps = g.pose();
        int shift = editing ? 0 : (int) ((1f - ta) * 50f);
        ps.pushPose();
        ps.translate(shift, 0, 0);

        frame(g, px, py, pw, ph, ta);
        int ix = px + pad, iw = pw - pad * 2;

        if (editing) {
            int lc = GuiUtil.rgba(0x8E8AA0, ta);
            g.drawString(font, Component.translatable("gui.itemview.name_label"), ix, py + 14, lc, false);
            g.drawString(font, Component.translatable("gui.itemview.desc_label"), ix, py + 54, lc, false);
        } else {
            // название
            ps.pushPose();
            ps.translate(ix, titleY, 0);
            ps.scale(TITLE_SCALE, TITLE_SCALE, 1f);
            for (int i = 0; i < titleLines.size(); i++) {
                g.drawString(font, titleLines.get(i), 0, i * 10, GuiUtil.rgba(0xFFFFFF, ta), false);
            }
            ps.popPose();

            // разделитель с ромбиком
            GuiUtil.diamond(g, ix + 3, divY + 0.5f, 7f, GuiUtil.rgba(0xFFFFFF, ta));
            g.fill(ix + 12, divY, px + pw - pad, divY + 1, GuiUtil.rgba(0xFFFFFF, ta * 0.22f));

            // подпись «ОПИСАНИЕ»
            String label = Component.translatable("gui.itemview.description").getString().toUpperCase(Locale.ROOT);
            int lw = GuiUtil.spaced(g, font, label, ix, labelY, 0.9f, 1.4f, GuiUtil.rgba(0x9A94B8, ta));
            g.fill(ix + lw + 8, labelY + 4, px + pw - pad, labelY + 5, GuiUtil.rgba(0xFFFFFF, ta * 0.18f));

            // поле текста
            g.fill(ix, boxY, ix + iw, boxY + boxH, GuiUtil.rgba(INSET_EDGE, ta));
            g.fill(ix + 1, boxY + 1, ix + iw - 1, boxY + boxH - 1, GuiUtil.rgba(INSET, ta));

            g.enableScissor(ix + 1 + shift, boxY + 1, ix + iw - 1 + shift, boxY + boxH - 1);
            ps.pushPose();
            ps.translate(ix + 8, boxY + 8 - scroll, 0);
            ps.scale(BODY_SCALE, BODY_SCALE, 1f);
            for (int i = 0; i < descLines.size(); i++) {
                g.drawString(font, descLines.get(i), 0, i * 12, GuiUtil.rgba(0xE8E2D6, ta), false);
            }
            ps.popPose();
            g.disableScissor();

            int solid = GuiUtil.rgba(INSET, ta), clear = GuiUtil.rgba(INSET, 0f);
            if (scroll > 1) g.fillGradient(ix + 1, boxY + 1, ix + iw - 1, boxY + 15, solid, clear);
            if (scroll < maxScroll - 1) g.fillGradient(ix + 1, boxY + boxH - 15, ix + iw - 1, boxY + boxH - 1, clear, solid);
        }
        ps.popPose();
    }

    /** Рамка панели: тёмная заливка, светлая кромка, угловые скобки и фиолетовые «зарубки». */
    private void frame(GuiGraphics g, int x, int y, int w, int h, float ta) {
        g.fill(x + 5, y + 7, x + w + 5, y + h + 7, GuiUtil.rgba(0x000000, ta * 0.40f));
        g.fill(x, y, x + w, y + h, GuiUtil.rgba(EDGE, ta));
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GuiUtil.rgba(PANEL, ta));
        g.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, GuiUtil.rgba(0xFFFFFF, ta * 0.05f), GuiUtil.rgba(0x000000, ta * 0.18f));
        g.fill(x + 2, y + 2, x + w - 2, y + 3, GuiUtil.rgba(0xFFFFFF, ta * 0.10f));

        int dark = GuiUtil.rgba(0x0A0810, ta);
        corner(g, x + 5, y + 5, 1, 1, 8, 2, dark);
        corner(g, x + w - 5, y + 5, -1, 1, 8, 2, dark);
        corner(g, x + 5, y + h - 5, 1, -1, 8, 2, dark);
        corner(g, x + w - 5, y + h - 5, -1, -1, 8, 2, dark);

        int pc = GuiUtil.rgba(PURPLE, ta * 0.95f);
        int ay1 = y + (int) (h * 0.12f), ay2 = y + (int) (h * 0.84f);
        g.fill(x - 1, ay1, x + 2, ay1 + 8, pc);
        g.fill(x - 1, ay1, x + 5, ay1 + 2, pc);
        g.fill(x - 1, ay2, x + 2, ay2 + 8, pc);
        g.fill(x - 1, ay2 + 6, x + 5, ay2 + 8, pc);
        int by1 = y + (int) (h * 0.30f), by2 = y + (int) (h * 0.62f), by3 = y + (int) (h * 0.84f);
        g.fill(x + w - 2, by1, x + w + 1, by1 + 8, pc);
        g.fill(x + w - 5, by1, x + w + 1, by1 + 2, pc);
        g.fill(x + w - 2, by2, x + w + 1, by2 + 8, pc);
        g.fill(x + w - 2, by3, x + w + 1, by3 + 8, pc);
        g.fill(x + w - 5, by3 + 6, x + w + 1, by3 + 8, pc);
        int cx1 = x + (int) (w * 0.17f), cx2 = x + (int) (w * 0.78f);
        g.fill(cx1, y + h - 2, cx1 + 12, y + h + 1, pc);
        g.fill(cx1, y + h - 5, cx1 + 2, y + h + 1, pc);
        g.fill(cx2, y + h - 2, cx2 + 12, y + h + 1, pc);
        g.fill(cx2 + 10, y + h - 5, cx2 + 12, y + h + 1, pc);
    }

    /** Угол-скобка: (cx, cy) — вершина угла, dx/dy — направление ног. */
    private static void corner(GuiGraphics g, int cx, int cy, int dx, int dy, int len, int th, int argb) {
        int hx0 = dx > 0 ? cx : cx - len, hx1 = dx > 0 ? cx + len : cx;
        int hy0 = dy > 0 ? cy : cy - th, hy1 = dy > 0 ? cy + th : cy;
        g.fill(hx0, hy0, hx1, hy1, argb);
        int vx0 = dx > 0 ? cx : cx - th, vx1 = dx > 0 ? cx + th : cx;
        int vy0 = dy > 0 ? cy : cy - len, vy1 = dy > 0 ? cy + len : cy;
        g.fill(vx0, vy0, vx1, vy1, argb);
    }

    // ------------------------------------------------------------------ подсказки внизу

    private void drawHints(GuiGraphics g, float ta) {
        int y = (int) (height * 0.945f);
        int x = (int) (width * 0.19f);
        if (editing) {
            g.drawString(font, Component.translatable("gui.itemview.hint_edit"), x, y + 1,
                    GuiUtil.rgba(0x6F6A72, ta), false);
            return;
        }
        x = key(g, x, y, "gui.itemview.key_lmb", "gui.itemview.act_rotate", ta);
        x = key(g, x, y, "gui.itemview.key_wheel", "gui.itemview.act_zoom", ta);
        x = key(g, x, y, "gui.itemview.key_r", "gui.itemview.act_reset", ta);
        key(g, x, y, "gui.itemview.key_esc", "gui.itemview.act_close", ta);
    }

    private int key(GuiGraphics g, int x, int y, String keyId, String actionId, float ta) {
        String k = Component.translatable(keyId).getString();
        String a = Component.translatable(actionId).getString();
        int kw = font.width(k) + 10;
        g.fill(x, y - 3, x + kw, y + 12, GuiUtil.rgba(0x2C2738, ta));
        g.fill(x, y - 3, x + kw, y - 2, GuiUtil.rgba(0xFFFFFF, ta * 0.12f));
        g.drawString(font, k, x + 5, y + 1, GuiUtil.rgba(0xF2EEFA, ta), false);
        g.drawString(font, a, x + kw + 6, y + 1, GuiUtil.rgba(0x8E8AA0, ta), false);
        return x + kw + 6 + font.width(a) + 22;
    }

    // ------------------------------------------------------------------ ввод

    private void resetView() {
        yaw = 0f;
        pitch = 0f;
        zoomTarget = 1f;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && mx < px - 6) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            yaw += (float) dx * 0.8f;
            pitch = Mth.clamp(pitch + (float) dy * 0.8f, -85f, 85f);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < px - 6) {
            zoomTarget = Mth.clamp(zoomTarget * (1f + (float) delta * 0.1f), 0.5f, 3f);
            return true;
        }
        if (!editing) {
            scrollTarget = Mth.clamp(scrollTarget - (float) delta * 28f, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!editing) {
            if (key == GLFW.GLFW_KEY_R) {
                resetView();
                return true;
            }
            if (minecraft.options.keyInventory.matches(key, scan)) {
                onClose();
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }
}
