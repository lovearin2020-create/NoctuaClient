package dev.lumen.fabric.gui;

import dev.lumen.fabric.Lumen;
import dev.lumen.fabric.util.Draw;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Animated title screen: aurora backdrop, logo that fades in, staggered buttons. */
public class LumenMainMenu extends Screen {
    private static final String[] LABELS = {"Singleplayer", "Multiplayer", "Lumen Settings", "Options", "Quit"};
    private static boolean introPlayed;
    private final long opened = System.currentTimeMillis();
    private final boolean intro = !introPlayed;
    private final float[] hover = new float[LABELS.length];

    public LumenMainMenu() { super(Component.literal("Lumen Client")); introPlayed = true; }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {}

    private float ease(float delayMs, float lengthMs) {
        float t = (System.currentTimeMillis() - opened - (intro ? delayMs : delayMs * 0.25f)) / (intro ? lengthMs : lengthMs * 0.5f);
        t = Math.max(0, Math.min(1, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private float bw() { return 190; }
    private float bx() { return width / 2f - bw() / 2f; }
    private float logoY() { return Math.max(12, height / 2f - 116); }
    private float by(int i) { return logoY() + 76 + i * 28; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        Draw.begin(g);
        Backdrop.draw(width, height, mx, my);

        float e = ease(150, 900);
        float ly = logoY() + (1 - e) * 14;
        Draw.uiCenterB("LUMEN", width / 2f, ly, 44, Draw.alpha(0xFFFFFFFF, e));
        String sub = "C L I E N T";
        float sw = Draw.uiWidth(sub, 10, true);
        float lineW = 34 * ease(500, 700);
        Draw.rect(width / 2f - sw / 2f - 10 - lineW, ly + 59, lineW, 1, Draw.alpha(0x88FFFFFF, e));
        Draw.rect(width / 2f + sw / 2f + 10, ly + 59, lineW, 1, Draw.alpha(0x88FFFFFF, e));
        Draw.uiCenterB(sub, width / 2f, ly + 54, 10, Draw.alpha(0xFFB9D2FF, e));

        for (int i = 0; i < LABELS.length; i++) {
            float a = ease(450 + i * 90, 600);
            if (a <= 0.03f) continue;
            float x = bx(), y = by(i) + (1 - a) * 16;
            boolean h = a > 0.9f && Draw.in(mx, my, x, by(i), bw(), 23);
            hover[i] = Draw.lerp(hover[i], h ? 1 : 0, 0.25f);
            float grow = Math.round(hover[i] * 4);
            boolean quit = i == LABELS.length - 1;
            int base = Draw.alpha(0x30FFFFFF, a), hot = quit ? 0xE6E0526A : 0xE64C8DFF;
            Draw.roundRect(x - grow, y, bw() + grow * 2, 23, 7, Draw.mix(base, hot, hover[i]));
            Draw.outline(x - grow, y, bw() + grow * 2, 23, Draw.alpha(Draw.mix(0x38FFFFFF, 0x99FFFFFF, hover[i]), a));
            Draw.uiCenter(LABELS[i], width / 2f, y + 6.5f, 10, Draw.alpha(0xFFFFFFFF, a));
        }

        float f = ease(1100, 600);
        Draw.ui("Lumen Client " + Lumen.VERSION + "  |  Minecraft " + SharedConstants.getCurrentVersion().name(), 6, height - 14, 8, Draw.alpha(0xFF9AA8C7, f));
        String c = "Copyright Mojang AB. Do not distribute!";
        Draw.ui(c, width - 6 - Draw.uiWidth(c, 8), height - 14, 8, Draw.alpha(0xFF9AA8C7, f));
        if (essential()) {
            boolean h = Draw.in(mx, my, width - 128, 8, 120, 18);
            Draw.roundRect(width - 128, 8, 120, 18, 6, Draw.alpha(h ? 0xE64C8DFF : 0x30FFFFFF, f));
            Draw.uiCenter("Accounts (Essential)", width - 68, 13, 8, Draw.alpha(0xFFFFFFFF, f));
        }
    }

    /** Essential adds its account manager to the vanilla title screen; this button takes you there. */
    private static boolean essential() {
        try { return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("essential"); } catch (Throwable t) { return false; }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (e.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (essential() && Draw.in(e.x(), e.y(), width - 128, 8, 120, 18)) {
            dev.lumen.fabric.Hooks.vanillaTitleOnce = true;
            minecraft.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            return true;
        }
        for (int i = 0; i < LABELS.length; i++) {
            if (!Draw.in(e.x(), e.y(), bx(), by(i), bw(), 23)) continue;
            switch (i) {
                case 0 -> minecraft.gui.setScreen(new SelectWorldScreen(this));
                case 1 -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
                case 2 -> minecraft.gui.setScreen(new ModMenuScreen());
                case 3 -> minecraft.gui.setScreen(dev.lumen.fabric.compat.Compat.optionsScreen(this, minecraft.options));
                default -> minecraft.stop();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (dev.lumen.fabric.core.Client.keyMenu.is(e.key())) { minecraft.gui.setScreen(new ModMenuScreen()); return true; }
        return false;
    }
}
