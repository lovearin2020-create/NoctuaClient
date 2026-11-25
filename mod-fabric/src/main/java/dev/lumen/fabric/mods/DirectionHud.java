package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.HudModule;
import dev.lumen.fabric.core.Setting;
import dev.lumen.fabric.util.Draw;
import net.minecraft.util.Mth;

/** A compass strip: letters for the eight directions, degree marks between them and your exact heading in the middle. */
public class DirectionHud extends HudModule {
    private static final int W = 200, H = 30;
    private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool degrees = bool("Show Degrees", true);
    private final Setting.Bool heading = bool("Show Heading", true);
    private final Setting.Col color = col("Marker Color", 0xFF4C8DFF);
    private final Setting.Col north = col("North Color", 0xFFFF5A5A);

    public DirectionHud() { super("Direction HUD", "Compass bar.", CENTER, -100, START, 4); }

    public float width() { return W; }
    public float height() { return H; }

    public void draw(boolean editing) {
        if (mc.player == null) return;
        if (background.on()) Draw.roundRect(0, 0, W, H, 6, 0x78000000);
        float yaw = Mth.wrapDegrees(mc.player.getYRot());
        float per = 1.3f, half = W / 2f - 8; // pixels per degree; how far from the middle things are drawn
        for (int deg = -180; deg < 180; deg += 5) {
            float off = Mth.wrapDegrees(deg - yaw) * per;
            if (Math.abs(off) > half) continue;
            float a = Math.min(1f, (half - Math.abs(off)) / 26f);
            float x = W / 2f + off;
            boolean gap = heading.on() && Math.abs(off) < 15; // leave room for the heading in the middle
            if (deg % 45 == 0) {
                String n = NAMES[((deg + 360) % 360) / 45];
                boolean main = n.length() == 1;
                int c = Draw.alpha(n.equals("N") ? north.get() : main ? 0xFFFFFFFF : 0xFFAEB6C8, a);
                if ((c >>> 24) > 8) { if (main) Draw.uiCenterB(n, x, 3, 10, c); else Draw.uiCenter(n, x, 5, 7, c); }
                if (!gap) Draw.rect(x - 0.5f, 17, 1, 6, Draw.alpha(0xFFFFFFFF, a));
            } else if (deg % 15 == 0) {
                if (degrees.on()) {
                    int c = Draw.alpha(0xFF8891A5, a * 0.9f);
                    if ((c >>> 24) > 8) Draw.uiCenter(String.valueOf((deg + 540) % 360), x, 6, 6, c);
                }
                if (!gap) Draw.rect(x - 0.5f, 19, 1, 4, Draw.alpha(0xCCFFFFFF, a));
            } else if (!gap) {
                Draw.rect(x - 0.5f, 21, 1, 2, Draw.alpha(0x66FFFFFF, a));
            }
        }
        int m = color.get();
        // the pointer: a small downward triangle built from three bars
        Draw.rect(W / 2f - 4, 0, 8, 1.5f, m); Draw.rect(W / 2f - 2.5f, 1.5f, 5, 1.5f, m); Draw.rect(W / 2f - 1, 3, 2, 1.5f, m);
        if (heading.on()) {
            String h = String.valueOf(Math.round((yaw + 540f) % 360f) % 360);
            float w = Math.max(24, Draw.uiWidth(h, 8, true) + 10);
            Draw.roundRect(W / 2f - w / 2f, 16, w, 11, 5, m);
            Draw.uiCenterB(h, W / 2f, 17.4f, 8, 0xFFFFFFFF);
        } else Draw.rect(W / 2f - 0.5f, 5, 1, H - 8, m);
    }
}
