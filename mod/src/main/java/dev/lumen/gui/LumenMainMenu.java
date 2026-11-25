package dev.lumen.gui;

import dev.lumen.Lumen;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.GuiModList;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;

/** Title screen: a night sky, the menu on a panel down the left side, and your own Quick Play servers bottom right. */
public class LumenMainMenu extends GuiScreen {
    private static final ResourceLocation SKY = new ResourceLocation("lumen", "textures/title/night.png");
    private static final ResourceLocation MOON = new ResourceLocation("lumen", "icon/moon.png");
    private static final String[] LINKS = {"SINGLEPLAYER", "MULTIPLAYER", "MODS", "STORE", "OPTIONS", "QUIT"};
    private static final int TEXT = 0xFFFFFFFF, DIM = 0xFF9AA3B8, PANEL = 160, QW = 158, ROW = 17;
    private static boolean introPlayed;

    private final long opened = System.currentTimeMillis();
    private final boolean intro = !introPlayed;
    private final float[] hover = new float[LINKS.length];
    /** The Quick Play entry being edited: -2 none, -1 a new one. */
    private int editing = -2;
    private String editName = "", editAddress = "";
    private boolean addressFocus;

    public LumenMainMenu() { introPlayed = true; }

    public boolean doesGuiPauseGame() { return false; }
    public void initGui() { Keyboard.enableRepeatEvents(true); }
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    /** Everything is laid out on a screen 360 units high, then scaled to the window. */
    private float s() { return height / 360f; }
    private float vw() { return width / s(); }

