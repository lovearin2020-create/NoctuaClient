package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.effect.MobEffectInstance;

public class PotionEffects extends HudModule {
    public final Setting.Bool hideVanilla = bool("Hide Vanilla Icons", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private int rows = 1;
    private float w = 90;
    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    public PotionEffects() { super("Potion Effects", "Lists your active effects and their time left.", END, 4, START, 4); }

    public float width() { return w; }
    public float height() { return lumen() ? rows * 20 + 2 : rows * 22 - 2; }
    /** The longest each effect has been seen to last, for the bar under it. */
    private final java.util.Map<Object, Integer> longest = new java.util.HashMap<>();

    private static String time(MobEffectInstance e) {
        if (e.isInfiniteDuration()) return "\u221E";
        int s = e.getDuration() / 20;
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    private void row(int i, String name, String time, int tint) { row(i, name, time, tint, 1f, false); }

    private void row(int i, String name, String time, int tint, float left, boolean low) {
        if (lumen()) {
            // each row: 5 above the letters, the bar, 5 below; name and bar centred on the colour mark
            float y = 1 + i * 20, tw = Draw.uiWidth(time, 8);
            Draw.roundRect(6, y + 4, 2.5f, 12, 1.2f, 0xFF000000 | tint);
            Draw.ui(name, 13, cap(y + 4.75f, 8), 8, color.get());
            Draw.ui(time, w - 7 - tw, cap(y + 4.75f, 8), 8, low ? 0xFFFFB454 : 0xFFFFFFFF);
            Draw.roundRect(13, y + 13.25f, w - 20, 2, 1, 0x30FFFFFF);
            Draw.roundRect(13, y + 13.25f, Math.max(2, (w - 20) * left), 2, 1, low ? 0xFFFFB454 : 0xFF000000 | tint);
            return;
        }
        if (background.on()) Draw.roundRect(0, i * 22, w, 20, 3, 0x70000000);
        Draw.rect(0, i * 22 + 3, 2, 14, 0xFF000000 | tint);
        Draw.text(name, 7, i * 22 + 2, color.get(), true);
        Draw.text(time, 7, i * 22 + 11, 0xFFB3BACB, true);
    }

    public void draw(boolean editing) {
        java.util.Collection<MobEffectInstance> list = mc.player.getActiveEffects();
        if (list.isEmpty()) {
            rows = 1;
            if (editing) { w = lumen() ? 112 : 90; if (lumen()) panel(0, 0, w, height()); row(0, "Speed II", "1:30", 0x33EBFF, 0.7f, false); }
            return;
        }
        float max = 60;
        int i = 0;
        String[] names = new String[list.size()];
        for (MobEffectInstance e : list) {
            int amp = e.getAmplifier();
            names[i] = e.getEffect().value().getDisplayName().getString() + (amp > 0 && amp < 10 ? ROMAN[amp] : amp >= 10 ? " " + (amp + 1) : "");
            max = Math.max(max, lumen() ? Draw.uiWidth(names[i], 8) + 13 + 12 + Draw.uiWidth("00:00", 8) + 7 : mc.font.width(names[i]) + 14);
            i++;
        }
        w = max; rows = list.size();
        if (lumen()) panel(0, 0, w, height());
        i = 0;
        for (MobEffectInstance e : list) {
            Object key = e.getEffect();
            Integer top = longest.get(key);
            if (top == null || e.getDuration() > top) { top = e.getDuration(); longest.put(key, top); }
            float left = e.isInfiniteDuration() ? 1f : Math.max(0, Math.min(1, e.getDuration() / (float) Math.max(1, top)));
            row(i, names[i], time(e), e.getEffect().value().getColor(), left, !e.isInfiniteDuration() && e.getDuration() < 200);
            i++;
        }
    }
}
