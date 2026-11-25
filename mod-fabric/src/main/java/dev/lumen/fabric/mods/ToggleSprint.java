package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class ToggleSprint extends TextHud {
    private final Setting.Bool show = bool("Show Status Text", true);
    public ToggleSprint() { super("Toggle Sprint", "Keeps you sprinting without holding the key.", START, 4, END, 4); }
    public void onTick() { if (mc.gui.screen() == null) hold(mc.options.keySprint, true); }
    protected void onDisable() { try { hold(mc.options.keySprint, false); } catch (Throwable ignored) {} }

    /**
     * Puts a movement key in the wanted state and leaves it alone once it is there.
     * The sprint and sneak keys are "toggle" keys: with the game's own Sprint/Sneak option set to Toggle,
     * setDown(true) does not press the key, it flips it. Calling that every tick switched sprinting on and
     * off twenty times a second, which a server sees as a sprint-reset cheat. So: only act when the state is
     * wrong, and if a plain set did not take (toggle mode), flip once.
     */
    static void hold(net.minecraft.client.KeyMapping key, boolean want) {
        if (key.isDown() == want) return;
        key.setDown(want);
        if (key.isDown() != want) key.setDown(true);
    }
    protected int minWidth() { return 0; }
    protected String text(boolean editing) { return show.on() ? "[Sprinting (Toggled)]" : null; }
}
