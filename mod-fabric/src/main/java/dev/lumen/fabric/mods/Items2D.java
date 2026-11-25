package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

/** Draws every dropped stack as a single copy, with flat items facing you: fewer models in item-heavy fights. */
public class Items2D extends Module {
    public Items2D() { super("2D Items", "Dropped items as single flat sprites (faster).", Category.PERFORMANCE); }
}
