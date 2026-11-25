package dev.lumen.fabric.gui;

import dev.lumen.fabric.util.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Shown by Crash Patcher after a recovered crash. */
public class CrashScreen extends Screen {
    private final File file;
    private final List<String> lines = new ArrayList<>();

    public CrashScreen(Throwable cause, File file) {
        super(Component.literal("Lumen crash recovery"));
        this.file = file;
        Throwable t = cause;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        lines.add(String.valueOf(t));
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length && i < 9; i++) lines.add("    at " + st[i]);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        Draw.begin(g);
        Draw.rect(0, 0, width, height, 0xFF0F1116);
        float cx = width / 2f;
        int w = Math.min(420, width - 40), x = (int) (cx - w / 2f), y = Math.max(8, height / 2 - 110);
        Draw.uiCenterB("Lumen recovered from a crash", cx, y, 15, 0xFFF2F4F8);
        Draw.uiCenter("The game hit an error but did not have to close. You were disconnected to keep things stable.", cx, y + 24, 8, 0xFF8891A5);
        Draw.roundRect(x, y + 42, w, 118, 6, 0xFF1A1D25);
        for (int i = 0; i < lines.size(); i++) Draw.ui(Draw.fit(lines.get(i), 7, w - 16), x + 8, y + 49 + i * 11, 7, i == 0 ? 0xFFFF7B7B : 0xFFB3BACB);
        if (file != null) Draw.uiCenter("Report saved to crash-reports/" + file.getName(), cx, y + 166, 7, 0xFF5C6475);
        button(mx, my, cx - 125, y + 184, "Back to Title Screen", true);
        button(mx, my, cx + 5, y + 184, "Open Crash Report", false);
    }

    private void button(int mx, int my, float x, float y, String label, boolean primary) {
        boolean h = Draw.in(mx, my, x, y, 120, 22);
        Draw.roundRect(x, y, 120, 22, 6, primary ? (h ? 0xFF6AA0FF : Draw.ACCENT) : (h ? 0xFF303644 : 0xFF262B36));
        Draw.uiCenter(label, x + 60, y + 6.5f, 9, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        float cx = width / 2f;
        int y = Math.max(8, height / 2 - 110) + 184;
        if (Draw.in(e.x(), e.y(), cx - 125, y, 120, 22)) minecraft.gui.setScreen(new TitleScreen());
        else if (Draw.in(e.x(), e.y(), cx + 5, y, 120, 22) && file != null) {
            try { dev.lumen.fabric.compat.Compat.openFile(file); } catch (Throwable ignored) {}
        }
        return true;
    }
}
