package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.gui.CrashScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MinecraftError;
import net.minecraft.util.ReportedException;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Catches crashes in the game loop, saves the report and returns you to a recovery screen instead of closing the game. */
public class CrashPatcher extends Module {
    public static CrashPatcher INSTANCE;
    private long lastCrash;
    private int streak;

    public CrashPatcher() {
        super("Crash Patcher", "Recovers from in-game crashes.", Category.PERFORMANCE);
        INSTANCE = this;
    }

    /** @return true if the crash was handled and the game loop may continue. */
    public boolean handle(Throwable t) {
        if (t instanceof MinecraftError || t instanceof OutOfMemoryError || t instanceof ThreadDeath) return false;
        long now = System.currentTimeMillis();
        streak = now - lastCrash < 10000 ? streak + 1 : 1;
        lastCrash = now;
        if (streak > 3) return false; // crash loop: let the vanilla crash handler take over
        try {
            CrashReport report = t instanceof ReportedException ? ((ReportedException) t).getCrashReport() : new CrashReport("Unexpected error", t);
            File file = null;
            try {
                mc.addGraphicsAndWorldToCrashReport(report);
                File dir = new File(mc.mcDataDir, "crash-reports");
                file = new File(dir, "crash-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + "-client-recovered.txt");
                report.saveToFile(file);
            } catch (Throwable ignored) {}
            System.err.println("[Noctua] Recovered from a crash:");
            t.printStackTrace();
            resetRenderState();
            try {
                if (mc.getNetHandler() != null) mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Client crashed"));
            } catch (Throwable ignored) {}
            try { mc.loadWorld(null); } catch (Throwable ignored) {}
            try { mc.ingameGUI.getChatGUI().clearChatMessages(); } catch (Throwable ignored) {}
            mc.displayGuiScreen(new CrashScreen(report, file));
            return true;
        } catch (Throwable fatal) {
            return false;
        }
    }

    private static void resetRenderState() {
        try { Tessellator.getInstance().getWorldRenderer().finishDrawing(); } catch (Throwable ignored) {}
        try {
            unwind(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_STACK_DEPTH);
            unwind(GL11.GL_PROJECTION, GL11.GL_PROJECTION_STACK_DEPTH);
            unwind(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            while (GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH) > 0) GL11.glPopAttrib();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.color(1, 1, 1, 1);
            while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* drain */ }
        } catch (Throwable ignored) {}
    }

    private static void unwind(int mode, int depthName) {
        GL11.glMatrixMode(mode);
        int guard = 64;
        while (GL11.glGetInteger(depthName) > 1 && guard-- > 0) GL11.glPopMatrix();
    }
}
