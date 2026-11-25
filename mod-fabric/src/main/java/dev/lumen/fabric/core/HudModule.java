package dev.lumen.fabric.core;

import dev.lumen.fabric.util.Draw;

/** A module that draws a movable, scalable element on the in-game HUD. */
public abstract class HudModule extends Module {
    public static final int START = 0, CENTER = 1, END = 2;

    /** Which screen edge (or the centre) the element is attached to, and its pixel offset from it. */
    public int ax, ay;
    public float ox, oy;
    public float scale = 1f;

    protected HudModule(String name, String description, int ax, float ox, int ay, float oy) { this(name, description, Category.HUD, ax, ox, ay, oy); }

    /** For HUD elements that belong in another tab of the menu (e.g. Hypixel). */
    protected HudModule(String name, String description, Category category, int ax, float ox, int ay, float oy) {
        super(name, description, category);
        this.ax = ax; this.ox = ox; this.ay = ay; this.oy = oy;
    }

    /** Set by elements that had no box of their own: an optional panel behind them. */
    private Setting.Bool panel;
    protected void optionalBackground() { panel = bool("Background", false); }
    /** Whether the element is drawing anything right now (the panel is hidden with it). */
    protected boolean showing(boolean editing) { return true; }

    public abstract float width();
    public abstract float height();
    /** Draw at the origin. {@code editing} is true inside the HUD editor (show placeholder data). */
    public abstract void draw(boolean editing);

    private static float resolve(int anchor, float off, float screen, float size) {
        float v = anchor == START ? off : anchor == CENTER ? screen / 2f + off : screen - off - size;
        return Math.max(0, Math.min(Math.max(0, screen - size), v));
    }

    public float px(int sw) { return resolve(ax, ox, sw, width() * scale); }
    public float py(int sh) { return resolve(ay, oy, sh, height() * scale); }

    /** Moves the element so its top-left corner is at (x, y), re-attaching it to the nearest edge. */
    public void moveTo(int sw, int sh, float x, float y) {
        float w = width() * scale, h = height() * scale, cx = x + w / 2f, cy = y + h / 2f;
        ax = cx < sw / 3f ? START : cx > sw * 2f / 3f ? END : CENTER;
        ay = cy < sh / 3f ? START : cy > sh * 2f / 3f ? END : CENTER;
        ox = ax == START ? x : ax == CENTER ? x - sw / 2f : sw - x - w;
        oy = ay == START ? y : ay == CENTER ? y - sh / 2f : sh - y - h;
    }

    public void render(int sw, int sh, boolean editing) {
        Draw.push(Math.round(px(sw)), Math.round(py(sh)), scale);
        if (panel != null && panel.on() && showing(editing)) Draw.roundRect(-3, -3, width() + 6, height() + 6, 3, 0x70000000);
        try { draw(editing); } catch (Throwable ignored) {}
        Draw.pop();
    }
}
