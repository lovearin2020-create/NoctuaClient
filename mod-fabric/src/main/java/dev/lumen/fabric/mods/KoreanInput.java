package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;

/** Types Hangul inside the game without the system input method. The toggle key switches between English and Korean. */
public class KoreanInput extends Module {
    private final Setting.Bool blockIme = bool("Turn Off Windows IME In Game", true); // only used by the 1.8.9 build; kept so settings stay shared
    private final Setting.Bool indicator = bool("Show Indicator", true);

    private final Hangul hangul = new Hangul();
    private boolean korean;
    private EditBox field;
    private String expectText;
    private int expectCursor;

    public KoreanInput() { super("Korean Input", "Type Hangul without the Windows IME. The language key is set in Settings.", Category.VISUAL); }

    private boolean isToggle(int key) { return Client.keyKorean.is(key); }

    public boolean onKey(KeyEvent e, int action) {
        if (action != 1 || mc.gui.screen() == null || !isToggle(e.key())) return false;
        hangul.reset(); korean = !korean;
        return true;
    }

    public boolean korean() { return korean && indicator.on(); }

    private void remember(EditBox f) { field = f; expectText = f.getValue(); expectCursor = f.getCursorPosition(); }
    private void sync(EditBox f) {
        if (f != field || !f.getValue().equals(expectText) || f.getCursorPosition() != expectCursor || !f.getHighlighted().isEmpty()) hangul.reset();
    }

    /** A character is about to be typed into {@code f}. @return true if it was consumed. */
    public boolean typed(EditBox f, int codepoint) {
        if (!korean || !f.canConsumeInput() || codepoint > 0xFFFF) return false;
        sync(f);
        char j = Hangul.jamo((char) codepoint);
        if (j == 0) { hangul.reset(); return false; }
        boolean had = hangul.composing();
        hangul.add(j);
        if (had) f.deleteChars(-1);
        String cur = hangul.current();
        f.insertText(hangul.commit + cur);
        String t = f.getValue();
        int cp = f.getCursorPosition();
        if (cp < cur.length() || !t.substring(cp - cur.length(), cp).equals(cur)) hangul.reset(); // the field refused it (full)
        remember(f);
        return true;
    }

    /** A key is about to be handled by {@code f}. @return true if it was consumed. */
    public boolean key(EditBox f, int key) {
        if (!korean || !f.canConsumeInput()) return false;
        sync(f);
        // Every letter is a key press first and a typed character second; only Backspace is ours here. Anything that
        // really moves the cursor or changes the text is noticed by sync() on the next key.
        if (key != InputConstants.KEY_BACKSPACE) return false;
        if (!hangul.composing()) return false;
        f.deleteChars(-1);
        hangul.backspace();
        f.insertText(hangul.current());
        remember(f);
        return true;
    }

    protected void onDisable() { hangul.reset(); korean = false; }
}
