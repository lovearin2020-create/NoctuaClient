package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.ChatAccess;
import dev.lumen.util.Util;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class ChatTweaks extends Module {
    public static ChatTweaks INSTANCE;
    public final Setting.Bool noBackground = bool("Transparent Background", true);
    public final Setting.Num history = num("History Length", 1000, 100, 5000, 100);
    public final Setting.Bool compact = bool("Compact Duplicate Messages", true);
    public final Setting.Bool copy = bool("Right Click To Copy", true);

    private static final int ID_BASE = 0x4C550000;
    private String lastKey;
    private int lastId, count, nextId;
    private long lastTime;

    public ChatTweaks() {
        super("Chat Tweaks", "Clear background, longer history, stacked duplicates, right click copy.", Category.VISUAL);
        INSTANCE = this;
    }

    /** Prints the message ourselves so a repeat can replace the line before it. @return true if it was handled. */
    public boolean print(GuiNewChat chat, IChatComponent c) {
        if (!compact.on() || c == null) return false;
        String key = c.getFormattedText();
        long now = System.currentTimeMillis();
        IChatComponent out = c;
        if (key.equals(lastKey) && now - lastTime < 60000 && !Util.strip(key).trim().isEmpty()) {
            count++;
            chat.deleteChatLine(lastId);
            out = c.createCopy().appendSibling(new ChatComponentText(" §7[x" + count + "]"));
        } else count = 1;
        lastKey = key; lastTime = now;
        lastId = ID_BASE + (nextId++ & 0xFFFF);
        chat.printChatMessageWithOptionalDeletion(out, lastId);
        return true;
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        if (!copy.on() || !(e.gui instanceof GuiChat) || Mouse.getEventButton() != 1 || !Mouse.getEventButtonState()) return;
        try {
            String s = ((ChatAccess) mc.ingameGUI.getChatGUI()).lumen$messageAt(Mouse.getX(), Mouse.getY());
            if (s == null) return;
            GuiScreen.setClipboardString(Util.strip(s));
            mc.ingameGUI.setRecordPlaying("Copied chat message", false);
            e.setCanceled(true);
        } catch (Throwable ignored) {}
    }
}
