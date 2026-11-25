package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

public class TabEditor extends Module {
    private final Setting.Bool numbers = bool("Ping As Numbers", true);
    public final Setting.Bool hideHeader = bool("Hide Header", false);
    public final Setting.Bool hideFooter = bool("Hide Footer", false);

    public TabEditor() { super("Tab Editor", "Ping numbers and cleaner tab list.", Category.HUD); }

    /** @return true if the vanilla ping bars should be skipped. */
    public boolean drawPing(GuiGraphicsExtractor g, int slotWidth, int x, int y, PlayerInfo info) {
        if (!numbers.on() || info == null) return false;
        int ping = info.getLatency();
        String s = String.valueOf(ping);
        int color = ping < 0 ? 0xFFAAAAAA : ping < 80 ? 0xFF55FF55 : ping < 150 ? 0xFFFFFF55 : ping < 250 ? 0xFFFFAA00 : 0xFFFF5555;
        g.pose().pushMatrix();
        g.pose().translate(x + slotWidth - 1 - mc.font.width(s) * 0.5f, y + 2);
        g.pose().scale(0.5f, 0.5f);
        g.text(mc.font, s, 0, 0, color, true);
        g.pose().popMatrix();
        return true;
    }
}
