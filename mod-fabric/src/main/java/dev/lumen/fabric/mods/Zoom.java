package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import dev.lumen.fabric.Hooks;

public class Zoom extends Module {
    private final Setting.Num factor = num("Zoom Factor", 4, 2, 10, 0.5);
    private final Setting.Bool smooth = bool("Smooth Transition", true);
    private float progress;
    private long last = System.nanoTime();

    public Zoom() { super("Zoom", "Hold the zoom key to zoom (set the key in Settings).", Category.VISUAL); }

    public float apply(float fov) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        boolean held = isEnabled() && mc.gui.screen() == null && Client.keyZoom.code() > 0 && Hooks.keyDown(Client.keyZoom.code());
        float target = held ? 1f : 0f;
        if (!smooth.on()) progress = target;
        else progress += (target - progress) * Math.min(1f, dt * 14f);
        if (progress < 0.002f) { progress = held ? progress : 0f; if (!held) return fov; }
        float eased = progress * progress * (3f - 2f * progress);
        return fov / (1f + (factor.f() - 1f) * eased);
    }
}
