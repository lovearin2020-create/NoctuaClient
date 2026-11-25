package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Cps extends TextHud {
    private final Setting.Bool right = bool("Show Right Click", true);
    public Cps() { super("CPS", "Shows your clicks per second.", START, 64, START, 4); }
    protected String text(boolean editing) { return right.on() ? Clicks.left() + " | " + Clicks.right() + " CPS" : Clicks.left() + " CPS"; }
}
