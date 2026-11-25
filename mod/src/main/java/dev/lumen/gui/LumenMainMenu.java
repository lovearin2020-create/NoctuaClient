package dev.lumen.gui;

import dev.lumen.Lumen;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.GuiModList;

import java.io.IOException;

/** Animated title screen: aurora backdrop, logo that fades in, staggered buttons. */
public class LumenMainMenu extends GuiScreen {
    private static final String[] LABELS = {"Singleplayer", "Multiplayer", "Noctua Settings", "Options", "Forge Mods", "Quit"};
    private static boolean introPlayed;
    private final long opened = System.currentTimeMillis();
    private final boolean intro = !introPlayed;
    private final float[] hover = new float[LABELS.length];

    public LumenMainMenu() { introPlayed = true; }

    public boolean doesGuiPauseGame() { return false; }

    private float ease(float delayMs, float lengthMs) {
        float t = (System.currentTimeMillis() - opened - (intro ? delayMs : delayMs * 0.25f)) / (intro ? lengthMs : lengthMs * 0.5f);
        t = Math.max(0, Math.min(1, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private float bw() { return 190; }
    private float bx() { return width / 2f - bw() / 2f; }
    private float by(int i) { return logoY() + 78 + i * 28; }
    private float logoY() { return Math.max(16, height / 2f - 130); }

    public void drawScreen(int mx, int my, float pt) {
        Fnt.transform(1f, 0, 0);
        Backdrop.draw(width, height, mx, my);

        // logo
        float e = ease(150, 900);
        float ly = logoY() + (1 - e) * 14;
        R.glow(width / 2f, ly + 26, 150, R.alpha(0x554C8DFF, e));
        String logo = "NOCTUA";
        float sp = 6, lw = Fnt.width(logo, 44, Fnt.LOGO, sp);
        Fnt.draw(logo, width / 2f - lw / 2f, ly, 44, Fnt.LOGO, sp, R.alpha(0xFFFFFFFF, e), R.alpha(0xFF7FB2FF, e));
        String sub = "CLIENT";
        float sw = Fnt.width(sub, 10, Fnt.BOLD, 7);
        float lineW = 34 * ease(500, 700);
        R.rect(width / 2f - sw / 2f - 10 - lineW, ly + 60, lineW, 1, R.alpha(0x88FFFFFF, e));
        R.rect(width / 2f + sw / 2f + 10, ly + 60, lineW, 1, R.alpha(0x88FFFFFF, e));
        Fnt.draw(sub, width / 2f - sw / 2f, ly + 54, 10, Fnt.BOLD, 7, R.alpha(0xFFB9D2FF, e), R.alpha(0xFFB9D2FF, e));

        // buttons
        for (int i = 0; i < LABELS.length; i++) {
            float a = ease(450 + i * 90, 600);
            if (a <= 0.01f) continue;
            float x = bx(), y = by(i) + (1 - a) * 16;
            boolean h = a > 0.9f && R.in(mx, my, x, by(i), bw(), 23);
            hover[i] = Util.lerp(hover[i], h ? 1 : 0, 0.25f);
            float grow = hover[i] * 4;
            boolean quit = i == LABELS.length - 1;
            int base = R.alpha(0x30FFFFFF, a), hot = quit ? 0xE6E0526A : 0xE64C8DFF;
            R.roundRect(x - grow, y, bw() + grow * 2, 23, 7, R.mix(base, hot, hover[i]));
            R.roundOutline(x - grow, y, bw() + grow * 2, 23, 7, 1f, R.alpha(R.mix(0x38FFFFFF, 0x99FFFFFF, hover[i]), a));
            Fnt.center(LABELS[i], width / 2f, y + 6.5f, 10, R.alpha(0xFFFFFFFF, a));
        }

        float f = ease(1100, 600);
        Fnt.draw("Noctua Client " + Lumen.VERSION + "  |  Minecraft 1.8.9", 6, height - 14, 8, R.alpha(0xFF9AA8C7, f));
        String c = "Copyright Mojang AB. Do not distribute!";
        Fnt.draw(c, width - 6 - Fnt.width(c, 8), height - 14, 8, R.alpha(0xFF9AA8C7, f));
        if (essential()) {
            boolean h = R.in(mx, my, width - 128, 8, 120, 18);
            R.roundRect(width - 128, 8, 120, 18, 6, R.alpha(h ? 0xE64C8DFF : 0x30FFFFFF, f));
            Fnt.center("Accounts (Essential)", width - 68, 13, 8, R.alpha(0xFFFFFFFF, f));
        }
        R.reset();
        GlStateManager.color(1, 1, 1, 1);
    }

    /** Essential adds its account manager to the vanilla title screen; this button takes you there. */
    private static boolean essential() {
        try { return net.minecraftforge.fml.common.Loader.isModLoaded("essential"); } catch (Throwable t) { return false; }
    }

    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        if (essential() && R.in(mx, my, width - 128, 8, 120, 18)) {
            dev.lumen.mods.MainMenu.vanillaOnce = true;
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
            return;
        }
        for (int i = 0; i < LABELS.length; i++) {
            if (!R.in(mx, my, bx(), by(i), bw(), 23)) continue;
            switch (i) {
                case 0: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
                case 1: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
                case 2: mc.displayGuiScreen(new ModMenuScreen()); break;
                case 3: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
                case 4: mc.displayGuiScreen(new GuiModList(this)); break;
                default: mc.shutdown();
            }
            return;
        }
    }

    protected void keyTyped(char c, int key) {
        if (dev.lumen.core.Client.keyMenu.is(key)) mc.displayGuiScreen(new ModMenuScreen());
    }
}
