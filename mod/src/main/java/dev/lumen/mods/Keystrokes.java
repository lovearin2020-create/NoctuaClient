package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.Clicks;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Keystrokes extends HudModule {
    private final Setting.Bool mouse = bool("Mouse Buttons", true);
    private final Setting.Bool showCps = bool("Show CPS", true);
    private final Setting.Bool space = bool("Space Bar", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col text = col("Text Color", 0xFFFFFFFF);
    private final Setting.Col pressed = col("Pressed Color", 0xFFFFFFFF);
    private final float[] fade = new float[7];

    public Keystrokes() { super("Keystrokes", "Movement keys and mouse buttons.", START, 4, CENTER, -10); }

    public float width() { return 70; }
    public float height() { return 46 + (mouse.on() ? 24 : 0) + (space.on() ? 14 : 0); }

    private static boolean down(KeyBinding k) {
        int c = k.getKeyCode();
        if (c == 0) return false;
        return c < 0 ? Mouse.isButtonDown(c + 100) : Keyboard.isKeyDown(c);
    }

    private void key(int i, String label, String sub, boolean down, float x, float y, float w, float h) {
        fade[i] = Util.lerp(fade[i], down ? 1 : 0, 0.3f);
        int bg = R.mix(0x70000000, R.alpha(pressed.get(), 0.75f), fade[i]);
        // without the boxes a pressed key is shown by its colour alone
        if (lumen()) { if (!background.on() && fade[i] > 0.02f) R.roundRect(x, y, w, h, 5, R.alpha(pressed.get(), 0.3f * fade[i])); }
        else if (background.on()) R.roundRect(x, y, w, h, 3, bg);
        else if (fade[i] > 0.02f) R.roundRect(x, y, w, h, 3, R.alpha(pressed.get(), 0.3f * fade[i]));
        int tc = background.on() ? R.mix(text.get(), 0xFF101010, fade[i]) : R.mix(text.get(), pressed.get(), fade[i]);
        if (lumen()) {
            // the same keys as soft tiles with Noctua's letters, centred in each
            if (background.on()) R.roundRect(x, y, w, h, 5, R.mix(0x78000000, R.alpha(pressed.get(), 0.85f), fade[i]));
            if (label == null) R.roundRect(x + w / 2 - 10, y + h / 2 - 1, 20, 2, 1, tc);
            else if (sub == null) Fnt.centerB(label, x + w / 2f, mid(y, h, 9), 9, tc);
            else { Fnt.centerB(label, x + w / 2f, cap(y + 4.3f, 8), 8, tc); Fnt.center(sub, x + w / 2f, cap(y + 13.9f, 5), 5, tc); }
            return;
        }
        if (label != null) {
            float ty = sub == null ? y + (h - 8) / 2f : y + 3;
            R.mcText(label, x + (w - mc.fontRendererObj.getStringWidth(label)) / 2f, ty, tc, false);
        } else {
            R.rect(x + w / 2 - 10, y + h / 2 - 0.5f, 20, 1, tc);
        }
        if (sub != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + w / 2f, y + 13, 0);
            GlStateManager.scale(0.5f, 0.5f, 1);
            R.mcText(sub, -mc.fontRendererObj.getStringWidth(sub) / 2f, 0, tc, false);
            GlStateManager.popMatrix();
        }
    }

    public void draw(boolean editing) {
        boolean live = mc.currentScreen == null;
        key(0, name(mc.gameSettings.keyBindForward), null, live && down(mc.gameSettings.keyBindForward), 24, 0, 22, 22);
        key(1, name(mc.gameSettings.keyBindLeft), null, live && down(mc.gameSettings.keyBindLeft), 0, 24, 22, 22);
        key(2, name(mc.gameSettings.keyBindBack), null, live && down(mc.gameSettings.keyBindBack), 24, 24, 22, 22);
        key(3, name(mc.gameSettings.keyBindRight), null, live && down(mc.gameSettings.keyBindRight), 48, 24, 22, 22);
        float y = 48;
        if (mouse.on()) {
            key(4, "LMB", showCps.on() ? Clicks.left() + " CPS" : null, live && down(mc.gameSettings.keyBindAttack), 0, y, 34, 22);
            key(5, "RMB", showCps.on() ? Clicks.right() + " CPS" : null, live && down(mc.gameSettings.keyBindUseItem), 36, y, 34, 22);
            y += 24;
        }
        if (space.on()) key(6, null, null, live && down(mc.gameSettings.keyBindJump), 0, y, 70, 12);
    }

    private static String name(KeyBinding k) {
        String n = k.getKeyCode() > 0 ? Keyboard.getKeyName(k.getKeyCode()) : "?";
        return n == null ? "?" : (n.length() > 3 ? n.substring(0, 3) : n);
    }
}
