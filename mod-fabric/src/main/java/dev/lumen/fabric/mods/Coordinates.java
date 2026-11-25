package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Coordinates extends HudModule {
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool direction = bool("Direction", true);
    private final Setting.Mode layout = mode("Layout", "Vertical", "Vertical", "Horizontal");
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private final Setting.Col label = col("Label Color", 0xFF4C8DFF);
    private float w = 70;

    public Coordinates() { super("Coordinates", "Shows your position in the world.", START, 4, START, 24); }

    public float width() { return w; }
    public float height() { return layout.is("Vertical") ? 38 : 16; }

    public void draw(boolean editing) {
        int x = (int) Math.floor(mc.player.getX()), y = (int) Math.floor(mc.player.getY()), z = (int) Math.floor(mc.player.getZ());
        float yaw = ((mc.player.getYRot() % 360f) + 360f) % 360f;
        String dir = DIRS[Math.round(yaw / 45f) & 7];
        if (layout.is("Vertical")) {
            String[] k = {"X", "Y", "Z"}; int[] v = {x, y, z};
            int max = 0;
            for (int n : v) max = Math.max(max, mc.font.width(String.valueOf(n)));
            w = 8 + 12 + max + (direction.on() ? 22 : 6);
            if (background.on()) Draw.roundRect(0, 0, w, 38, 3, 0x70000000);
            for (int i = 0; i < 3; i++) {
                Draw.text(k[i], 6, 5 + i * 10, label.get(), true);
                Draw.text(String.valueOf(v[i]), 18, 5 + i * 10, color.get(), true);
            }
            if (direction.on()) Draw.text(dir, w - 6 - mc.font.width(dir), 15, label.get(), true);
        } else {
            String s = x + ", " + y + ", " + z + (direction.on() ? "  " + dir : "");
            w = mc.font.width(s) + 12;
            if (background.on()) Draw.roundRect(0, 0, w, 16, 3, 0x70000000);
            Draw.text(s, 6, 4, color.get(), true);
        }
    }
}
