package dev.lumen.fabric.gui;

import dev.lumen.fabric.util.Draw;

/** Animated aurora background for the Lumen title screen. */
public final class Backdrop {
    private static final long START = System.currentTimeMillis();
    private static final int[] ORBS = {0x4C8DFF, 0x8B5CFF, 0x38D9F5, 0xFF6BCB};

    /** Soft glow built from a few stacked translucent discs. */
    private static void glow(float cx, float cy, float r, int rgb, float strength) {
        int layers = 7;
        for (int l = 0; l < layers; l++) {
            float rr = r * (1f - l / (float) layers);
            int color = Math.round(9 * strength) << 24 | rgb;
            int step = 4;
            for (int y = (int) -rr; y < rr; y += step) {
                float half = (float) Math.sqrt(Math.max(0, rr * rr - (y + step / 2f) * (y + step / 2f)));
                Draw.rect(cx - half, cy + y, half * 2, step, color);
            }
        }
    }

    public static void draw(int w, int h, double mx, double my) {
        float t = (System.currentTimeMillis() - START) / 1000f;
        Draw.g().fillGradient(0, 0, w, h, 0xFF070A16, 0xFF0F1630);
        float px = (float) (mx - w / 2f) / w, py = (float) (my - h / 2f) / h;
        float big = Math.max(w, h);
        for (int i = 0; i < ORBS.length; i++) {
            float x = w * (0.5f + 0.38f * (float) Math.sin(t * 0.11f + i * 1.9f)) - px * 30;
            float y = h * (0.5f + 0.32f * (float) Math.cos(t * 0.09f + i * 2.4f)) - py * 30;
            glow(x, y, big * (0.34f + 0.05f * (float) Math.sin(t * 0.2f + i)), ORBS[i], 1f);
        }
        for (int i = 0; i < 70; i++) {
            float sx = hash(i * 3 + 1), sy = hash(i * 3 + 2), depth = 0.3f + hash(i * 3 + 3) * 0.7f;
            float x = (sx * w + (float) Math.sin(t * 0.3f * depth + i) * 12 - px * 40 * depth + w) % w;
            float y = ((sy * h - t * 9f * depth) % h + h) % h - py * 40 * depth;
            float tw = 0.45f + 0.55f * (float) Math.sin(t * (0.8f + depth) + i * 1.3f);
            int a = (int) (40 + 150 * depth * tw);
            int s = depth > 0.75f ? 2 : 1;
            Draw.rect(x, y, s, s, (Math.max(0, Math.min(255, a)) << 24) | 0xDDE8FF);
        }
        Draw.g().fillGradient(0, (int) (h * 0.55f), w, h, 0x00000000, 0x66000000);
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647f;
    }
}
