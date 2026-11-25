package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.ReloadAccess;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces the game's glyph sheet (chat, HUD, menus) with a PvP pack font's, without the pack. It is done where the
 * game looks its files up, so anything that reads the sheet (the game itself, OptiFine, Patcher's own text drawing)
 * gets the same picture and works out the same letter widths from it.
 */
public class GameFont extends Module {
    public static GameFont INSTANCE;
    private final Setting.Mode font = mode("Font", "Circle", "Circle", "Smooth");

    /** The sheet being served right now (null: the game's own). */
    private volatile String applied;
    private static final Set<ResourceLocation> SEEN = Collections.newSetFromMap(new ConcurrentHashMap<ResourceLocation, Boolean>());

    public GameFont() { super("Game Font", "PvP pack letters for chat and HUD.", Category.VISUAL); INSTANCE = this; }

    /** Called for every file the game opens: our sheet in place of any pack's ascii.png (HD variants included). */
    public static IResource resource(ResourceLocation loc) {
        GameFont g = INSTANCE;
        String name = g == null ? null : g.applied;
        if (name == null || loc == null || !"minecraft".equals(loc.getResourceDomain()) || !loc.getResourcePath().endsWith("font/ascii.png")) return null;
        InputStream in = GameFont.class.getResourceAsStream("/assets/lumen/fonts/pack/" + name + ".png");
        if (in == null) return null;
        SEEN.add(loc);
        return new SimpleResource("Noctua", loc, in, null, null);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.fontRendererObj == null || !isEnabled()) return;
        String want = font.get().toLowerCase();
        if (!want.equals(applied)) { applied = want; reload(); }
    }

    protected void onDisable() { if (applied != null) { applied = null; reload(); } }

    public static void beforeReload(FontRenderer fr) {}

    /** Makes everything that keeps a copy of the sheet read it again. */
    private void reload() {
        try {
            if (mc.fontRendererObj == null) return;
            IResourceManager rm = mc.getResourceManager();
            SEEN.add(new ResourceLocation("textures/font/ascii.png"));
            for (ResourceLocation l : SEEN) mc.getTextureManager().loadTexture(l, new SimpleTexture(l)); // read again, in place of the copy on the graphics card
            mc.fontRendererObj.onResourceManagerReload(rm);
            if (rm instanceof ReloadAccess) {
                for (Object o : new ArrayList<Object>(((ReloadAccess) rm).lumen$listeners())) {
                    String n = o.getClass().getName();
                    if (n.startsWith("net.minecraft") || n.startsWith("dev.lumen") || !(o instanceof IResourceManagerReloadListener)) continue;
                    try { ((IResourceManagerReloadListener) o).onResourceManagerReload(rm); } catch (Throwable ignored) {} // other mods' text drawing
                }
            }
        } catch (Throwable t) {
            System.err.println("[Noctua] Could not switch the game font: " + t);
        }
    }
}
