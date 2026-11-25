package dev.lumen.fabric.gui;

import dev.lumen.fabric.Lumen;
import dev.lumen.fabric.core.Config;
import dev.lumen.fabric.core.HudModule;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag HUD elements to move them, scroll over one to resize it. */
public class HudEditorScreen extends Screen {
    private HudModule dragging;
    private float offX, offY;

    public HudEditorScreen() { super(Component.literal("Noctua HUD Editor")); }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { Config.save(); }
    @Override public boolean shouldCloseOnEsc() { return false; }

    /** No blur here: the game must stay visible while arranging the HUD. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        if (minecraft.level == null) super.extractBackground(g, mx, my, partial);
    }

    private HudModule at(double mx, double my) {
        HudModule hit = null;
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled() || !(m instanceof HudModule h)) continue;
            if (Draw.in(mx, my, h.px(width) - 2, h.py(height) - 2, h.width() * h.scale + 4, h.height() * h.scale + 4)) hit = h;
        }
        return hit;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        Draw.begin(g);
        Draw.rect(0, 0, width, height, 0x55000000);

        if (dragging != null) {
            float nx = mx - offX, ny = my - offY;
            float w = dragging.width() * dragging.scale, h = dragging.height() * dragging.scale;
            // snap to the screen centre lines
            if (Math.abs(nx + w / 2f - width / 2f) < 4) { nx = width / 2f - w / 2f; Draw.rect(width / 2f, 0, 1, height, Draw.ACCENT); }
            if (Math.abs(ny + h / 2f - height / 2f) < 4) { ny = height / 2f - h / 2f; Draw.rect(0, height / 2f, width, 1, Draw.ACCENT); }
            nx = Math.max(0, Math.min(width - w, nx));
            ny = Math.max(0, Math.min(height - h, ny));
            dragging.moveTo(width, height, nx, ny);
        }

        HudModule hover = dragging != null ? dragging : at(mx, my);
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled() || !(m instanceof HudModule h)) continue;
            h.render(width, height, true);
            float x = h.px(width) - 2, y = h.py(height) - 2, w = h.width() * h.scale + 4, hh = h.height() * h.scale + 4;
            Draw.outline(x, y, w, hh, h == hover ? Draw.ACCENT : 0x66FFFFFF);
        }
        if (hover != null) {
            float x = hover.px(width) - 2, y = hover.py(height) - 2, hh = hover.height() * hover.scale + 4;
            String label = hover.name + "  " + Math.round(hover.scale * 100) + "%";
            float lw = Draw.uiWidth(label, 8) + 8;
            float ly = y - 13 < 0 ? y + hh + 2 : y - 13;
            float lx = Math.max(0, Math.min(width - lw, x));
            Draw.roundRect(lx, ly, lw, 11, 3, 0xF0101216);
            Draw.ui(label, lx + 4, ly + 2, 8, 0xFFFFFFFF);
        }

        boolean bh = Draw.in(mx, my, width / 2f - 40, height / 2f - 11, 80, 22);
        Draw.roundBorder(width / 2f - 40, height / 2f - 11, 80, 22, 6, bh ? 0xFF283041 : 0xFF101216, bh ? Draw.ACCENT : 0xFF2B313D);
        Draw.uiCenterB("MODS", width / 2f, height / 2f - 5, 10, 0xFFFFFFFF);

        String hint = "Drag to move   |   Scroll to resize   |   Right-click to reset size   |   ESC to save";
        float hw = Draw.uiWidth(hint, 8) + 16;
        Draw.roundRect(width / 2f - hw / 2f, height - 22, hw, 15, 5, 0xF0101216);
        Draw.uiCenter(hint, width / 2f, height - 18, 8, 0xFFB3BACB);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        double mx = e.x(), my = e.y();
        HudModule h = at(mx, my);
        if (e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && h == null && Draw.in(mx, my, width / 2f - 40, height / 2f - 11, 80, 22)) {
            minecraft.gui.setScreen(new ModMenuScreen());
            return true;
        }
        if (h == null) return true;
        if (e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) { dragging = h; offX = (float) mx - h.px(width); offY = (float) my - h.py(height); }
        else if (e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) h.scale = 1f;
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) { dragging = null; return true; }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        HudModule h = at(mx, my);
        if (h != null && sy != 0) h.scale = Math.max(0.5f, Math.min(2.5f, Math.round((h.scale + (sy > 0 ? 0.05f : -0.05f)) * 20f) / 20f));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE || dev.lumen.fabric.core.Client.keyMenu.is(e.key())) minecraft.gui.setScreen(null);
        return true;
    }
}
