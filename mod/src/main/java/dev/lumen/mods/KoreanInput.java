package dev.lumen.mods;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Hangul;
import dev.lumen.util.R;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Method;

/**
 * Types Hangul inside the game. The Windows input method is switched off for the game window (it is
 * what makes fullscreen flicker), and the toggle key switches between English and Korean here instead.
 */
public class KoreanInput extends Module {
    public static KoreanInput INSTANCE;
    private final Setting.Bool blockIme = bool("Turn Off Windows IME In Game", true);
    private final Setting.Bool indicator = bool("Show Indicator", true);

    private final Hangul hangul = new Hangul();
    private boolean korean;
    private GuiTextField field;
    private String expectText;
    private int expectCursor;
    private GuiScreen usedIn;

    public KoreanInput() { super("Korean Input", "Type Hangul without the Windows IME. The language key is set in Settings.", Category.VISUAL); INSTANCE = this; }

    private boolean isToggle(int key) {
        return key == Keyboard.KEY_KANA /* the Han/Yeong key */ || dev.lumen.core.Client.keyKorean.is(key);
    }

    private void remember(GuiTextField f) { field = f; expectText = f.getText(); expectCursor = f.getCursorPosition(); usedIn = mc.currentScreen; }

    /** Called before a text field handles a key. @return true if the key was consumed. */
    public boolean key(GuiTextField f, char c, int key) {
        if (!f.isFocused()) return false;
        if (isToggle(key)) { hangul.reset(); korean = !korean; usedIn = mc.currentScreen; return true; }
        if (!korean) return false;
        if (f != field || !f.getText().equals(expectText) || f.getCursorPosition() != expectCursor || f.getSelectionEnd() != f.getCursorPosition()) hangul.reset();
        if (GuiScreen.isCtrlKeyDown() || GuiScreen.isAltKeyDown()) { hangul.reset(); return false; }
        if (key == Keyboard.KEY_BACK) {
            if (!hangul.composing()) return false;
            f.deleteFromCursor(-1);
            hangul.backspace();
            f.writeText(hangul.current());
            remember(f);
            return true;
        }
        // Shift arrives as a key of its own before the letter it shifts; it must not end the syllable (있 = ㅇ ㅣ Shift+ㅅ).
        if (key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT || key == Keyboard.KEY_CAPITAL) return false;
        char j = Hangul.jamo(c);
        if (j == 0) { hangul.reset(); return false; }
        boolean had = hangul.composing();
        hangul.add(j);
        if (had) f.deleteFromCursor(-1);
        String cur = hangul.current();
        f.writeText(hangul.commit + cur);
        String t = f.getText();
        int cp = f.getCursorPosition();
        if (cp < cur.length() || !t.substring(cp - cur.length(), cp).equals(cur)) hangul.reset(); // the field refused it (full)
        remember(f);
        return true;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (!indicator.on() || !(e.gui instanceof GuiChat || e.gui == usedIn)) return;
        boolean chat = e.gui instanceof GuiChat;
        float x = chat ? e.gui.width - 20 : 2, y = chat ? e.gui.height - 27 : 2;
        R.roundRect(x, y, 18, 11, 3, korean ? 0xE04C8DFF : 0xA0000000);
        String s = korean ? "한" : "EN";
        R.mcText(s, x + 9 - mc.fontRendererObj.getStringWidth(s) / 2f, y + 1.5f, 0xFFFFFFFF, false);
        R.reset();
    }

    // ---- Windows IME ----

    public interface Imm32 extends StdCallLibrary {
        boolean ImmAssociateContextEx(Pointer hwnd, Pointer himc, int flags);
    }

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private Imm32 imm;
    private boolean imeFailed;
    private long blockedHwnd;
    private int ticks;

    private static long hwnd() throws Exception {
        Method impl = Display.class.getDeclaredMethod("getImplementation");
        impl.setAccessible(true);
        Object d = impl.invoke(null);
        Method m = d.getClass().getDeclaredMethod("getHwnd");
        m.setAccessible(true);
        return ((Number) m.invoke(d)).longValue();
    }

    private void ime(boolean block) {
        if (!WINDOWS || imeFailed) return;
        try {
            if (imm == null) imm = (Imm32) Native.loadLibrary("imm32", Imm32.class);
            long h = hwnd();
            if (h == 0 || (block && h == blockedHwnd)) return;
            // No context = no input method for this window; 0x10 (IACE_DEFAULT) gives the normal one back.
            imm.ImmAssociateContextEx(new Pointer(h), null, block ? 0 : 0x10);
            blockedHwnd = block ? h : 0;
        } catch (Throwable t) {
            imeFailed = true;
            System.err.println("[Noctua] Could not change the Windows IME state: " + t);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++ticks % 20 != 0) return;
        // Checked every second because switching fullscreen makes a new window.
        if (blockIme.on()) ime(true); else if (blockedHwnd != 0) ime(false);
    }

    protected void onDisable() { hangul.reset(); korean = false; if (blockedHwnd != 0) ime(false); }
}
