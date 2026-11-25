package dev.lumen.fabric.core;

import dev.lumen.fabric.util.Draw;

/** Single line text box, e.g. "120 FPS". */
public abstract class TextHud extends HudModule {
    protected final Setting.Bool background = bool("Background", true);
    protected final Setting.Bool shadow = bool("Text Shadow", true);
    protected final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private String current = "";

    protected TextHud(String name, String description, int ax, float ox, int ay, float oy) { super(name, description, ax, ox, ay, oy); }

    /** Text to show, or null to hide. */
    protected abstract String text(boolean editing);
    protected int minWidth() { return 56; }

    public float width() {
        if (lumen()) return background.on() ? Math.max(minWidth(), Draw.uiWidth(current, 8) + 16) : Draw.uiWidth(bracket(current), 8);
        return background.on() ? Math.max(minWidth(), mc.font.width(current) + 10) : mc.font.width(bracket(current));
    }
    /** Without a box the text gets brackets, e.g. [120 FPS]; text that already has them keeps the ones it has. */
    private static String bracket(String s) { return s.startsWith("[") && s.endsWith("]") ? s : "[" + s + "]"; }
    public float height() { return background.on() ? 16 : 9; }

    public void draw(boolean editing) {
        String s = text(editing);
        if (s == null) { current = ""; return; }
        current = s;
        int w = (int) width();
        if (lumen()) {
            if (background.on()) { panel(0, 0, width(), 16); Draw.uiCenter(s, width() / 2f, mid(0, 16, 8), 8, color.get()); }
            else Draw.ui(bracket(s), 0, 0.5f, 8, color.get());
            return;
        }
        if (background.on()) {
            Draw.roundRect(0, 0, w, 16, 3, 0x70000000);
            Draw.text(s, (w - mc.font.width(s)) / 2, 4, color.get(), shadow.on());
        } else Draw.text(bracket(s), 0, 0, color.get(), shadow.on());
    }
}
