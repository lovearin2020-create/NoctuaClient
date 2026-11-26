package dev.lumen.fabric.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumen.fabric.Lumen;
import dev.lumen.fabric.util.Draw;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Title screen: a night sky, the menu on a panel down the left side, and your own Quick Play servers bottom right. */
public class LumenMainMenu extends Screen {
    private static final Identifier SKY = Identifier.fromNamespaceAndPath("lumen", "textures/title/night.png");
    private static final Identifier MOON = Identifier.fromNamespaceAndPath("lumen", "textures/title/moon.png");
    private static final String[] LINKS = {"SINGLEPLAYER", "MULTIPLAYER", "MODS", "STORE", "OPTIONS", "QUIT"};
    private static final int TEXT = 0xFFFFFFFF, DIM = 0xFF9AA3B8, PANEL = 160, QW = 158, ROW = 17;
    private static boolean introPlayed;

    private final long opened = System.currentTimeMillis();
    private final boolean intro = !introPlayed;
    private final float[] hover = new float[LINKS.length];
    /** The Quick Play entry being edited: -2 none, -1 a new one. */
    private int editing = -2;

    /** 26.3 only delivers typed characters while text input is on, so the Quick Play form needs it. */
    @Override protected void init() { dev.lumen.fabric.compat.Compat.textInput(this, true); }
    @Override public void removed() { dev.lumen.fabric.compat.Compat.textInput(this, false); }
    private String editName = "", editAddress = "";
    private boolean addressFocus;

    public LumenMainMenu() { super(Component.literal("Noctua Client")); introPlayed = true; }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {}

    /** Everything is laid out on a screen 360 units high, then scaled to the window. */
    private float s() { return height / 360f; }
    private float vw() { return width / s(); }
    /** Where a line of letters goes so that its capitals start at this height. */
    private static float ui(float capTop) { return capTop; }

