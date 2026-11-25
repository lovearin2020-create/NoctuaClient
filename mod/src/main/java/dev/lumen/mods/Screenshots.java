package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;

/** Adds Open / Copy / Delete / Folder buttons to the chat line you get after taking a screenshot. */
public class Screenshots extends Module {
    public static Screenshots INSTANCE;

    public Screenshots() { super("Screenshot Manager", "Open, copy or delete a screenshot from chat.", Category.VISUAL); INSTANCE = this; }

    private static File dir() { return new File(mc.mcDataDir, "screenshots"); }

    private static IChatComponent button(String label, EnumChatFormatting color, ClickEvent click, String hover) {
        ChatComponentText t = new ChatComponentText(" [" + label + "]");
        t.getChatStyle().setColor(color).setChatClickEvent(click)
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(hover)));
        return t;
    }

    /** Replaces the vanilla "Saved screenshot as ..." line. */
    public IChatComponent decorate(IChatComponent original) {
        try {
            if (!(original instanceof ChatComponentTranslation)) return original;
            ChatComponentTranslation tr = (ChatComponentTranslation) original;
            if (!"screenshot.success".equals(tr.getKey()) || tr.getFormatArgs().length == 0) return original;
            Object arg = tr.getFormatArgs()[0];
            String name = arg instanceof IChatComponent ? ((IChatComponent) arg).getUnformattedText() : String.valueOf(arg);
            if (!name.matches("[\\w.\\-]+\\.png")) return original;
            File f = new File(dir(), name);
            IChatComponent out = new ChatComponentText("Screenshot saved");
            out.appendSibling(button("Open", EnumChatFormatting.GREEN, new ClickEvent(ClickEvent.Action.OPEN_FILE, f.getAbsolutePath()), "Open " + name));
            out.appendSibling(button("Copy", EnumChatFormatting.AQUA, new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lumenshot copy " + name), "Copy the image to the clipboard"));
            out.appendSibling(button("Delete", EnumChatFormatting.RED, new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lumenshot delete " + name), "Delete this screenshot"));
            out.appendSibling(button("Folder", EnumChatFormatting.YELLOW, new ClickEvent(ClickEvent.Action.OPEN_FILE, dir().getAbsolutePath()), "Open the screenshots folder"));
            return out;
        } catch (Throwable t) {
            return original;
        }
    }

    static void say(String s) {
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText("§7[Lumen] " + s));
    }

    static void copy(final File f) {
        new Thread(new Runnable() {
            public void run() {
                String result;
                try {
                    final Image img = javax.imageio.ImageIO.read(f);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
                        public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
                        public boolean isDataFlavorSupported(DataFlavor fl) { return DataFlavor.imageFlavor.equals(fl); }
                        public Object getTransferData(DataFlavor fl) { return img; }
                    }, null);
                    result = "Screenshot copied to the clipboard.";
                } catch (Throwable t) {
                    result = "Could not copy the screenshot: " + t.getClass().getSimpleName();
                }
                final String msg = result;
                mc.addScheduledTask(new Runnable() { public void run() { say(msg); } });
            }
        }, "Lumen-ScreenshotCopy").start();
    }

    /** Client-side command behind the chat buttons. It only ever touches files inside the screenshots folder. */
    public static class Command extends CommandBase {
        public String getCommandName() { return "lumenshot"; }
        public String getCommandUsage(ICommandSender s) { return "/lumenshot <copy|delete> <file>"; }
        public int getRequiredPermissionLevel() { return 0; }
        public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
        public void processCommand(ICommandSender s, String[] a) {
            if (a.length != 2 || !a[1].matches("[\\w.\\-]+\\.png")) return;
            File f = new File(dir(), a[1]);
            if (!f.isFile()) { say("That screenshot no longer exists."); return; }
            if (a[0].equals("copy")) copy(f);
            else if (a[0].equals("delete")) say(f.delete() ? "Deleted " + a[1] + "." : "Could not delete " + a[1] + ".");
        }
    }
}