    private float ease(float delayMs, float lengthMs) {
        float t = (System.currentTimeMillis() - opened - (intro ? delayMs : 0)) / (intro ? lengthMs : lengthMs * 0.4f);
        t = Math.max(0, Math.min(1, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private void image(ResourceLocation tex, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float alpha) {
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1, 1, 1, alpha);
        mc.getTextureManager().bindTexture(tex);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex2f(x, y + h);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GlStateManager.color(1, 1, 1, 1);
    }

    private float linkY(int i) { return 66 + i * 21; }
    private float qx() { return vw() - QW - 32; }
    private int qRows() { return QuickServers.all().size(); }
    private float qh() { return editing != -2 ? 96 : 30 + Math.max(1, qRows()) * (ROW + 3) + 4; }
    private float qy() { return 360 - 34 - qh(); }

    public void drawScreen(int rawX, int rawY, float pt) {
        float s = s(), vw = vw();
        float mx = rawX / s, my = rawY / s;
        // the sky fills the window whatever its shape: the picture is cropped, never stretched
        float sa = 16f / 9f, wa = width / (float) height, u0 = 0, u1 = 1, v0 = 0, v1 = 1;
        if (wa > sa) { float k = sa / wa; v0 = 1 - k; } else { float k = wa / sa; u1 = k; }
        image(SKY, 0, 0, width, height, u0, v0, u1, v1, 1);

        GlStateManager.pushMatrix();
        GlStateManager.scale(s, s, 1);
        Fnt.transform(s, 0, 0);
        float in = ease(0, 700);

        // the menu panel
        float slide = (1 - in) * -24;
        R.rect(slide, 0, PANEL, 360, R.alpha(0x85060910, in));
        R.rect(slide + PANEL, 0, 0.5f, 360, R.alpha(0x16FFFFFF, in));
        image(MOON, slide + 17, 26, 17, 17, 0, 0, 1, 1, in);
        Fnt.draw("NOCTUA", slide + 41, 28.5f, 12, Fnt.LOGO, 3f, R.alpha(TEXT, in), R.alpha(TEXT, in));
        Fnt.draw("CLIENT", slide + 42, 46, 5, Fnt.REGULAR, 1.2f, R.alpha(DIM, in), R.alpha(DIM, in));
        R.rect(slide + 17, 57, (PANEL - 32) * ease(250, 600), 1, R.alpha(R.ACCENT, 0.8f));
        for (int i = 0; i < LINKS.length; i++) {
            float a = ease(200 + i * 70, 500);
            float y = linkY(i);
            boolean h = editing == -2 && R.in(mx, my, 10, y, PANEL - 20, 20);
            hover[i] = Util.lerp(hover[i], h ? 1 : 0, 0.3f);
            if (hover[i] > 0.02f) {
                R.roundRect(slide + 12, y, PANEL - 26, 20, 5, R.alpha(i == LINKS.length - 1 ? 0xFFE0526A : R.ACCENT, 0.22f * hover[i]));
                R.roundRect(slide + 12, y + 5, 1.5f, 10, 0.7f, R.alpha(i == LINKS.length - 1 ? 0xFFE0526A : R.ACCENT, hover[i]));
            }
            int c = R.alpha(R.mix(0x94FFFFFF | 0xFF000000, TEXT, hover[i]), a * (0.6f + 0.4f * hover[i]));
            Fnt.draw(LINKS[i], slide + 21 + hover[i] * 2, y + 6.5f, 8, Fnt.BOLD, 1.1f, c, c);
        }
        // who is playing, and the way to Forge's own mod list
        String who = mc.getSession() == null ? "" : mc.getSession().getUsername();
        R.roundRect(slide + 17, 360 - 40, 13, 13, 3, R.alpha(R.ACCENT, in));
        Fnt.centerB(who.isEmpty() ? "?" : who.substring(0, 1).toUpperCase(), slide + 23.5f, 360 - 36.5f, 7, R.alpha(TEXT, in));
        Fnt.draw(Fnt.fit(who, 6, 70), slide + 35, 360 - 36, 6, R.alpha(TEXT, in));
        boolean fh = R.in(mx, my, PANEL - 52, 360 - 41, 40, 14);
        Fnt.draw("FORGE MODS", slide + PANEL - 14 - Fnt.width("FORGE MODS", 5, Fnt.REGULAR, 0.6f), 360 - 35.5f, 5, Fnt.REGULAR, 0.6f, R.alpha(fh ? TEXT : DIM, in), R.alpha(fh ? TEXT : DIM, in));

        quickPlay(mx, my, ease(450, 700));

        float f = ease(700, 600);
        Fnt.draw("Noctua Client " + Lumen.VERSION + "  |  Minecraft 1.8.9", 14, 360 - 12, 5, R.alpha(0x6BFFFFFF | 0xFF000000, f * 0.42f));
        String c = "Not affiliated with Mojang or Microsoft";
        Fnt.draw(c, vw - 14 - Fnt.width(c, 5), 360 - 12, 5, R.alpha(0xFFFFFFFF, f * 0.42f));
        if (essential()) {
            boolean h = R.in(mx, my, vw - 112, 12, 98, 16);
            R.roundRect(vw - 112, 12, 98, 16, 5, R.alpha(h ? R.ACCENT : 0x85060910, f));
            Fnt.center("Accounts (Essential)", vw - 63, 16.5f, 6, R.alpha(TEXT, f));
        }
        GlStateManager.popMatrix();
        Fnt.transform(1f, 0, 0);
        R.reset();
        GlStateManager.color(1, 1, 1, 1);
    }

    private void field(String label, String value, boolean focus, float x, float y, float w) {
        Fnt.draw(label, x, y, 5, Fnt.REGULAR, 0.6f, DIM, DIM);
        R.roundRect(x, y + 8, w, 14, 4, 0x55000000);
        R.roundOutline(x, y + 8, w, 14, 4, 1f, focus ? R.ACCENT : 0x1CFFFFFF);
        String shown = value;
        while (shown.length() > 0 && Fnt.width(shown, 7) > w - 12) shown = shown.substring(1);
        if (focus && System.currentTimeMillis() / 500 % 2 == 0) shown += "|";
        Fnt.draw(shown, x + 5, y + 11.5f, 7, TEXT);
    }

    private void quickPlay(float mx, float my, float a) {
        if (a <= 0.01f) return;
        float x = qx(), y = qy() + (1 - a) * 12, h = qh();
        R.roundRect(x, y, QW, h, 7, R.alpha(0x85060910, a));
        R.gradient(x + 5, y + 1, QW - 10, 9, R.alpha(0x12FFFFFF, a), 0x00FFFFFF);
        R.roundOutline(x, y, QW, h, 7, 1f, R.alpha(0x16FFFFFF, a));
        Fnt.draw(editing == -2 ? "QUICK PLAY" : editing == -1 ? "ADD A SERVER" : "EDIT SERVER", x + 10, y + 9, 5, Fnt.REGULAR, 0.9f, R.alpha(DIM, a), R.alpha(DIM, a));
        R.rect(x + 10, y + 19, QW - 20, 1, R.alpha(R.ACCENT, 0.6f * a));
        if (editing != -2) {
            field("NAME", editName, !addressFocus, x + 10, y + 25, QW - 20);
            field("ADDRESS", editAddress, addressFocus, x + 10, y + 50, QW - 20);
            String[] b = editing >= 0 ? new String[]{"SAVE", "DELETE", "CANCEL"} : new String[]{"SAVE", "CANCEL"};
            float bw = (QW - 20 - (b.length - 1) * 4) / b.length;
            for (int i = 0; i < b.length; i++) {
                float bx = x + 10 + i * (bw + 4);
                boolean hv = R.in(mx, my, bx, y + 77, bw, 13);
                int col = i == 0 ? R.ACCENT : b[i].equals("DELETE") ? 0xFFE0526A : 0xFF3A4254;
                R.roundRect(bx, y + 77, bw, 13, 4, hv ? col : R.alpha(col, i == 0 ? 0.85f : 0.45f));
                Fnt.track(b[i], bx + bw / 2f, y + 80.8f, 5, Fnt.BOLD, 0.7f, TEXT);
            }
            return;
        }
        List<QuickServers.Entry> list = QuickServers.all();
        if (list.size() < QuickServers.MAX) {
            boolean hv = R.in(mx, my, x + QW - 40, y + 5, 32, 11);
            Fnt.draw("+ ADD", x + QW - 10 - Fnt.width("+ ADD", 5, Fnt.BOLD, 0.6f), y + 9, 5, Fnt.BOLD, 0.6f, R.alpha(hv ? TEXT : R.ACCENT, a), R.alpha(hv ? TEXT : R.ACCENT, a));
        }
        if (list.isEmpty()) Fnt.center("No servers yet. Press + ADD.", x + QW / 2f, y + 30, 6, R.alpha(DIM, a));
        for (int i = 0; i < list.size(); i++) {
            float ry = y + 25 + i * (ROW + 3);
            boolean hv = R.in(mx, my, x + 8, ry, QW - 16, ROW);
            boolean eh = hv && mx >= x + QW - 34;
            R.roundRect(x + 8, ry, QW - 16, ROW, 5, R.alpha(hv && !eh ? R.mix(0x0EFFFFFF, R.ACCENT, 0.35f) : 0x0EFFFFFF, a));
            R.circle(x + 16.5f, ry + ROW / 2f, 1.8f, R.alpha(0xFF3DD68C, a));
            QuickServers.Entry e = list.get(i);
            Fnt.drawB(Fnt.fit(e.name, 7, 62), x + 23, ry + 5, 7, R.alpha(TEXT, a));
            if (hv) Fnt.draw("EDIT", x + QW - 14 - Fnt.width("EDIT", 5, Fnt.BOLD, 0.6f), ry + 6, 5, Fnt.BOLD, 0.6f, R.alpha(eh ? TEXT : DIM, a), R.alpha(eh ? TEXT : DIM, a));
            else { String ad = Fnt.fit(e.address, 6, 58); Fnt.draw(ad, x + QW - 14 - Fnt.width(ad, 6), ry + 5.5f, 6, R.alpha(DIM, a)); }
        }
    }

    /** Essential adds its account manager to the vanilla title screen; this button takes you there. */
    private static boolean essential() {
        try { return net.minecraftforge.fml.common.Loader.isModLoaded("essential"); } catch (Throwable t) { return false; }
    }

    private void startEdit(int index) {
        editing = index;
        editName = index >= 0 ? QuickServers.all().get(index).name : "";
        editAddress = index >= 0 ? QuickServers.all().get(index).address : "";
        addressFocus = false;
    }

    protected void mouseClicked(int rawX, int rawY, int button) throws IOException {
        float s = s(), mx = rawX / s, my = rawY / s, vw = vw();
        float x = qx(), y = qy();
        if (editing != -2) {
            if (!R.in(mx, my, x, y, QW, qh())) { editing = -2; return; }
            if (R.in(mx, my, x + 10, y + 33, QW - 20, 14)) addressFocus = false;
            else if (R.in(mx, my, x + 10, y + 58, QW - 20, 14)) addressFocus = true;
            int n = editing >= 0 ? 3 : 2;
            float bw = (QW - 20 - (n - 1) * 4) / (float) n;
            for (int i = 0; i < n; i++) {
                if (!R.in(mx, my, x + 10 + i * (bw + 4), y + 77, bw, 13)) continue;
                if (i == 0) QuickServers.put(editing, editName, editAddress);
                else if (n == 3 && i == 1) QuickServers.remove(editing);
                editing = -2;
                return;
            }
            return;
        }
        List<QuickServers.Entry> list = QuickServers.all();
        if (button == 0 && list.size() < QuickServers.MAX && R.in(mx, my, x + QW - 40, y + 5, 32, 11)) { startEdit(-1); return; }
        for (int i = 0; i < list.size(); i++) {
            float ry = y + 25 + i * (ROW + 3);
            if (!R.in(mx, my, x + 8, ry, QW - 16, ROW)) continue;
            if (button == 1 || mx >= x + QW - 34) startEdit(i);
            else if (button == 0) mc.displayGuiScreen(new GuiConnecting(this, mc, new ServerData(list.get(i).name, list.get(i).address, false)));
            return;
        }
        if (button != 0) return;
        if (essential() && R.in(mx, my, vw - 112, 12, 98, 16)) {
            dev.lumen.mods.MainMenu.vanillaOnce = true;
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
            return;
        }
        if (R.in(mx, my, PANEL - 52, 360 - 41, 40, 14)) { mc.displayGuiScreen(new GuiModList(this)); return; }
        for (int i = 0; i < LINKS.length; i++) {
            if (!R.in(mx, my, 10, linkY(i), PANEL - 20, 20)) continue;
            switch (i) {
                case 0: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
                case 1: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
                case 2: mc.displayGuiScreen(new ModMenuScreen()); break;
                case 3: mc.displayGuiScreen(new ModMenuScreen().openStore()); break;
                case 4: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
                default: mc.shutdown();
            }
            return;
        }
    }

    protected void keyTyped(char c, int key) {
        if (editing != -2) {
            String v = addressFocus ? editAddress : editName;
            if (key == Keyboard.KEY_ESCAPE) { editing = -2; return; }
            if (key == Keyboard.KEY_TAB) { addressFocus = !addressFocus; return; }
            if (key == Keyboard.KEY_RETURN) { QuickServers.put(editing, editName, editAddress); editing = -2; return; }
            if (key == Keyboard.KEY_BACK) { if (!v.isEmpty()) v = v.substring(0, v.length() - 1); }
            else if (key == Keyboard.KEY_V && isCtrlKeyDown()) v += ChatAllowedCharacters.filterAllowedCharacters(getClipboardString());
            else if (ChatAllowedCharacters.isAllowedCharacter(c)) v += c;
            int max = addressFocus ? 64 : 20;
            if (v.length() > max) v = v.substring(0, max);
            if (addressFocus) editAddress = v.trim(); else editName = v;
            return;
        }
        if (dev.lumen.core.Client.keyMenu.is(key)) mc.displayGuiScreen(new ModMenuScreen());
    }

    /** For screenshots and tests. */
    public LumenMainMenu editDemo(int index) { startEdit(index); return this; }
}