    private float ease(float delayMs, float lengthMs) {
        float t = (System.currentTimeMillis() - opened - (intro ? delayMs : 0)) / (intro ? lengthMs : lengthMs * 0.4f);
        t = Math.max(0, Math.min(1, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static boolean has(Identifier id) {
        try { return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(id).isPresent(); } catch (Throwable t) { return false; }
    }

    /** Draws a whole picture into the box, scaled. */
    private static void image(Identifier tex, float x, float y, float w, float h, int tw, int th) {
        var g = Draw.g();
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(w / tw, h / th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f, tw, th, tw, th);
        g.pose().popMatrix();
    }

    private void spaced(String s, float x, float y, float size, float gap, int color) {
        for (int i = 0; i < s.length(); i++) { String c = s.substring(i, i + 1); Draw.uiB(c, x, y, size, color); x += Draw.uiWidth(c, size, true) + gap; }
    }

    private float linkY(int i) { return 66 + i * 21; }
    private float qx() { return vw() - QW - 32; }
    private float qh() { return editing != -2 ? 96 : 30 + Math.max(1, QuickServers.all().size()) * (ROW + 3) + 4; }
    private float qy() { return 360 - 34 - qh(); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int rawX, int rawY, float partial) {
        Draw.begin(g);
        float s = s(), vw = vw(), mx = rawX / s, my = rawY / s;
        if (has(SKY)) {
            // the sky fills the window whatever its shape: the picture is cropped at the right or top, never stretched
            float k = Math.max(width / 1920f, height / 1080f);
            g.enableScissor(0, 0, width, height);
            image(SKY, 0, height - 1080 * k, 1920 * k, 1080 * k, 1920, 1080);
            g.disableScissor();
        } else Backdrop.draw(width, height, rawX, rawY);

        Draw.push(0, 0, s);
        Draw.uiScale = s;
        float in = ease(0, 700), slide = (1 - in) * -24;
        Draw.rect(slide, 0, PANEL, 360, Draw.alpha(0x85060910, in));
        Draw.rect(slide + PANEL, 0, 0.5f, 360, Draw.alpha(0x16FFFFFF, in));
        if (has(MOON)) image(MOON, slide + 17, 26, 17, 17, 128, 128);
        spaced("NOCTUA", slide + 41, ui(31.5f), 12, 3f, Draw.alpha(TEXT, in));
        spaced("CLIENT", slide + 42, ui(47.5f), 5, 1.2f, Draw.alpha(DIM, in));
        Draw.rect(slide + 17, 57, (PANEL - 32) * ease(250, 600), 1, Draw.alpha(Draw.ACCENT, 0.8f));
        for (int i = 0; i < LINKS.length; i++) {
            float a = ease(200 + i * 70, 500), y = linkY(i);
            boolean h = editing == -2 && Draw.in(mx, my, 10, y, PANEL - 20, 20);
            hover[i] = Draw.lerp(hover[i], h ? 1 : 0, 0.3f);
            int hot = i == LINKS.length - 1 ? 0xFFE0526A : Draw.ACCENT;
            if (hover[i] > 0.02f) {
                Draw.roundRect(slide + 12, y, PANEL - 26, 20, 5, Draw.alpha(hot, 0.22f * hover[i]));
                Draw.roundRect(slide + 12, y + 5, 1.5f, 10, 0.7f, Draw.alpha(hot, hover[i]));
            }
            spaced(LINKS[i], slide + 21 + hover[i] * 2, ui(y + 7), 8, 1.1f, Draw.alpha(TEXT, a * (0.6f + 0.4f * hover[i])));
        }
        String who = minecraft.getUser() == null ? "" : minecraft.getUser().getName();
        Draw.roundRect(slide + 17, 360 - 40, 13, 13, 3, Draw.alpha(Draw.ACCENT, in));
        Draw.uiCenterB(who.isEmpty() ? "?" : who.substring(0, 1).toUpperCase(), slide + 23.5f, ui(360 - 36.2f), 7, Draw.alpha(TEXT, in));
        Draw.ui(Draw.fit(who, 6, 90), slide + 35, ui(360 - 35.8f), 6, Draw.alpha(TEXT, in));

        quickPlay(mx, my, ease(450, 700));

        float f = ease(700, 600);
        Draw.ui("Noctua Client " + Lumen.VERSION + "  |  Minecraft " + SharedConstants.getCurrentVersion().name(), 14, 360 - 11, 5, Draw.alpha(0xFFFFFFFF, f * 0.42f));
        String c = "Not affiliated with Mojang or Microsoft";
        Draw.ui(c, vw - 14 - Draw.uiWidth(c, 5), 360 - 11, 5, Draw.alpha(0xFFFFFFFF, f * 0.42f));
        if (essential()) {
            boolean h = Draw.in(mx, my, vw - 112, 12, 98, 16);
            Draw.roundRect(vw - 112, 12, 98, 16, 5, Draw.alpha(h ? Draw.ACCENT : 0x85060910, f));
            Draw.uiCenter("Accounts (Essential)", vw - 63, ui(17.7f), 6, Draw.alpha(TEXT, f));
        }
        Draw.uiScale = 1f;
        Draw.pop();
    }

    private void field(String label, String value, boolean focus, float x, float y, float w) {
        Draw.ui(label, x, ui(y + 1.5f), 5, DIM);
        Draw.roundRect(x, y + 8, w, 14, 4, 0x55000000);
        Draw.roundOutline(x, y + 8, w, 14, 4, focus ? Draw.ACCENT : 0x1CFFFFFF);
        String shown = value;
        while (shown.length() > 0 && Draw.uiWidth(shown, 7) > w - 12) shown = shown.substring(1);
        if (focus && System.currentTimeMillis() / 500 % 2 == 0) shown += "|";
        Draw.ui(shown, x + 5, ui(y + 12.3f), 7, TEXT);
    }

    private void quickPlay(float mx, float my, float a) {
        if (a <= 0.01f) return;
        float x = qx(), y = qy() + (1 - a) * 12, h = qh();
        Draw.roundRect(x, y, QW, h, 7, Draw.alpha(0x85060910, a));
        Draw.roundOutline(x, y, QW, h, 7, Draw.alpha(0x16FFFFFF, a));
        Draw.ui(editing == -2 ? "QUICK PLAY" : editing == -1 ? "ADD A SERVER" : "EDIT SERVER", x + 10, ui(y + 10.5f), 5, Draw.alpha(DIM, a));
        Draw.rect(x + 10, y + 19, QW - 20, 1, Draw.alpha(Draw.ACCENT, 0.6f * a));
        if (editing != -2) {
            field("NAME", editName, !addressFocus, x + 10, y + 25, QW - 20);
            field("ADDRESS", editAddress, addressFocus, x + 10, y + 50, QW - 20);
            String[] b = editing >= 0 ? new String[]{"SAVE", "DELETE", "CANCEL"} : new String[]{"SAVE", "CANCEL"};
            float bw = (QW - 20 - (b.length - 1) * 4) / b.length;
            for (int i = 0; i < b.length; i++) {
                float bx = x + 10 + i * (bw + 4);
                boolean hv = Draw.in(mx, my, bx, y + 77, bw, 13);
                int col = i == 0 ? Draw.ACCENT : b[i].equals("DELETE") ? 0xFFE0526A : 0xFF3A4254;
                Draw.roundRect(bx, y + 77, bw, 13, 4, hv ? col : Draw.alpha(col, i == 0 ? 0.85f : 0.45f));
                Draw.uiCenterB(b[i], bx + bw / 2f, ui(y + 81.6f), 5, TEXT);
            }
            return;
        }
        List<QuickServers.Entry> list = QuickServers.all();
        if (list.size() < QuickServers.MAX) {
            boolean hv = Draw.in(mx, my, x + QW - 40, y + 5, 32, 11);
            Draw.uiB("+ ADD", x + QW - 10 - Draw.uiWidth("+ ADD", 5, true), ui(y + 10.5f), 5, Draw.alpha(hv ? TEXT : Draw.ACCENT, a));
        }
        if (list.isEmpty()) Draw.uiCenter("No servers yet. Press + ADD.", x + QW / 2f, ui(y + 31), 6, Draw.alpha(DIM, a));
        for (int i = 0; i < list.size(); i++) {
            float ry = y + 25 + i * (ROW + 3);
            boolean hv = Draw.in(mx, my, x + 8, ry, QW - 16, ROW);
            boolean eh = hv && mx >= x + QW - 34;
            Draw.roundRect(x + 8, ry, QW - 16, ROW, 5, Draw.alpha(hv && !eh ? Draw.mix(0x0EFFFFFF, Draw.ACCENT, 0.35f) : 0x0EFFFFFF, a));
            Draw.roundRect(x + 14.7f, ry + ROW / 2f - 1.8f, 3.6f, 3.6f, 1.8f, Draw.alpha(0xFF3DD68C, a));
            QuickServers.Entry e = list.get(i);
            Draw.uiB(Draw.fit(e.name, 7, 62), x + 23, ui(ry + 5.8f), 7, Draw.alpha(TEXT, a));
            if (hv) Draw.uiB("EDIT", x + QW - 14 - Draw.uiWidth("EDIT", 5, true), ui(ry + 6.6f), 5, Draw.alpha(eh ? TEXT : DIM, a));
            else { String ad = Draw.fit(e.address, 6, 58); Draw.ui(ad, x + QW - 14 - Draw.uiWidth(ad, 6), ui(ry + 6.2f), 6, Draw.alpha(DIM, a)); }
        }
    }

    /** Essential adds its account manager to the vanilla title screen; this button takes you there. */
    private static boolean essential() {
        try { return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("essential"); } catch (Throwable t) { return false; }
    }

    private void startEdit(int index) {
        editing = index;
        editName = index >= 0 ? QuickServers.all().get(index).name : "";
        editAddress = index >= 0 ? QuickServers.all().get(index).address : "";
        addressFocus = false;
    }
    /** For screenshots and tests. */
    public LumenMainMenu editDemo(int index) { startEdit(index); return this; }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        float s = s(), mx = (float) e.x() / s, my = (float) e.y() / s, vw = vw();
        float x = qx(), y = qy();
        boolean left = e.button() == InputConstants.MOUSE_BUTTON_LEFT, right = e.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        if (editing != -2) {
            if (!Draw.in(mx, my, x, y, QW, qh())) { editing = -2; return true; }
            if (Draw.in(mx, my, x + 10, y + 33, QW - 20, 14)) addressFocus = false;
            else if (Draw.in(mx, my, x + 10, y + 58, QW - 20, 14)) addressFocus = true;
            int n = editing >= 0 ? 3 : 2;
            float bw = (QW - 20 - (n - 1) * 4) / (float) n;
            for (int i = 0; i < n; i++) {
                if (!Draw.in(mx, my, x + 10 + i * (bw + 4), y + 77, bw, 13)) continue;
                if (i == 0) QuickServers.put(editing, editName, editAddress);
                else if (n == 3 && i == 1) QuickServers.remove(editing);
                editing = -2;
                return true;
            }
            return true;
        }
        List<QuickServers.Entry> list = QuickServers.all();
        if (left && list.size() < QuickServers.MAX && Draw.in(mx, my, x + QW - 40, y + 5, 32, 11)) { startEdit(-1); return true; }
        for (int i = 0; i < list.size(); i++) {
            float ry = y + 25 + i * (ROW + 3);
            if (!Draw.in(mx, my, x + 8, ry, QW - 16, ROW)) continue;
            if (right || mx >= x + QW - 34) startEdit(i);
            else if (left) {
                QuickServers.Entry q = list.get(i);
                try { ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(q.address), new ServerData(q.name, q.address, ServerData.Type.OTHER), false, null); }
                catch (Throwable t) { System.err.println("[Noctua] Could not connect to " + q.address + ": " + t); }
            }
            return true;
        }
        if (!left) return false;
        if (essential() && Draw.in(mx, my, vw - 112, 12, 98, 16)) {
            dev.lumen.fabric.Hooks.vanillaTitleOnce = true;
            minecraft.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            return true;
        }
        for (int i = 0; i < LINKS.length; i++) {
            if (!Draw.in(mx, my, 10, linkY(i), PANEL - 20, 20)) continue;
            switch (i) {
                case 0 -> minecraft.gui.setScreen(new SelectWorldScreen(this));
                case 1 -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
                case 2 -> minecraft.gui.setScreen(new ModMenuScreen());
                case 3 -> minecraft.gui.setScreen(new ModMenuScreen().openStore());
                case 4 -> minecraft.gui.setScreen(dev.lumen.fabric.compat.Compat.optionsScreen(this, minecraft.options));
                default -> minecraft.stop();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = e.key();
        if (editing != -2) {
            String v = addressFocus ? editAddress : editName;
            if (key == InputConstants.KEY_ESCAPE) editing = -2;
            else if (key == InputConstants.KEY_TAB) addressFocus = !addressFocus;
            else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { QuickServers.put(editing, editName, editAddress); editing = -2; }
            else if (key == InputConstants.KEY_BACKSPACE) { if (!v.isEmpty()) set(v.substring(0, v.length() - 1)); }
            else if (key == InputConstants.KEY_V && (e.modifiers() & InputConstants.MOD_CONTROL) != 0) set(v + minecraft.keyboardHandler.getClipboard().replaceAll("[\\p{Cntrl}§]", ""));
            return true;
        }
        if (dev.lumen.fabric.core.Client.keyMenu.is(key)) { minecraft.gui.setScreen(new ModMenuScreen()); return true; }
        return false;
    }

    private void set(String v) {
        int max = addressFocus ? 64 : 20;
        if (v.length() > max) v = v.substring(0, max);
        if (addressFocus) editAddress = v.trim(); else editName = v;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (editing == -2 || !e.isAllowedChatCharacter()) return false;
        set((addressFocus ? editAddress : editName) + e.codepointAsString());
        return true;
    }
}
