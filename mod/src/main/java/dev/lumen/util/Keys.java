package dev.lumen.util;

import org.lwjgl.input.Keyboard;

/** Key names that mean the same key in every Lumen build, so key settings can be shared between game versions. */
public final class Keys {
    private static final String[][] ALIAS = {{"RMENU", "RALT"}, {"LMENU", "LALT"}, {"CAPITAL", "CAPSLOCK"}, {"PRIOR", "PAGEUP"}, {"NEXT", "PAGEDOWN"}, {"BACK", "BACKSPACE"}};

    private Keys() {}

    /** Shared name of a key code, or "NONE". */
    public static String name(int code) {
        if (code <= 0) return "NONE";
        String n = Keyboard.getKeyName(code);
        if (n == null) return "NONE";
        for (String[] a : ALIAS) if (a[0].equals(n)) return a[1];
        return n;
    }

    /** Key code for a shared name; -1 when it is unset or this version has no such key. */
    public static int code(String name) {
        if (name == null || name.equals("NONE")) return -1;
        for (String[] a : ALIAS) if (a[1].equals(name)) name = a[0];
        int c = Keyboard.getKeyIndex(name);
        return c <= 0 ? -1 : c;
    }
}
