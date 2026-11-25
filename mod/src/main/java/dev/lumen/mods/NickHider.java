package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

/** Replaces your own name with another one on your screen only. Nothing is sent to the server. */
public class NickHider extends Module {
    public static NickHider INSTANCE;
    private final Setting.Text replacement = text("Shown Name", "You", 16);
    private String cachedName;
    private long checked;

    public NickHider() { super("Nick Hider", "Hides your name on your own screen (streams, clips).", Category.VISUAL); INSTANCE = this; }

    private String name() {
        long now = System.currentTimeMillis();
        if (now - checked > 1000) {
            checked = now;
            try { cachedName = mc.getSession() == null ? null : mc.getSession().getUsername(); } catch (Throwable t) { cachedName = null; }
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
