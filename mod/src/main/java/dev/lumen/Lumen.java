package dev.lumen;

import dev.lumen.core.Config;
import dev.lumen.core.HudModule;
import dev.lumen.core.Module;
import dev.lumen.core.ModuleManager;
import dev.lumen.cosmetics.CapeLayer;
import dev.lumen.cosmetics.WingsLayer;
import dev.lumen.gui.HudEditorScreen;
import dev.lumen.gui.ModMenuScreen;
import dev.lumen.util.Clicks;
import dev.lumen.util.R;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = Lumen.MODID, name = Lumen.NAME, version = Lumen.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class Lumen {
    public static final String MODID = "lumen", NAME = "Noctua Client", VERSION = "2.7.6";

    public static ModuleManager modules;

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** The Lumen icon on the game window and task bar. */
    private static void windowIcon() {
        try {
            java.nio.ByteBuffer[] icons = new java.nio.ByteBuffer[2];
            String[] files = {"lumen-16.png", "lumen-32.png"};
            for (int i = 0; i < files.length; i++) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(Lumen.class.getResourceAsStream("/assets/lumen/icon/" + files[i]));
                int w = img.getWidth(), h = img.getHeight();
                int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
                java.nio.ByteBuffer b = java.nio.ByteBuffer.allocateDirect(w * h * 4);
                for (int p : argb) b.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
                b.flip();
                icons[i] = b;
            }
            org.lwjgl.opengl.Display.setIcon(icons);
        } catch (Throwable ignored) {}
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        modules = new ModuleManager();
        dev.lumen.online.Online.start();
        windowIcon();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Clicks());
        MinecraftForge.EVENT_BUS.register(new dev.lumen.core.Shaders());
        Config.load();
        Config.save(); // make sure the file exists even if the game is closed abruptly
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() { Config.save(); }
        }, "Noctua-ConfigSave"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new dev.lumen.mods.Screenshots.Command());
        try {
            for (RenderPlayer rp : mc.getRenderManager().getSkinMap().values()) {
                rp.addLayer(new CapeLayer(rp));
                rp.addLayer(new WingsLayer(rp));
                rp.addLayer(new dev.lumen.cosmetics.HatLayer(rp));
                rp.addLayer(new dev.lumen.cosmetics.ShieldLayer(rp));
            }
        } catch (Throwable t) {
            System.err.println("[Noctua] Cosmetics unavailable: " + t);
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        // Keys are set in the menu's Settings tab (dev.lumen.core.Client), not in the game's Controls screen.
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent() || mc.currentScreen != null) return;
        int key = Keyboard.getEventKey();
        if (dev.lumen.core.Client.keyMenu.is(key)) mc.displayGuiScreen(new ModMenuScreen());
        else if (dev.lumen.core.Client.keyQuickPlay.is(key) && dev.lumen.mods.QuickPlay.INSTANCE != null && dev.lumen.mods.QuickPlay.INSTANCE.isEnabled()) dev.lumen.mods.QuickPlay.INSTANCE.open();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.gameSettings.showDebugInfo || mc.currentScreen instanceof HudEditorScreen || mc.currentScreen instanceof dev.lumen.gui.ModMenuScreen) return; // the menu is see-through: no HUD under it
        for (Module m : modules.all) {
            if (m.isEnabled() && m instanceof HudModule) ((HudModule) m).render(e.resolution, false);
        }
        R.reset();
    }
}
