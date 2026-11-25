package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.server.packs.repository.Pack;

public class PackDisplay extends HudModule {
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private float w = 100;

    private final dev.lumen.fabric.core.Setting.Bool background = bool("Background", true);
    public PackDisplay() { super("Pack Display", "Shows your active resource pack.", END, 4, END, 70); }

    public float width() { return w; }
    public float height() { return lumen() ? 30 : 16; }

    public void draw(boolean editing) {
        String name = "Default";
        try {
            for (Pack p : mc.getResourcePackRepository().getSelectedPacks()) {
                if (p.getId().startsWith("file/")) name = Servers.strip(p.getTitle().getString()).replace(".zip", "");
            }
        } catch (Throwable ignored) {}
        if (name.length() > 28) name = name.substring(0, 27) + "...";
        if (lumen()) {
            String sub = name.equals("Default") ? "No resource pack" : "Resource pack";
            w = 9 + Math.max(Draw.uiWidth(name, 9, true), Draw.uiWidth(sub, 6)) + 10;
            if (background.on()) panel(0, 0, w, 30);
            Draw.uiB(name, 9, cap(7.25f, 9), 9, color.get());
            Draw.ui(sub, 9, cap(18.3f, 6), 6, 0xFF8891A5);
            return;
        }
        w = mc.font.width(name) + 12;
        if (background.on()) Draw.roundRect(0, 0, w, 16, 3, 0x70000000);
        Draw.text(name, 6, 4, color.get(), true);
    }
}
