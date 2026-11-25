package dev.lumen.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.EnumChatFormatting;

public final class Util {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final boolean FAKE_HYPIXEL = Boolean.getBoolean("lumen.fakeHypixel"); // for testing offline

    public static boolean onHypixel() {
        if (FAKE_HYPIXEL) return true;
        ServerData d = mc.getCurrentServerData();
        return d != null && d.serverIP != null && d.serverIP.toLowerCase().contains("hypixel");
    }

    public static String strip(String s) {
        String r = EnumChatFormatting.getTextWithoutFormattingCodes(s);
        return r == null ? "" : r;
    }

    public static boolean inGame() { return mc.thePlayer != null && mc.theWorld != null; }

    public static float lerp(float cur, float target, float speed) {
        float d = target - cur;
        if (Math.abs(d) < 0.001f) return target;
        return cur + d * Math.min(1f, speed);
    }
}
