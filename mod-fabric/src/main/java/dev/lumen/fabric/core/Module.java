package dev.lumen.fabric.core;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final String name, description;
    public final Category category;
    /** Not listed in the mod grid (it is controlled from the Settings tab instead). */
    public boolean hidden;
    public final List<Setting> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected Setting.Bool bool(String n, boolean d) { Setting.Bool s = new Setting.Bool(n, d); settings.add(s); return s; }
    protected Setting.Num num(String n, double d, double min, double max, double step) { Setting.Num s = new Setting.Num(n, d, min, max, step); settings.add(s); return s; }
    protected Setting.Mode mode(String n, String d, String... o) { Setting.Mode s = new Setting.Mode(n, d, o); settings.add(s); return s; }
    protected Setting.Col col(String n, int d) { Setting.Col s = new Setting.Col(n, d); settings.add(s); return s; }
    protected Setting.Text text(String n, String d, int max) { Setting.Text s = new Setting.Text(n, d, max); settings.add(s); return s; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        try { if (e) onEnable(); else onDisable(); } catch (Throwable t) { System.err.println("[Noctua] Failed to toggle " + name + ": " + t); }
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}

    // Events, delivered only while the module is enabled.
    public void onTick() {}
    public void onAttack(Entity target) {}
    /** @return true to hide the message. */
    public boolean onChat(String plainText) { return false; }
    /** Key pressed (action 1) or released (action 0). @return true to keep the event from the game. */
    public boolean onKey(net.minecraft.client.input.KeyEvent event, int action) { return false; }
    /** Called once per rendered frame while in a world; world-space gizmos may be added here. */
    public void onFrame(float partialTick) {}
}
