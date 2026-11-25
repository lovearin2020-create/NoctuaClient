package dev.lumen.gui;

import dev.lumen.Lumen;
import dev.lumen.core.Config;
import dev.lumen.core.HudModule;
import dev.lumen.core.Module;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/** Drag HUD elements to move them, scroll over one to resize it. */
public class HudEditorScreen extends GuiScreen {
    private HudModule dragging;
    private float offX, offY;

    public boolean doesGuiPauseGame() { return false; }

    public void onGuiClosed() { Config.save(); }

    private HudModule at(ScaledResolution sr, int mx, int my) {
        HudModule hit = null;
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled() || !(m instanceof HudModule)) continue;
            HudModule h = (HudModule) m;
            if (R.in(mx, my, h.px(sr) - 2, h.py(sr) - 2, h.width() * h.scale + 4, h.height() * h.scale + 4)) hit = h;
        }
        return hit;
    }

    public void drawScreen(int mx, int my, float pt) {
        ScaledResolution sr = new ScaledResolution(mc);
        Fnt.transform(1f, 0, 0);
        if (mc.theWorld == null) drawDefaultBackground(); else R.rect(0, 0, width, height, 0x55000000);

        if (dragging != null) {
            if (!Mouse.isButtonDown(0)) dragging = null;
            else {
                float nx = mx - offX, ny = my - offY;
                float w = dragging.width() * dragging.scale, h = dragging.height() * dragging.scale;
                // snap to the screen centre lines
                if (Math.abs(nx + w / 2f - width / 2f) < 4) { nx = width / 2f - w / 2f; R.rect(width / 2f, 0, 0.5f, height, R.ACCENT); }
                if (Math.abs(ny + h / 2f - height / 2f) < 4) { ny = height / 2f - h / 2f; R.rect(0, height / 2f, width, 0.5f, R.ACCENT); }
                nx = Math.max(0, Math.min(width - w, nx));
                ny = Math.max(0, Math.min(height - h, ny));
                dragging.moveTo(sr, nx, ny);
            }
        }

        HudModule hover = dragging != null ? dragging : at(sr, mx, my);
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled() || !(m instanceof HudModule)) continue;
            HudModule h = (HudModule) m;
            h.render(sr, true);
            float x = h.px(sr) - 2, y = h.py(sr) - 2, w = h.width() * h.scale + 4, hh = h.height() * h.scale + 4;
            R.roundOutline(x, y, w, hh, 3, h == hover ? 1.5f : 1f, h == hover ? R.ACCENT : 0x66FFFFFF);
            if (h == hover) {
                String label = h.name + "  " + Math.round(h.scale * 100) + "%";
                float lw = Fnt.width(label, 8) + 8;
                float ly = y - 13 < 0 ? y + hh + 2 : y - 13;
                float lx = Math.max(0, Math.min(width - lw, x));
                R.roundRect(lx, ly, lw, 11, 3, 0xE6101216);
                Fnt.draw(label, lx + 4, ly + 2, 8, 0xFFFFFFFF);
            }
        }

        boolean bh = R.in(mx, my, width / 2f - 40, height / 2f - 11, 80, 22);
        R.roundRect(width / 2f - 40, height / 2f - 11, 80, 22, 6, bh ? 0xF2283041 : 0xE6101216);
        R.roundOutline(width / 2f - 40, height / 2f - 11, 80, 22, 6, 1f, bh ? R.ACCENT : 0xFF2B313D);
        Fnt.centerB("MODS", width / 2f, height / 2f - 5, 10, 0xFFFFFFFF);

        String hint = "Drag to move   |   Scroll to resize   |   Right-click to reset size   |   ESC to save";
        float hw = Fnt.width(hint, 8) + 16;
        R.roundRect(width / 2f - hw / 2f, height - 22, hw, 15, 5, 0xE6101216);
        Fnt.center(hint, width / 2f, height - 18, 8, 0xFFB3BACB);
        R.reset();
    }

    protected void mouseClicked(int mx, int my, int button) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        if (button == 0 && R.in(mx, my, width / 2f - 40, height / 2f - 11, 80, 22) && at(sr, mx, my) == null) {
            mc.displayGuiScreen(new ModMenuScreen());
            return;
        }
        HudModule h = at(sr, mx, my);
        if (h == null) return;
        if (button == 0) { dragging = h; offX = mx - h.px(sr); offY = my - h.py(sr); }
        else if (button == 1) h.scale = 1f;
    }

    protected void mouseReleased(int mx, int my, int state) { dragging = null; }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int mx = Mouse.getEventX() * width / mc.displayWidth, my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        HudModule h = at(sr, mx, my);
        if (h != null) h.scale = Math.max(0.5f, Math.min(2.5f, Math.round((h.scale + (d > 0 ? 0.05f : -0.05f)) * 20f) / 20f));
    }

    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE || dev.lumen.core.Client.keyMenu.is(key)) mc.displayGuiScreen(null);
    }
}
