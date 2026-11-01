package com.example.itemview.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/** Рисование HUD-декора (кольца, дуги, точки) одним батчем. Между begin() и end() нельзя вызывать g.fill и т.п. */
@OnlyIn(Dist.CLIENT)
public final class Hud {

    public static final class Batch {
        private final BufferBuilder bb = Tesselator.getInstance().getBuilder();
        private final Matrix4f m;

        public Batch(GuiGraphics g) {
            m = g.pose().last().pose();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        private void v(float x, float y, int r, int g, int b, int a) {
            bb.vertex(m, x, y, 0).color(r, g, b, a).endVertex();
        }

        /** Дуга от a0 до a1 градусов (0 = вправо, по часовой на экране). */
        public void arc(float cx, float cy, float rad, float thick, float a0, float a1, int argb) {
            int a = argb >>> 24 & 255, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
            if (a == 0) return;
            float ri = rad - thick / 2f, ro = rad + thick / 2f;
            int n = Math.max(1, (int) Math.ceil(Math.abs(a1 - a0) / 3f));
            for (int i = 0; i < n; i++) {
                double t0 = Math.toRadians(a0 + (a1 - a0) * i / n);
                double t1 = Math.toRadians(a0 + (a1 - a0) * (i + 1) / n);
                float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0);
                float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
                v(cx + ro * c0, cy + ro * s0, r, g, b, a);
                v(cx + ri * c0, cy + ri * s0, r, g, b, a);
                v(cx + ri * c1, cy + ri * s1, r, g, b, a);
                v(cx + ro * c1, cy + ro * s1, r, g, b, a);
            }
        }

        /** Пунктирная окружность. */
        public void dashed(float cx, float cy, float rad, float thick, float offsetDeg, float dashDeg, float gapDeg, int argb) {
            float step = dashDeg + gapDeg;
            for (float a = 0; a < 360f; a += step) arc(cx, cy, rad, thick, offsetDeg + a, offsetDeg + a + dashDeg, argb);
        }

        public void rect(float x0, float y0, float x1, float y1, int argb) {
            int a = argb >>> 24 & 255, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
            if (a == 0) return;
            v(x0, y1, r, g, b, a);
            v(x1, y1, r, g, b, a);
            v(x1, y0, r, g, b, a);
            v(x0, y0, r, g, b, a);
        }

        public void end() {
            BufferUploader.drawWithShader(bb.end());
        }
    }

    private Hud() {}
}
