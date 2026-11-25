package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.client.input.KeyEvent;

public class ToggleSneak extends TextHud {
    private final Setting.Bool showText = bool("Show Status Text", true);
    private boolean toggled;

    public ToggleSneak() { super("Toggle Sneak", "Tap sneak to stay sneaking.", START, 4, END, 22); }

    public boolean onKey(KeyEvent e, int action) {
        if (mc.gui.screen() != null || mc.player == null || !mc.options.keyShift.matches(e)) return false;
        if (action == 1) { toggled = !toggled; return false; }
        // While toggled, swallow the release so the game keeps treating the key as held.
        return action == 0 && toggled;
    }

    public void onTick() {
        if (toggled && mc.gui.screen() == null) ToggleSprint.hold(mc.options.keyShift, true);
    }

    protected void onDisable() {
        if (toggled) try { ToggleSprint.hold(mc.options.keyShift, false); } catch (Throwable ignored) {}
        toggled = false;
    }

    protected int minWidth() { return 0; }
    protected String text(boolean editing) {
        if (editing) return "[Sneaking (Toggled)]";
        return toggled && showText.on() ? "[Sneaking (Toggled)]" : null;
    }
}
