package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ChatTweaks extends Module {
    public final Setting.Bool noBackground = bool("Transparent Background", true);
    public final Setting.Num history = num("History Length", 1000, 100, 5000, 100);
    public final Setting.Bool compact = bool("Compact Duplicate Messages", true);
    public final Setting.Bool copy = bool("Right Click To Copy", true);

    private String lastKey;
    private Component lastShown;
    private int count;
    private long lastTime;

    public ChatTweaks() { super("Chat Tweaks", "Clear background, longer history, stacked duplicates, right click copy.", Category.VISUAL); }

    /** Called before a message is added: a repeat removes the line before it and comes back with a counter. */
    public Component compact(Component c, List<GuiMessage> all, List<GuiMessage.Line> lines) {
        if (!compact.on() || c == null) return c;
        String key = c.getString();
        long now = System.currentTimeMillis();
        Component out = c;
        if (key.equals(lastKey) && now - lastTime < 60000 && !key.trim().isEmpty() && !all.isEmpty() && all.get(0).content() == lastShown) {
            count++;
            GuiMessage prev = all.remove(0);
            lines.removeIf(l -> l.parent() == prev);
            out = Component.empty().append(c).append(Component.literal(" [x" + count + "]").withStyle(ChatFormatting.GRAY));
        } else count = 1;
        lastKey = key; lastTime = now; lastShown = out;
        return out;
    }

    /** Removes the counter this module adds, so other mods see the message as the server sent it. */
    public static String stripCounter(String plain) {
        return plain.endsWith("]") ? plain.replaceFirst(" \\[x\\d+\\]$", "") : plain;
    }
}
