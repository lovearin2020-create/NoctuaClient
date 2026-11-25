package dev.lumen.gui;

import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.crash.CrashReport;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Shown by Crash Patcher after a recovered crash. */
public class CrashScreen extends GuiScreen {
    private final File file;
    private final List<String> lines = new ArrayList<String>();

    public CrashScreen(CrashReport report, File file) {
        this.file = file;
        try {
            Throwable t = report.getCrashCause();
            lines.add(String.valueOf(t));
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length && i < 9; i++) lines.add("    at " + st[i]);
        } catch (Throwable ignored) {
            lines.add(report.getDescription());
        }
    }

    public void drawScreen(int mx, int my, float pt) {
        Fnt.transform(1f, 0, 0);
        R.rect(0, 0, width, height, 0xFF0F1116);
        float cx = width / 2f;
        int w = Math.min(420, width - 40), x = (int) (cx - w / 2f), y = height / 2 - 110;
        Fnt.centerB("Noctua recovered from a crash", cx, y, 15, 0xFFF2F4F8);
        Fnt.center("The game hit an error but did not have to close. You were disconnected to keep things stable.", cx, y + 24, 8, 0xFF8891A5);
        R.roundRect(x, y + 42, w, 118, 6, 0xFF1A1D25);
        for (int i = 0; i < lines.size(); i++) {
            Fnt.draw(Fnt.fit(lines.get(i), 7, w - 16), x + 8, y + 49 + i * 11, 7, i == 0 ? 0xFFFF7B7B : 0xFFB3BACB);
        }
        if (file != null) Fnt.center("Report saved to crash-reports/" + file.getName(), cx, y + 166, 7, 0xFF5C6475);
        button(mx, my, cx - 125, y + 184, "Back to Title Screen", true);
        button(mx, my, cx + 5, y + 184, "Open Crash Report", false);
        R.reset();
    }

    private void button(int mx, int my, float x, float y, String label, boolean primary) {
        boolean h = R.in(mx, my, x, y, 120, 22);
        R.roundRect(x, y, 120, 22, 6, primary ? (h ? 0xFF6AA0FF : R.ACCENT) : (h ? 0xFF303644 : 0xFF262B36));
        Fnt.center(label, x + 60, y + 6.5f, 9, 0xFFFFFFFF);
    }

    protected void mouseClicked(int mx, int my, int b) throws IOException {
        float cx = width / 2f;
        int y = height / 2 - 110 + 184;
        if (R.in(mx, my, cx - 125, y, 120, 22)) mc.displayGuiScreen(new GuiMainMenu());
        else if (R.in(mx, my, cx + 5, y, 120, 22) && file != null) {
            try { Desktop.getDesktop().open(file); } catch (Throwable ignored) {}
        }
    }

    protected void keyTyped(char c, int key) {}
}
