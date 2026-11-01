package com.example.itemview.client;

import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Плавное появление/исчезновение экрана. value(): 0 = скрыт, 1 = показан. */
@OnlyIn(Dist.CLIENT)
public final class ScreenAnim {
    private static final float OPEN_MS = 520f, CLOSE_MS = 300f;

    private final long start = Util.getMillis();
    private long closeStart = -1;

    public boolean closing() {
        return closeStart >= 0;
    }

    public void startClose() {
        if (closeStart < 0) closeStart = Util.getMillis();
    }

    public boolean done() {
        return closing() && Util.getMillis() - closeStart >= CLOSE_MS;
    }

    public float value() {
        long now = Util.getMillis();
        if (closing()) {
            float t = clamp((now - closeStart) / CLOSE_MS);
            return 1f - t * t;
        }
        float u = 1f - clamp((now - start) / OPEN_MS);
        return 1f - u * u * u;
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
