package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Fps extends TextHud {
    public Fps() { super("FPS", "Shows your frames per second.", START, 4, START, 4); }
    protected String text(boolean editing) { return mc.getFps() + " FPS"; }
}
