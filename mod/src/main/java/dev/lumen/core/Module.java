package dev.lumen.core;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public final String name, description;
    public final Category category;
    /** Not listed in the mod grid (it is controlled from the Settings tab instead). */
    public boolean hidden;
    public final List<Setting> settings = new ArrayList<Setting>();
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected Setting.Bool bool(String n, boolean d) { Setting.Bool s = new Setting.Bool(n, d); settings.add(s); return s; }
    protected Setting.Num num(String n, double d, double min, double max, double step) { Setting.Num s = new Setting.Num(n, d, min, max, step); settings.add(s); return s; }
    protected Setting.Mode mode(String n, String d, String... o) { Setting.Mode s = new Setting.Mode(n, d, o); settings.add(s); return s; }
    protected Setting.Text text(String n, String d, int max) { Setting.Text s = new Setting.Text(n, d, max); settings.add(s); return s; }
    protected Setting.Col col(String n, int d) { Setting.Col s = new Setting.Col(n, d); settings.add(s); return s; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        try {
            if (e) { MinecraftForge.EVENT_BUS.register(this); onEnable(); }
            else { MinecraftForge.EVENT_BUS.unregister(this); onDisable(); }
        } catch (Throwable t) {
            System.err.println("[Noctua] Failed to toggle " + name + ": " + t);
        }
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}
}
