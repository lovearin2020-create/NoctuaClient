package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.client.KeyMapping;

public class Keystrokes extends HudModule {
    private final Setting.Bool mouse = bool("Mouse Buttons", true);
    private final Setting.Bool cps = bool("Show CPS", true);
    private final Setting.Bool space = bool("Space Bar", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private final Setting.Col pressed = col("Pressed Color", 0xFFFFFFFF);
    private final float[] fade = new float[7];

    private final Setting.Bool background = bool("Background", true);
    public Keystrokes() { super("Keystrokes", "Shows your movement keys and mouse buttons.", START, 4, START, 66); }

    protected boolean hasLumenStyle() { return false; }
    public float width() { return 62; }
    public float height() { return 40 + (mouse.on() ? 20 : 0) + (space.on() ? 12 : 0) + 2; }

    private void key(int i, KeyMapping k, String label, String sub, float x, float y, float w, float h, boolean editing) {
        boolean down = !editing && k.isDown();
        fade[i] = Draw.lerp(fade[i], down ? 1 : 0, 0.35f);
        // without the boxes a pressed key is shown by its colour alone
        if (background.on()) Draw.roundRect(x, y, w, h, 3, Draw.mix(0x70000000, Draw.alpha(pressed.get(), 0.75f), fade[i]));
        else if (fade[i] > 0.02f) Draw.roundRect(x, y, w, h, 3, Draw.alpha(pressed.get(), 0.3f * fade[i]));
        int c = background.on() ? Draw.mix(color.get(), 0xFF000000, fade[i]) : Draw.mix(color.get(), pressed.get(), fade[i]);
        if (label == null) { Draw.rect(x + w / 2 - 12, y + h / 2 - 1, 24, 2, c); return; }
        if (sub == null) Draw.text(label, x + (w - mc.font.width(label)) / 2f, y + (h - 8) / 2f, c, false);
        else {
            Draw.text(label, x + (w - mc.font.width(label)) / 2f, y + 2, c, false);
            Draw.push(x + w / 2f, y + 11, 0.6f);
            Draw.text(sub, -mc.font.width(sub) / 2f, 0, c, false);
            Draw.pop();
        }
    }

    public void draw(boolean editing) {
        var o = mc.options;
        key(0, o.keyUp, "W", null, 21, 0, 20, 19, editing);
        key(1, o.keyLeft, "A", null, 0, 20, 20, 19, editing);
        key(2, o.keyDown, "S", null, 21, 20, 20, 19, editing);
        key(3, o.keyRight, "D", null, 42, 20, 20, 19, editing);
        float y = 40;
        if (mouse.on()) {
            key(4, o.keyAttack, "LMB", cps.on() ? Clicks.left() + " CPS" : null, 0, y, 30.5f, 19, editing);
            key(5, o.keyUse, "RMB", cps.on() ? Clicks.right() + " CPS" : null, 31.5f, y, 30.5f, 19, editing);
            y += 20;
        }
        if (space.on()) key(6, o.keyJump, null, null, 0, y, 62, 11, editing);
    }
}
