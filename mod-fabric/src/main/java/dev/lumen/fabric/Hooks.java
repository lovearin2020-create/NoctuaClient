package dev.lumen.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumen.fabric.core.HudModule;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.cosmetics.CapeTextures;
import dev.lumen.fabric.gui.HudEditorScreen;
import dev.lumen.fabric.gui.LumenMainMenu;
import dev.lumen.fabric.gui.ModMenuScreen;
import dev.lumen.fabric.mixin.OverlayTextureAccessor;
import dev.lumen.fabric.mods.*;
import dev.lumen.fabric.util.Clicks;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.scores.Objective;
import org.joml.Matrix3x2fStack;

import java.util.HashSet;
import java.util.Set;

/** Static entry points called from the mixins. Every method is safe to call at any time. */
public final class Hooks {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Set<Integer> DOWN = new HashSet<>();

    public static boolean keyDown(int key) { return DOWN.contains(key); }

    private static boolean ready() { return Lumen.modules != null; }

    public static void tick() {
        if (!ready()) return;
        Lumen.ensureLoaded();
        dev.lumen.fabric.online.Online.tick();
        MemoryOptimizer mem = Lumen.modules.get(MemoryOptimizer.class);
        if (mem != null && mem.isEnabled()) mem.anyTick();
        syncHitColor();
        if (mc.player == null) return;
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled()) continue;
            try { m.onTick(); } catch (Throwable ignored) {}
        }
    }

    public static void hud(GuiGraphicsExtractor g) {
        if (!ready() || mc.player == null || mc.gui.screen() instanceof HudEditorScreen || mc.gui.screen() instanceof dev.lumen.fabric.gui.ModMenuScreen) return; // the menu is see-through: no HUD under it
        Draw.begin(g);
        int sw = g.guiWidth(), sh = g.guiHeight();
        for (Module m : Lumen.modules.all) {
            if (m.isEnabled() && m instanceof HudModule h) h.render(sw, sh, false);
        }
    }

    /** @return true when the key event was used by Lumen and must not reach the game. */
    public static boolean key(KeyEvent event, int action) {
        int key = event.key();
        if (action == 1) DOWN.add(key); else if (action == 0) DOWN.remove(key);
        if (!ready()) return false;
        if (action == 1 && dev.lumen.fabric.core.Client.keyMenu.is(key) && mc.gui.screen() == null && mc.player != null) {
            mc.gui.setScreen(new ModMenuScreen());
            return true;
        }
        boolean used = false;
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled()) continue;
            try { used |= m.onKey(event, action); } catch (Throwable ignored) {}
        }
        return used;
    }

    public static void mouse(int button, int action) {
        if (action != 1 || !ready() || mc.gui.screen() != null) return;
        if (button == InputConstants.MOUSE_BUTTON_LEFT) Clicks.press(0);
        else if (button == InputConstants.MOUSE_BUTTON_RIGHT) Clicks.press(1);
    }

    public static void scroll(double amount) {
        ScrollableTooltips t = get(ScrollableTooltips.class);
        if (t != null) t.scroll(amount);
    }

    public static void attack(Entity target) {
        if (!ready() || target == null) return;
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled()) continue;
            try { m.onAttack(target); } catch (Throwable ignored) {}
        }
    }

    /** @return true to hide the chat line. */
    public static boolean chat(String plain) {
        if (!ready() || plain == null) return false;
        if (get(ChatTweaks.class) != null) plain = ChatTweaks.stripCounter(plain);
        boolean hide = false;
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled()) continue;
            try { hide |= m.onChat(plain); } catch (Throwable ignored) {}
        }
        return hide;
    }

    /** Once per frame, while the game is collecting world-space gizmos. */
    public static void frame() {
        if (!ready() || mc.level == null || mc.player == null) return;
        float pt;
        try { pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true); } catch (Throwable t) { pt = 1f; }
        for (Module m : Lumen.modules.all) {
            if (!m.isEnabled()) continue;
            try { m.onFrame(pt); } catch (Throwable ignored) {}
        }
    }

    // ---- camera

    public static float fov(float fov) {
        Zoom z = ready() ? Lumen.modules.get(Zoom.class) : null;
        return z == null ? fov : z.apply(fov);
    }

    public static float fovModifier(float modifier) {
        FovTweaks f = get(FovTweaks.class);
        return f == null ? modifier : f.apply(modifier);
    }

    public static boolean noHurtCam() { return get(HurtCam.class) != null; }
    public static boolean fullbright() { return get(Fullbright.class) != null; }
    public static float fireLower() { FireOverlay f = get(FireOverlay.class); return f == null ? 0f : f.lower.f(); }
    public static long time(long original) { TimeChanger t = get(TimeChanger.class); return t == null ? original : t.time(); }
    public static Identifier motionBlur() { MotionBlur m = get(MotionBlur.class); return m == null ? null : m.effect(); }
    public static Identifier vibrance() { dev.lumen.fabric.mods.Vibrance m = get(dev.lumen.fabric.mods.Vibrance.class); return m == null ? null : m.effect(); }

    public static boolean cleanView(Object entity) {
        return entity == mc.player && get(CleanView.class) != null && mc.options.getCameraType().isFirstPerson();
    }

    // ---- HUD pieces

    /** Draws the custom crosshair. @return true when the vanilla one must be skipped. */
    public static boolean crosshair(GuiGraphicsExtractor g) {
        Crosshair c = get(Crosshair.class);
        if (c == null) return false;
        Draw.begin(g);
        try { c.draw(g.guiWidth(), g.guiHeight()); } catch (Throwable ignored) {}
        return true;
    }

    /** @return true when the sidebar was drawn (or hidden) by Lumen. */
    public static boolean sidebar(GuiGraphicsExtractor g, Objective objective) {
        ScoreboardMod s = get(ScoreboardMod.class);
        if (s == null) return false;
        try { s.render(g, objective); } catch (Throwable ignored) {}
        return true;
    }

    public static boolean hideBossBar() { return get(BossBarMod.class) != null; }
    public static boolean hideVanillaEffects() { PotionEffects p = get(PotionEffects.class); return p != null && p.hideVanilla.on(); }
    public static boolean hideTitles() { Titles t = get(Titles.class); return t != null && t.hide.on(); }
    public static float titleScale() { Titles t = get(Titles.class); return t == null ? 1f : t.scale.f(); }

    public static boolean tabPing(GuiGraphicsExtractor g, int slotWidth, int x, int y, PlayerInfo info) {
        TabEditor t = get(TabEditor.class);
        return t != null && t.drawPing(g, slotWidth, x, y, info);
    }
    public static boolean tabHideHeader() { TabEditor t = get(TabEditor.class); return t != null && t.hideHeader.on(); }
    public static boolean tabHideFooter() { TabEditor t = get(TabEditor.class); return t != null && t.hideFooter.on(); }

    public static int chatHistory(int original) { ChatTweaks c = get(ChatTweaks.class); return c == null ? original : Math.max(original, c.history.i()); }
    public static float chatBackground(float original) { ChatTweaks c = get(ChatTweaks.class); return c != null && c.noBackground.on() ? 0f : original; }

    public static void tooltip(Matrix3x2fStack pose) {
        ScrollableTooltips t = get(ScrollableTooltips.class);
        if (t != null) t.offset(pose);
    }

    public static boolean menuBlur() { return get(MenuBlur.class) != null; }

    /** Set by the "Accounts" button: the next title screen is the vanilla one, where other mods (Essential) put their buttons. */
    public static boolean vanillaTitleOnce;

    public static Screen screen(Screen next) {
        if (ready() && next != null) Lumen.ensureLoaded();
        if (vanillaTitleOnce && next != null && next.getClass() == TitleScreen.class) { vanillaTitleOnce = false; return next; }
        if (next != null && next.getClass() == TitleScreen.class && get(MainMenu.class) != null) return new LumenMainMenu();
        return next;
    }

    // ---- world rendering

    public static int outlineColor(int original) { BlockOverlay b = get(BlockOverlay.class); return b == null ? original : b.color(); }
    public static float outlineWidth(float original) { BlockOverlay b = get(BlockOverlay.class); return b == null ? original : b.width(); }

    public static boolean ownNametag(Object entity) {
        return entity == mc.player && get(Nametags.class) != null && !mc.options.getCameraType().isFirstPerson();
    }

    public static Component nameTag(Object entity, Component original) {
        if (entity != mc.player) return original;
        Nametags n = get(Nametags.class);
        String s = n == null ? null : n.replacement();
        return s == null ? original : Component.literal(s);
    }

    public static PlayerSkin skin(Object player, PlayerSkin skin) {
        if (skin == null) return skin;
        String cape = dev.lumen.fabric.online.Online.wear(player, "cape");
        return cape == null ? skin : CapeTextures.apply(skin, cape);
    }

    /** Copies what this player wears onto the render state; the cosmetic layers draw from that. */
    public static void outfit(Object entity, Object state) {
        if (!(state instanceof dev.lumen.fabric.cosmetics.OutfitState o)) return;
        boolean air = entity instanceof net.minecraft.world.entity.Entity e && !e.onGround();
        o.lumen$outfit(dev.lumen.fabric.online.Online.wear(entity, "wings"), dev.lumen.fabric.online.Online.wear(entity, "hat"), dev.lumen.fabric.online.Online.wear(entity, "shield"), air);
    }

    public static boolean cullEntity(Entity e, double camX, double camY, double camZ) {
        Performance p = get(Performance.class);
        return p != null && p.cull(e, camX, camY, camZ);
    }

    public static boolean cullTile(BlockEntity be) {
        Performance p = get(Performance.class);
        if (p == null || be == null) return false;
        try { return p.cullTile(be.getBlockPos().distToCenterSqr(mc.gameRenderer.mainCamera().position())); } catch (Throwable t) { return false; }
    }

    public static boolean dropParticle() { Performance p = get(Performance.class); return p != null && p.dropParticle(); }
    public static int frameLimit(int original) { Performance p = get(Performance.class); return p == null ? original : p.frameLimit(original); }

    public static float swapScale(float original) { OldAnimations o = get(OldAnimations.class); return o != null && o.noCooldownDip.on() ? 1f : original; }
    public static boolean instantReplace() { OldAnimations o = get(OldAnimations.class); return o != null && o.noReequip.on(); }

    /** @return true if the crash was handled and the game can keep running. */
    public static boolean crash(Throwable t) {
        CrashPatcher c = get(CrashPatcher.class);
        return c != null && c.handle(t);
    }

    private static int hitPixel = 0xB2FF0000;

    /** Keeps the red "damaged" row of the entity overlay texture in sync with the Hit Color mod. */
    private static void syncHitColor() {
        HitColor h = get(HitColor.class);
        int want = h == null ? 0xB2FF0000 : h.pixel();
        if (want == hitPixel || mc.gameRenderer == null) return;
        try {
            DynamicTexture tex = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).lumen$texture();
            for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) tex.getPixels().setPixel(x, y, want);
            tex.upload();
            hitPixel = want;
        } catch (Throwable t) { hitPixel = want; }
    }

    /** The module if it exists and is enabled, otherwise null. */
    private static <T extends Module> T get(Class<T> c) {
        if (!ready()) return null;
        T m = Lumen.modules.get(c);
        return m != null && m.isEnabled() ? m : null;
    }

    // ---- 2.2 additions

    public static net.minecraft.network.chat.Component chatCompact(net.minecraft.network.chat.Component c, java.util.List<net.minecraft.client.multiplayer.chat.GuiMessage> all, java.util.List<net.minecraft.client.multiplayer.chat.GuiMessage.Line> lines) {
        try { ChatTweaks t = get(ChatTweaks.class); return t == null ? c : t.compact(c, all, lines); } catch (Throwable e) { return c; }
    }

    /** Right click in the chat screen. @return true when a message was copied. */
    public static boolean chatClick(double x, double y, int button, int screenHeight) {
        ChatTweaks t = get(ChatTweaks.class);
        if (t == null || !t.copy.on() || button != InputConstants.MOUSE_BUTTON_RIGHT) return false;
        try {
            String s = ((dev.lumen.fabric.util.ChatAccess) mc.gui.hud.getChat()).lumen$messageAt(x, y, screenHeight);
            if (s == null) return false;
            mc.keyboardHandler.setClipboard(ChatTweaks.stripCounter(dev.lumen.fabric.util.Servers.strip(s)));
            mc.gui.hud.setOverlayMessage(net.minecraft.network.chat.Component.literal("Copied chat message"), false);
            return true;
        } catch (Throwable e) { return false; }
    }

    public static net.minecraft.network.chat.Component screenshot(net.minecraft.network.chat.Component c) {
        Screenshots s = get(Screenshots.class);
        return s == null ? c : s.decorate(c);
    }

    /** @return true when the command is one of ours and must not be sent to the server. */
    public static boolean localCommand(String command) {
        if (command == null || !command.startsWith(Screenshots.COMMAND + " ")) return false;
        try { Screenshots.run(command); } catch (Throwable ignored) {}
        return true;
    }

    public static void title(net.minecraft.network.chat.Component title) {
        SessionStats s = get(SessionStats.class);
        if (s != null && title != null) s.onTitle(title.getString());
    }

    private static final net.minecraft.network.chat.FontDescription MARK_FONT =
            new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("lumen", "mark"));

    public static net.minecraft.util.FormattedCharSink markSink(net.minecraft.util.FormattedCharSink sink) {
        if (!dev.lumen.fabric.online.Online.marking()) return sink;
        return (index, style, codepoint) -> sink.accept(index, codepoint == dev.lumen.fabric.online.Online.MARK
                ? style.withFont(MARK_FONT).withColor(0xFFFFFF).withBold(false).withItalic(false).withObfuscated(false) : style, codepoint);
    }

    /** The font the game's own text is drawn in: the Game Font mod's choice, or what was asked for. */
    public static net.minecraft.network.chat.FontDescription gameFont(net.minecraft.network.chat.FontDescription asked) {
        if (!ready()) return asked;
        dev.lumen.fabric.mods.GameFont f = get(dev.lumen.fabric.mods.GameFont.class);
        return f == null ? asked : f.replace(asked);
    }

    public static String nick(String s) {
        // Colour first, hide second: the Lumen+ colour then stays on whatever Nick Hider shows instead of your name.
        s = dev.lumen.fabric.online.Online.colorize(s);
        NickHider n = get(NickHider.class);
        return n == null ? s : n.apply(s);
    }

    public static boolean textTyped(net.minecraft.client.gui.components.EditBox box, int codepoint) {
        try { KoreanInput k = get(KoreanInput.class); return k != null && k.typed(box, codepoint); } catch (Throwable e) { return false; }
    }

    public static boolean textKey(net.minecraft.client.gui.components.EditBox box, int key) {
        try { KoreanInput k = get(KoreanInput.class); return k != null && k.key(box, key); } catch (Throwable e) { return false; }
    }

    /** Small language badge above a focused text box while Korean typing is on. */
    public static void textBoxDrawn(net.minecraft.client.gui.components.EditBox box, GuiGraphicsExtractor g) {
        KoreanInput k = get(KoreanInput.class);
        if (k == null || !k.korean() || !box.isFocused() || !box.isVisible()) return;
        Draw.begin(g);
        float x = box.getX() + box.getWidth() - 16, y = box.getY() - 13;
        Draw.roundRect(x, y, 14, 11, 3, 0xE04C8DFF);
        Draw.text("\uD55C", x + 7 - mc.font.width("\uD55C") / 2f, y + 1.5f, 0xFFFFFFFF, false);
    }

    /** Blur strength while a Lumen menu is open; otherwise whatever the game option says. */
    public static int blurRadius(int original) {
        if (!ready() || get(MenuBlur.class) == null) return original;
        Object screen = mc.gui.screen();
        if (!(screen instanceof dev.lumen.fabric.gui.ModMenuScreen || screen instanceof dev.lumen.fabric.gui.QuickPlayScreen)) return original;
        return dev.lumen.fabric.core.Client.blurStrength.i();
    }
}
