package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Clock extends TextHud {
    private final Setting.Bool h24 = bool("24 Hour", false);
    private static final DateTimeFormatter F12 = DateTimeFormatter.ofPattern("h:mm a"), F24 = DateTimeFormatter.ofPattern("HH:mm");
    public Clock() { super("Clock", "Shows the real-world time.", CENTER, -28, START, 4); }
    protected String text(boolean editing) { return LocalTime.now().format(h24.on() ? F24 : F12); }
}
