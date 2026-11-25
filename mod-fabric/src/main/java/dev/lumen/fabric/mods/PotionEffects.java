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
    public float height() { return rows * 22 - 2; }

    private static String time(MobEffectInstance e) {
        if (e.isInfiniteDuration()) return "\u221E";
        int s = e.getDuration() / 20;
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    private void row(int i, String name, String time, int tint) {
        if (background.on()) Draw.roundRect(0, i * 22, w, 20, 3, 0x70000000);
        Draw.rect(0, i * 22 + 3, 2, 14, 0xFF000000 | tint);
        Draw.text(name, 7, i * 22 + 2, color.get(), true);
        Draw.text(time, 7, i * 22 + 11, 0xFFB3BACB, true);
    }

    public void draw(boolean editing) {
        java.util.Collection<MobEffectInstance> list = mc.player.getActiveEffects();
        if (list.isEmpty()) {
            rows = 1;
            if (editing) { w = 90; row(0, "Speed II", "1:30", 0x33EBFF); }
            return;
        }
        float max = 60;
        int i = 0;
        String[] names = new String[list.size()];
        for (MobEffectInstance e : list) {
            int amp = e.getAmplifier();
            names[i] = e.getEffect().value().getDisplayName().getString() + (amp > 0 && amp < 10 ? ROMAN[amp] : amp >= 10 ? " " + (amp + 1) : "");
            max = Math.max(max, mc.font.width(names[i]) + 14);
            i++;
        }
        w = max; rows = list.size();
        i = 0;
        for (MobEffectInstance e : list) { row(i, names[i], time(e), e.getEffect().value().getColor()); i++; }
    }
}
