package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Crosshair extends Module {
    private final Setting.Mode style = mode("Style", "Cross", "Cross", "Dot", "Circle", "Square");
    private final Setting.Num size = num("Length", 5, 1, 15, 1);
    private final Setting.Num gap = num("Gap", 2, 0, 10, 1);
    private final Setting.Num thick = num("Thickness", 1, 1, 4, 1);
    private final Setting.Bool dot = bool("Center Dot", false);
    private final Setting.Bool outline = bool("Outline", true);
    private final Setting.Bool hideThirdPerson = bool("Hide In Third Person", true);
    private final Setting.Col color = col("Color", 0xFFFFFFFF);

    public Crosshair() { super("Crosshair", "Replaces the crosshair with a custom one.", Category.VISUAL); }

    private void bar(float x, float y, float w, float h) {
        if (outline.on()) Draw.rect(x - 1, y - 1, w + 2, h + 2, 0xFF000000);
    }

    public void draw(int sw, int sh) {
        if (hideThirdPerson.on() && !mc.options.getCameraType().isFirstPerson()) return;
        int c = color.get(), t = thick.i(), s = size.i(), g = gap.i();
        float cx = sw / 2f - t / 2f, cy = sh / 2f - t / 2f;
        float[][] r;
        switch (style.get()) {
            case "Dot": r = new float[][]{{cx - 1, cy - 1, t + 2, t + 2}}; break;
            case "Square": r = new float[][]{{cx - s, cy - s, 2 * s + t, t}, {cx - s, cy + s, 2 * s + t, t}, {cx - s, cy - s + t, t, 2 * s - t}, {cx + s, cy - s + t, t, 2 * s - t}}; break;
            case "Circle": {
                int n = Math.max(12, s * 6);
                r = new float[n][];
                for (int i = 0; i < n; i++) { double a = Math.PI * 2 * i / n; r[i] = new float[]{Math.round(sw / 2f + Math.cos(a) * (s + 0.5) - 0.5f), Math.round(sh / 2f + Math.sin(a) * (s + 0.5) - 0.5f), 1, 1}; }
                break;
            }
            default: r = new float[][]{{cx - g - s, cy, s, t}, {cx + t + g, cy, s, t}, {cx, cy - g - s, t, s}, {cx, cy + t + g, t, s}};
        }
        boolean center = dot.on() && !style.is("Dot");
        for (float[] b : r) bar(b[0], b[1], b[2], b[3]);
        if (center) bar(cx, cy, t, t);
        for (float[] b : r) Draw.rect(b[0], b[1], b[2], b[3], c);
        if (center) Draw.rect(cx, cy, t, t, c);
    }
}
