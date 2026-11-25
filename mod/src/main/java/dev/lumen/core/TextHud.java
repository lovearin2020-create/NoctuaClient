package dev.lumen.core;

import dev.lumen.util.Fnt;
import dev.lumen.util.R;

/** Lunar-style single line text box, e.g. "120 FPS". */
public abstract class TextHud extends HudModule {
    protected final Setting.Bool background = bool("Background", true);
    protected final Setting.Bool shadow = bool("Text Shadow", true);
    protected final Setting.Col color = col("Text Color", 0xFFFFFFFF);

    protected TextHud(String name, String description, int ax, float ox, int ay, float oy) { super(name, description, ax, ox, ay, oy); }

    /** Text to show, or null to hide. */
    protected abstract String text(boolean editing);
    protected int minWidth() { return 56; }

    private String current = "";

    public float width() {
        if (lumen()) return background.on() ? Math.max(minWidth(), Fnt.width(current, 8) + 16) : Fnt.width(bracket(current), 8);
        return background.on() ? Math.max(minWidth(), mc.fontRendererObj.getStringWidth(current) + 10) : mc.fontRendererObj.getStringWidth(bracket(current));
    }
    /** Text that already comes in brackets keeps the ones it has. */
    private static String bracket(String s) { return s.startsWith("[") && s.endsWith("]") ? s : "[" + s + "]"; }
    public float height() { return background.on() ? 16 : 9; }

    public void draw(boolean editing) {
        String s = text(editing);
        if (s == null) { current = ""; return; }
        current = s;
        float w = width();
        if (lumen()) {
            if (background.on()) { panel(0, 0, w, 16); Fnt.center(s, w / 2f, 4.5f, 8, color.get()); }
            else Fnt.draw(bracket(s), 0, 0.5f, 8, color.get());
            return;
        }
        if (background.on()) {
            R.roundRect(0, 0, w, 16, 3, 0x70000000);
            R.mcText(s, Math.round((w - mc.fontRendererObj.getStringWidth(s)) / 2f), 4, color.get(), shadow.on());
        } else {
            // no box: the text gets brackets instead, e.g. [120 FPS]
            R.mcText(bracket(s), 0, 0, color.get(), shadow.on());
        }
    }
}
