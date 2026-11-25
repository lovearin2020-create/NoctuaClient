package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

/** Dropped items tumble while falling and lie flat on the ground instead of hovering and spinning. */
public class ItemPhysics extends Module {
    public final Setting.Num speed = num("Tumble Speed", 1.0, 0.2, 3.0, 0.1);
    public ItemPhysics() { super("Item Physics", "Dropped items lie flat on the ground.", Category.VISUAL); }
}
