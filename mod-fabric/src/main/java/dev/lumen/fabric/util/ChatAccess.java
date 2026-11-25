package dev.lumen.fabric.util;

/** Implemented on ChatComponent by a mixin. */
public interface ChatAccess {
    /** Full text of the chat message under the given GUI position, or null. */
    String lumen$messageAt(double mouseX, double mouseY, int screenHeight);
}
