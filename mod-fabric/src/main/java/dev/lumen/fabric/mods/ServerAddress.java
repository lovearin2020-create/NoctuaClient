package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class ServerAddress extends TextHud {
    public ServerAddress() { super("Server Address", "Shows the server you are playing on.", CENTER, -40, START, 44); }
    protected String text(boolean editing) {
        try { if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip; } catch (Throwable ignored) {}
        return "Singleplayer";
    }
}
