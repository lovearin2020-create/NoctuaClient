package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import net.minecraft.util.MathHelper;

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

    protected boolean hasLumenStyle() { return false; }
    public float width() { return W; }
    public float height() { return H; }

    public void draw(boolean editing) {
        if (mc.thePlayer == null) return;
        if (background.on()) {
            R.roundRect(0, 0, W, H, 6, 0x78000000);
            R.gradient(4, 1, W - 8, 9, 0x14FFFFFF, 0x00FFFFFF);
        }
        float yaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        float per = 1.3f, half = W / 2f - 8; // pixels per degree; how far from the middle things are drawn
        for (int deg = -180; deg < 180; deg += 5) {
            float off = MathHelper.wrapAngleTo180_float(deg - yaw) * per;
            if (Math.abs(off) > half) continue;
            float a = Math.min(1f, (half - Math.abs(off)) / 26f);
            float x = W / 2f + off;
            boolean gap = heading.on() && Math.abs(off) < 15; // leave room for the heading in the middle
            if (deg % 45 == 0) {
                String n = NAMES[((deg + 360) % 360) / 45];
                boolean main = n.length() == 1;
                int c = R.alpha(n.equals("N") ? north.get() : main ? 0xFFFFFFFF : 0xFFAEB6C8, a);
                if ((c >>> 24) > 8) { if (main) Fnt.centerB(n, x, 3, 10, c); else Fnt.center(n, x, 5, 7, c); }
                if (!gap) R.rect(x - 0.5f, 17, 1, 6, R.alpha(0xFFFFFFFF, a));
            } else if (deg % 15 == 0) {
                if (degrees.on()) {
                    int c = R.alpha(0xFF8891A5, a * 0.9f);
                    if ((c >>> 24) > 8) Fnt.center(String.valueOf((deg + 540) % 360), x, 6, 6, c);
                }
                if (!gap) R.rect(x - 0.5f, 19, 1, 4, R.alpha(0xCCFFFFFF, a));
            } else if (!gap) {
                R.rect(x - 0.5f, 21, 1, 2, R.alpha(0x66FFFFFF, a));
            }
        }
        int m = color.get();
        R.poly(m, W / 2f - 4, 0, W / 2f + 4, 0, W / 2f, 5);
        if (heading.on()) {
            String h = String.valueOf(Math.round((yaw + 540f) % 360f) % 360);
            float w = Math.max(24, Fnt.widthB(h, 8) + 10);
            R.roundRect(W / 2f - w / 2f, 16, w, 11, 5, m);
            Fnt.centerB(h, W / 2f, 17.4f, 8, 0xFFFFFFFF);
        } else R.rect(W / 2f - 0.5f, 5, 1, H - 8, m);
    }
}
