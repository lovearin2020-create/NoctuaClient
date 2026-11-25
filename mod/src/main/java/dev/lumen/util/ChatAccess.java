package dev.lumen.util;

/** Implemented on GuiNewChat by a mixin. */
public interface ChatAccess {
    /** Full text of the chat message under the given raw mouse position, or null. */
    String lumen$messageAt(int mouseX, int mouseY);
}
