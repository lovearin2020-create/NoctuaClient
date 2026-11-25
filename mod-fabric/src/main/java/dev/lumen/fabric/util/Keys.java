package dev.lumen.fabric.util;

import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/** Key names that mean the same key in every Lumen build, so key settings can be shared between game versions. */
public final class Keys {
    private static final Map<String, Integer> CODES = new HashMap<>();
    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        // The game's own key table: the numbers differ between 26.2 and 26.3, the names do not.
        for (Field f : InputConstants.class.getFields()) {
            if (!f.getName().startsWith("KEY_") || f.getType() != int.class || !Modifier.isStatic(f.getModifiers())) continue;
            try {
                int code = f.getInt(null);
                String name = f.getName().substring(4);
                if (code < 0) continue;
                CODES.put(name, code);
                NAMES.putIfAbsent(code, name);
            } catch (Throwable ignored) {}
        }
    }

    private Keys() {}

    /** Shared name of a key code, or "NONE". */
    public static String name(int code) { return NAMES.getOrDefault(code, "NONE"); }

    /** Key code for a shared name; -1 when it is unset or this version has no such key. */
    public static int code(String name) { return name == null ? -1 : CODES.getOrDefault(name, -1); }
}
