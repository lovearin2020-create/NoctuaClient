package dev.lumen.fabric.util;

import net.minecraft.client.Minecraft;

public final class Servers {
    private static final boolean FAKE_HYPIXEL = Boolean.getBoolean("lumen.fakeHypixel"); // for testing offline

    public static boolean onHypixel() {
        if (FAKE_HYPIXEL) return true;
        try {
            var s = Minecraft.getInstance().getCurrentServer();
            return s != null && s.ip != null && s.ip.toLowerCase().contains("hypixel.net");
        } catch (Throwable t) { return false; }
    }

    /** Removes legacy colour codes. */
    public static String strip(String s) { return s == null ? "" : s.replaceAll("§.", ""); }
}
