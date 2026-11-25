package dev.lumen.mods;

import dev.lumen.core.TextHud;

import static dev.lumen.core.HudModule.START;
import dev.lumen.util.Clicks;
import net.minecraft.client.Minecraft;

import java.text.SimpleDateFormat;
import java.util.Date;

/** One-line text HUD elements. */
public final class SimpleHuds {
    public static TextHud fps() {
        return new TextHud("FPS", "Frames per second.", START, 4, START, 4) {
            protected String text(boolean e) { return Minecraft.getDebugFPS() + " FPS"; }
        };
    }

    public static TextHud cps() {
        return new TextHud("CPS", "Left and right clicks per second.", START, 4, START, 22) {
            protected String text(boolean e) { return Clicks.left() + " | " + Clicks.right() + " CPS"; }
        };
    }

    public static TextHud clock() {
        final SimpleDateFormat fmt = new SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH); // AM/PM in letters Noctua's own font has, whatever the system language
        return new TextHud("Clock", "Your local time.", START, 4, START, 98) {
            protected String text(boolean e) { return fmt.format(new Date()); }
        };
    }

    public static TextHud server() {
        return new TextHud("Server Address", "The server you are on.", START, 4, START, 116) {
            protected String text(boolean e) {
                if (mc.getCurrentServerData() == null) return e ? "Singleplayer" : null;
                return mc.getCurrentServerData().serverIP;
            }
        };
    }

    public static TextHud speed() {
        return new TextHud("Speedometer", "Horizontal speed in blocks per second.", START, 4, START, 134) {
            protected String text(boolean e) {
                if (mc.thePlayer == null) return "0.00 m/s";
                double dx = mc.thePlayer.posX - mc.thePlayer.prevPosX, dz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                return String.format("%.2f m/s", Math.sqrt(dx * dx + dz * dz) * 20.0);
            }
        };
    }

    public static TextHud memory() {
        return new TextHud("Memory", "Memory used by the game.", START, 4, START, 152) {
            protected String text(boolean e) {
                Runtime r = Runtime.getRuntime();
                long used = r.totalMemory() - r.freeMemory();
                return "Mem: " + (used * 100L / r.maxMemory()) + "%";
            }
        };
    }
}
