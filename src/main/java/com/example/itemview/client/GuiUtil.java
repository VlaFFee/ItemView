package com.example.itemview.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class GuiUtil {

    /** Углы прямоугольника (TL, TR, BR, BL) с поворотом вокруг центра. */
    public static float[] rect(float cx, float cy, float w, float h, float angDeg) {
        float[][] p = {{-w / 2, -h / 2}, {w / 2, -h / 2}, {w / 2, h / 2}, {-w / 2, h / 2}};
        float rad = (float) Math.toRadians(angDeg);
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
        float[] out = new float[8];
        for (int i = 0; i < 4; i++) {
            float x = p[i][0], y = p[i][1];
            out[i * 2] = cx + x * cos - y * sin;
            out[i * 2 + 1] = cy + x * sin + y * cos;
        }
        return out;
    }

    public static void quadColor(GuiGraphics g, float[] c, int argb) {
        int a = argb >>> 24 & 255, r = argb >> 16 & 255, gr = argb >> 8 & 255, b = argb & 255;
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, c[0], c[1], 0).color(r, gr, b, a).endVertex();
        bb.vertex(m, c[6], c[7], 0).color(r, gr, b, a).endVertex();
        bb.vertex(m, c[4], c[5], 0).color(r, gr, b, a).endVertex();
        bb.vertex(m, c[2], c[3], 0).color(r, gr, b, a).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    /** Ромбик (как маркеры в заголовках на макете). */
    public static void diamond(GuiGraphics g, float cx, float cy, float size, int argb) {
        quadColor(g, rect(cx, cy, size, size, 45f), argb);
    }

    /** Текст с разрядкой и масштабом. Возвращает ширину в пикселях экрана. */
    public static int spaced(GuiGraphics g, Font font, String s, float x, float y, float scale, float spacing, int argb) {
        var ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0);
        ps.scale(scale, scale, 1f);
        float cx = 0;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            g.drawString(font, ch, Math.round(cx), 0, argb, false);
            cx += font.width(ch) + spacing;
        }
        ps.popPose();
        return (int) (cx * scale);
    }

    public static int spacedWidth(Font font, String s, float scale, float spacing) {
        float cx = 0;
        for (int i = 0; i < s.length(); i++) cx += font.width(String.valueOf(s.charAt(i))) + spacing;
        return (int) (cx * scale);
    }

    /** Стабильное "случайное" число в [-1; 1]. */
    public static float hashUnit(int seed, int salt) {
        int h = (seed * 31 + salt * 7919) * 0x9E3779B1;
        h ^= h >>> 15;
        h &= 0xFFFF;
        return h / 65535f * 2f - 1f;
    }

    /** RGB + прозрачность -> ARGB. */
    public static int rgba(int rgb, float a) {
        return (int) (Math.max(0f, Math.min(1f, a)) * 255) << 24 | (rgb & 0xFFFFFF);
    }

    private GuiUtil() {}
}
