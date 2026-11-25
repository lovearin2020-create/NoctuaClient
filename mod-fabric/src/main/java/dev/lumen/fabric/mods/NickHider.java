package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

/** Replaces your own name with another one on your screen only. Nothing is sent to the server. */
public class NickHider extends Module {
    private final Setting.Text replacement = text("Shown Name", "You", 16);
    private String cachedName;
    private long checked;

    public NickHider() { super("Nick Hider", "Hides your name on your own screen (streams, clips).", Category.VISUAL); }

    private String name() {
        long now = System.currentTimeMillis();
        if (now - checked > 1000) {
            checked = now;
            try { cachedName = mc.getUser().getName(); } catch (Throwable t) { cachedName = null; }
        }
        return cachedName;
    }

    public String apply(String s) {
        if (s == null || s.length() < 3) return s;
        String n = name();
        if (n == null || n.length() < 3 || replacement.value.isEmpty() || s.indexOf(n) < 0) return s;
        return s.replace(n, replacement.value);
    }
}
