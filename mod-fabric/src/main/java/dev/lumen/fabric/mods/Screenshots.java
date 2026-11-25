package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.File;

/** Adds Open / Copy Path / Delete / Folder buttons to the chat line you get after taking a screenshot. */
public class Screenshots extends Module {
    public static final String COMMAND = "lumenshot";

    public Screenshots() { super("Screenshot Manager", "Open, copy or delete a screenshot from chat.", Category.VISUAL); }

    private static File dir() { return new File(mc.gameDirectory, "screenshots"); }

    private static Component button(String label, ChatFormatting color, ClickEvent click, String hover) {
        return Component.literal(" [" + label + "]").withStyle(s -> s.withColor(color).withClickEvent(click).withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    /** Replaces the vanilla "Saved screenshot as ..." line. */
    public Component decorate(Component original) {
        try {
            if (!(original.getContents() instanceof TranslatableContents tr) || !"screenshot.success".equals(tr.getKey()) || tr.getArgs().length == 0) return original;
            Object arg = tr.getArgs()[0];
            String name = arg instanceof Component c ? c.getString() : String.valueOf(arg);
            if (!name.matches("[\\w.\\-]+\\.png")) return original;
            File f = new File(dir(), name).getAbsoluteFile();
            MutableComponent out = Component.literal("Screenshot saved");
            out.append(button("Open", ChatFormatting.GREEN, new ClickEvent.OpenFile(f), "Open " + name));
            out.append(button("Copy Path", ChatFormatting.AQUA, new ClickEvent.CopyToClipboard(f.getPath()), "Copy the file path"));
            out.append(button("Delete", ChatFormatting.RED, new ClickEvent.RunCommand("/" + COMMAND + " delete " + name), "Delete this screenshot"));
            out.append(button("Folder", ChatFormatting.YELLOW, new ClickEvent.OpenFile(dir().getAbsoluteFile()), "Open the screenshots folder"));
            return out;
        } catch (Throwable t) {
            return original;
        }
    }

    /** Handles the command behind the Delete button locally. It only ever touches files inside the screenshots folder. */
    public static void run(String command) {
        String[] a = command.split(" ");
        if (a.length != 3 || !a[1].equals("delete") || !a[2].matches("[\\w.\\-]+\\.png")) return;
        File f = new File(dir(), a[2]);
        String msg = !f.isFile() ? "That screenshot no longer exists." : f.delete() ? "Deleted " + a[2] + "." : "Could not delete " + a[2] + ".";
        mc.gui.hud.getChat().addClientSystemMessage(Component.literal("[Lumen] " + msg).withStyle(ChatFormatting.GRAY));
    }
}
