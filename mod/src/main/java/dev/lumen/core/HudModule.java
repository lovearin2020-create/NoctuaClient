package dev.lumen.core;

import dev.lumen.util.R;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

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
        if (hasLumenStyle()) { lumenHud = new Setting.Bool("Noctua HUD", true); settings.add(lumenHud); }
    }

    /** The switch between the Lumen look (soft dark panel, Lumen's own letters) and the classic one. */
    private Setting.Bool lumenHud;
    /** True while the element paints the Noctua panel itself, so the optional one behind it is left out. */
    protected boolean ownPanel() { return false; }
    /** Whether the optional background is wanted (always, for elements without that option). */
    protected boolean panelOn() { return panel == null || panel.on(); }
    /** Elements that only have one look say no. */
    protected boolean hasLumenStyle() { return true; }
    protected boolean lumen() { return lumenHud != null && lumenHud.on(); }
    /** The Lumen panel: the one Direction HUD and Stats Overlay sit on. */
    protected static void panel(float x, float y, float w, float h) {
        R.roundRect(x, y, w, h, 6, 0x78000000);
        R.gradient(x + 4, y + 1, w - 8, Math.min(9, h / 2), 0x14FFFFFF, 0x00FFFFFF);
    }
    /** The y that puts a line of Noctua's letters (their capitals and digits) in the middle of a box. */
    protected static float mid(float top, float h, float size) { return top + h / 2f - 0.68f * size; }
    /** The y that puts the top of the capitals at this height. */
    protected static float cap(float inkTop, float size) { return inkTop - 0.31f * size; }
    /** The box behind an element, in whichever look is chosen. */
    protected void box(float x, float y, float w, float h) { if (lumen()) panel(x, y, w, h); else R.roundRect(x, y, w, h, 3, 0x70000000); }

    /** Set by elements that had no box of their own: an optional panel behind them. */
    private Setting.Bool panel;
    protected void optionalBackground() { panel = bool("Background", false); }
    /** Whether the element is drawing anything right now (the panel is hidden with it). */
    protected boolean showing(boolean editing) { return true; }

    /** Unscaled size of the element. */
    public abstract float width();
    public abstract float height();

    /** Draw at the origin. {@code editing} is true inside the HUD editor (show placeholder data). */
    public abstract void draw(boolean editing);

    private static float resolve(int anchor, float off, float screen, float size) {
        float v = anchor == START ? off : anchor == CENTER ? screen / 2f + off : screen - off - size;
        return Math.max(0, Math.min(Math.max(0, screen - size), v));
    }

    public float px(ScaledResolution sr) { return resolve(ax, ox, sr.getScaledWidth(), width() * scale); }
    public float py(ScaledResolution sr) { return resolve(ay, oy, sr.getScaledHeight(), height() * scale); }

    /** Moves the element so its top-left corner is at (x, y), re-attaching it to the nearest edge. */
    public void moveTo(ScaledResolution sr, float x, float y) {
        float sw = sr.getScaledWidth(), sh = sr.getScaledHeight(), w = width() * scale, h = height() * scale;
        float cx = x + w / 2f, cy = y + h / 2f;
        ax = cx < sw / 3f ? START : cx > sw * 2f / 3f ? END : CENTER;
        ay = cy < sh / 3f ? START : cy > sh * 2f / 3f ? END : CENTER;
        ox = ax == START ? x : ax == CENTER ? x - sw / 2f : sw - x - w;
        oy = ay == START ? y : ay == CENTER ? y - sh / 2f : sh - y - h;
    }

    public void render(ScaledResolution sr, boolean editing) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(Math.round(px(sr)), Math.round(py(sr)), 0);
        GlStateManager.scale(scale, scale, 1);
        if (panel != null && panel.on() && showing(editing) && !ownPanel()) box(-3, -3, width() + 6, height() + 6);
        try { draw(editing); } catch (Throwable ignored) {}
        GlStateManager.popMatrix();
        R.reset();
    }
}
