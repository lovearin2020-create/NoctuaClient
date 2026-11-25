package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.Category;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.gui.CrashScreen;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Catches crashes in the game loop, saves the report and returns you to a recovery screen instead of closing the game. */
public class CrashPatcher extends Module {
    private long lastCrash;
    private int streak;

    public CrashPatcher() { super("Crash Patcher", "Recovers from in-game crashes.", Category.PERFORMANCE); }

    /** @return true if the crash was handled and the game loop may continue. */
    public boolean handle(Throwable t) {
        if (t instanceof Error) return false; // out of memory, linkage errors: let the game handle those
        long now = System.currentTimeMillis();
        streak = now - lastCrash < 10000 ? streak + 1 : 1;
        lastCrash = now;
        if (streak > 3) return false; // crash loop: let the vanilla crash handler take over
        try {
            CrashReport report = t instanceof ReportedException r ? r.getReport() : new CrashReport("Unexpected error", t);
            File file = null;
            try {
                File dir = new File(mc.gameDirectory, "crash-reports");
                dir.mkdirs();
                file = new File(dir, "crash-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + "-client-recovered.txt");
                java.nio.file.Files.writeString(file.toPath(), report.getFriendlyReport(net.minecraft.ReportType.CRASH));
            } catch (Throwable e) { file = null; }
            System.err.println("[Noctua] Recovered from a crash:");
            t.printStackTrace();
            try { if (mc.level != null) mc.level.disconnect(Component.literal("Client crashed")); } catch (Throwable ignored) {}
            try { mc.disconnectWithProgressScreen(); } catch (Throwable ignored) {}
            mc.gui.setScreen(new CrashScreen(t, file));
            finishFrame();
            return true;
        } catch (Throwable fatal) {
            return false;
        }
    }

    /**
     * A crash in the middle of drawing leaves the window's frame claimed, and the game would never draw again.
     * This hands the unfinished frame back so the next one can start.
     */
    private void finishFrame() {
        try {
            var surface = mc.windowSurface();
            if (surface.isAcquired()) {
                var device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
                var color = mc.gameRenderer.mainRenderTarget().getColorTextureView();
                if (color != null) surface.blitFromTexture(device.createCommandEncoder(), color);
                device.createCommandEncoder().submit();
                surface.present();
            }
        } catch (Throwable e) { System.err.println("[Noctua] Could not release the unfinished frame: " + e); }
        try { com.mojang.blaze3d.systems.RenderSystem.getDynamicUniforms().reset(); } catch (Throwable ignored) {}
        try { mc.levelRenderer.endFrame(); } catch (Throwable ignored) {}
    }
}
