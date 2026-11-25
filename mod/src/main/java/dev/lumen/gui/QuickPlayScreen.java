package dev.lumen.gui;

import dev.lumen.mods.QuickPlay;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class QuickPlayScreen extends GuiScreen {
    private static int tab;
    private static final int W = 300, H = 170;

    private float x0() { return width / 2f - W / 2f; }
    private float y0() { return height / 2f - H / 2f; }

    public void drawScreen(int mx, int my, float pt) {
        Fnt.transform(1f, 0, 0);
        float x = x0(), y = y0();
        R.roundRect(x, y, W, H, 8, 0xF0141720);
        Fnt.drawB("Quick Play", x + 12, y + 10, 12, 0xFFF2F4F8);
        for (int i = 0; i < QuickPlay.GAMES.length; i++) {
            float ty = y + 34 + i * 24;
            boolean h = R.in(mx, my, x + 8, ty, 80, 20);
            R.roundRect(x + 8, ty, 80, 20, 5, i == tab ? R.ACCENT : h ? 0xFF303644 : 0xFF222630);
            Fnt.draw(QuickPlay.GAMES[i][0], x + 16, ty + 5.5f, 9, 0xFFFFFFFF);
        }
        String[] g = QuickPlay.GAMES[tab];
        for (int i = 0; i * 2 + 1 < g.length; i++) {
            float bx = x + 98 + (i % 2) * 98, by = y + 34 + (i / 2) * 22;
            boolean h = R.in(mx, my, bx, by, 94, 19);
            R.roundRect(bx, by, 94, 19, 5, h ? 0xFF3A4152 : 0xFF262B36);
            Fnt.center(g[i * 2 + 1], bx + 47, by + 5, 9, 0xFFFFFFFF);
        }
        R.reset();
    }

    protected void mouseClicked(int mx, int my, int b) throws IOException {
        if (b != 0) return;
        float x = x0(), y = y0();
        for (int i = 0; i < QuickPlay.GAMES.length; i++) if (R.in(mx, my, x + 8, y + 34 + i * 24, 80, 20)) { tab = i; return; }
        String[] g = QuickPlay.GAMES[tab];
        for (int i = 0; i * 2 + 1 < g.length; i++) {
            float bx = x + 98 + (i % 2) * 98, by = y + 34 + (i / 2) * 22;
            if (R.in(mx, my, bx, by, 94, 19)) {
                if (mc.thePlayer != null) mc.thePlayer.sendChatMessage(g[i * 2 + 2]);
                mc.displayGuiScreen(null);
                return;
            }
        }
    }

    public boolean doesGuiPauseGame() { return false; }
}
