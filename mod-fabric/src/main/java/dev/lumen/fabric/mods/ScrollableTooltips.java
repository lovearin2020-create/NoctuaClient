package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import dev.lumen.fabric.Hooks;

public class ScrollableTooltips extends Module {
    private final Setting.Num speed = num("Scroll Speed", 10, 2, 30, 1);
    private float sx, sy;
    private long lastDraw;

    public ScrollableTooltips() { super("Scrollable Tooltips", "Scroll long tooltips (SHIFT = sideways).", Category.VISUAL); }

    /** Called while a tooltip is being laid out. Offsets reset once no tooltip has been shown for a moment. */
    public void offset(org.joml.Matrix3x2fStack pose) {
        long now = System.currentTimeMillis();
        if (now - lastDraw > 150) { sx = 0; sy = 0; }
        lastDraw = now;
        if (sx != 0 || sy != 0) pose.translate(sx, sy);
    }

    public void scroll(double amount) {
        if (amount == 0 || mc.gui.screen() == null || System.currentTimeMillis() - lastDraw > 150) return;
        float step = (amount > 0 ? 1 : -1) * speed.f();
        if (Hooks.keyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT) || Hooks.keyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT)) sx += step; else sy += step;
    }
}
