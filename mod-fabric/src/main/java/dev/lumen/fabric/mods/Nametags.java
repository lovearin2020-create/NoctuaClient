package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Nametags extends Module {
    private final Setting.Text custom = text("Custom Text (& = color code)", "", 32);

    public Nametags() { super("Own Nametag", "Your nametag in third person, with optional custom text.", Category.VISUAL); }

    /** Replacement for your own nametag, or null to keep the real one. Chat and tab are not affected. */
    public String replacement() {
        String s = custom.value.trim();
        if (s.isEmpty()) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(s.charAt(i + 1)) >= 0) b.append('§'); else b.append(c);
        }
        return b.toString();
    }
}
