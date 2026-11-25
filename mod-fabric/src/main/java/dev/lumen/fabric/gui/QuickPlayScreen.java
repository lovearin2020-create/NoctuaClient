package dev.lumen.fabric.gui;

import dev.lumen.fabric.mods.QuickPlay;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class QuickPlayScreen extends Screen {
    private static int tab;
    private static final int W = 300, H = 170;

    public QuickPlayScreen() { super(Component.literal("Quick Play")); }

    private float x0() { return width / 2f - W / 2f; }
    private float y0() { return height / 2f - H / 2f; }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        Draw.begin(g);
        float x = x0(), y = y0();
        Draw.roundRect(x, y, W, H, 8, 0xF0141720);
        Draw.uiB("Quick Play", x + 12, y + 10, 12, 0xFFF2F4F8);
        for (int i = 0; i < QuickPlay.GAMES.length; i++) {
            float ty = y + 34 + i * 24;
            boolean h = Draw.in(mx, my, x + 8, ty, 80, 20);
            Draw.roundRect(x + 8, ty, 80, 20, 5, i == tab ? Draw.ACCENT : h ? 0xFF303644 : 0xFF222630);
            Draw.ui(QuickPlay.GAMES[i][0], x + 16, ty + 5.5f, 9, 0xFFFFFFFF);
        }
        String[] gm = QuickPlay.GAMES[tab];
        for (int i = 0; i * 2 + 1 < gm.length; i++) {
            float bx = x + 98 + (i % 2) * 98, by = y + 34 + (i / 2) * 22;
            boolean h = Draw.in(mx, my, bx, by, 94, 19);
            Draw.roundRect(bx, by, 94, 19, 5, h ? 0xFF3A4152 : 0xFF262B36);
            Draw.uiCenter(gm[i * 2 + 1], bx + 47, by + 5, 9, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        float x = x0(), y = y0();
        for (int i = 0; i < QuickPlay.GAMES.length; i++) if (Draw.in(e.x(), e.y(), x + 8, y + 34 + i * 24, 80, 20)) { tab = i; return true; }
        String[] gm = QuickPlay.GAMES[tab];
        for (int i = 0; i * 2 + 1 < gm.length; i++) {
            float bx = x + 98 + (i % 2) * 98, by = y + 34 + (i / 2) * 22;
            if (Draw.in(e.x(), e.y(), bx, by, 94, 19)) {
                if (minecraft.getConnection() != null) minecraft.getConnection().sendCommand(gm[i * 2 + 2].substring(1));
                minecraft.gui.setScreen(null);
                return true;
            }
        }
        return true;
    }
}
