package dev.lumen.mods;

import dev.lumen.Lumen;
import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Util;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Zoom extends Module {
    private final Setting.Num factor = num("Zoom Factor", 4, 2, 10, 0.5);
    private final Setting.Bool smooth = bool("Smooth Camera", true);
    private final Setting.Bool animate = bool("Smooth Transition", true);
    private final Setting.Bool scroll = bool("Scroll To Adjust", true);
    private boolean zooming;
    private float savedFov, current;
    private boolean savedSmooth;
    private double liveFactor;

    public Zoom() { super("Zoom", "Hold the zoom key to zoom (set the key in Settings).", Category.VISUAL); }

    private boolean keyDown() {
        int c = dev.lumen.core.Client.keyZoom.code();
        return c > 0 && mc.currentScreen == null && Keyboard.isKeyDown(c);
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || mc.thePlayer == null) return;
        boolean down = keyDown();
        if (down && !zooming) {
            zooming = true;
            savedFov = mc.gameSettings.fovSetting;
            savedSmooth = mc.gameSettings.smoothCamera;
            current = savedFov;
            liveFactor = factor.value;
            if (smooth.on()) mc.gameSettings.smoothCamera = true;
        }
        if (!zooming) return;
        float target = down ? (float) (savedFov / liveFactor) : savedFov;
        current = animate.on() ? Util.lerp(current, target, 0.25f) : target;
        if (!down && Math.abs(current - savedFov) < 0.5f) { restore(); return; }
        mc.gameSettings.fovSetting = current;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!zooming || !scroll.on() || e.dwheel == 0) return;
        liveFactor = Math.max(1.5, Math.min(20, liveFactor + (e.dwheel > 0 ? 1 : -1)));
        e.setCanceled(true);
    }

    private void restore() {
        if (!zooming) return;
        zooming = false;
        mc.gameSettings.fovSetting = savedFov;
        mc.gameSettings.smoothCamera = savedSmooth;
    }

    protected void onDisable() { restore(); }
}
