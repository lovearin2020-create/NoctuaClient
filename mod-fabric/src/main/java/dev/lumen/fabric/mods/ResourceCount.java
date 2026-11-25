package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.util.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bed Wars currencies: what you carry, plus what is stored in the team chest and your ender chest.
 * Stored amounts are read when you open a chest and adjusted from the "deposited" chat line when you punch one.
 */
public class ResourceCount extends HudModule {
    private static final Item[] ITEMS = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD};
    private ItemStack[] icons; // made on first draw: item stacks cannot exist before the game has finished loading
    private static final String[] WORDS = {"iron", "gold", "diamond", "emerald"};
    /** Hypixel: "Deposited x36 Iron Ingot into Ender Chest! (52 Total)". Other wordings around the same pieces are accepted too. */
    static final Pattern DEPOSIT = Pattern.compile("deposited\\s+(?:x\\s*)?(\\d+)?\\s*x?\\s*([A-Za-z ]+?)\\s*(?:x\\s*(\\d+))?\\s+(?:in|into|to)\\s+(?:your |the |team )*(ender ?chest|team ?chest|chest)", Pattern.CASE_INSENSITIVE);

    static final Pattern TOTAL = Pattern.compile("\\((\\d+)\\s*Total\\)", Pattern.CASE_INSENSITIVE);

    private final Setting.Mode layout = mode("Layout", "Vertical", "Vertical", "Horizontal");
    private final Setting.Bool chests = bool("Count Chests", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool hideEmpty = bool("Hide When Empty", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private final Setting.Col storedColor = col("Stored Color", 0xFF9AA3B5);

    private final int[] inv = new int[4], team = new int[4], ender = new int[4];
    private final String[] text = new String[4], stored = new String[4];
    private int total, ticks, windowId = -1, windowKind = -1;
    private Object world;
    private String chestName, enderName, doubleName;

    public ResourceCount() { super("Resource Count", "Iron, gold, diamonds, emeralds: carried + in chests.", Category.HYPIXEL, END, 4, CENTER, -34); }

    private boolean vertical() { return layout.is("Vertical"); }
    private int cell() { return chests.on() ? 62 : 38; }
    public float width() { return vertical() ? cell() + 6 : 4 * cell() + 2; }
    public float height() { return vertical() ? 4 * 17 + 2 : 20; }

    private static int index(ItemStack s) { if (!s.isEmpty()) for (int i = 0; i < 4; i++) if (s.getItem() == ITEMS[i]) return i; return -1; }

    /** Parses a deposit line. @return {resource index, amount, 0 team / 1 ender, new stored total or -1} or null. */
    public static int[] parseDeposit(String msg) {
        Matcher m = DEPOSIT.matcher(msg);
        if (!m.find()) return null;
        String n = m.group(1) != null ? m.group(1) : m.group(3);
        String item = m.group(2).toLowerCase();
        int idx = -1;
        for (int i = 0; i < 4; i++) if (item.contains(WORDS[i])) idx = i;
        if (idx < 0) return null;
        int amount;
        try { amount = n == null ? 1 : Integer.parseInt(n); } catch (NumberFormatException e) { return null; }
        // Hypixel adds "(52 Total)": what is now stored of that item, which is exact where adding up would drift.
        Matcher t = TOTAL.matcher(msg);
        int total = -1;
        if (t.find()) try { total = Integer.parseInt(t.group(1)); } catch (NumberFormatException ignored) {}
        return new int[]{idx, amount, m.group(4).toLowerCase().startsWith("ender") ? 1 : 0, total};
    }

    public boolean onChat(String raw) {
        if (!chests.on()) return false;
        String msg = Servers.strip(raw);
        int at = msg.toLowerCase().indexOf("deposited"), colon = msg.indexOf(": ");
        if (at < 0 || (colon > 0 && colon < at)) return false; // a player typing it
        int[] d = parseDeposit(msg);
        if (d != null) { int[] box = d[2] == 1 ? ender : team; box[d[0]] = d[3] >= 0 ? d[3] : box[d[0]] + d[1]; }
        return false;
    }

    /** Counted a few times a second instead of every frame. */
    public void onTick() {
        if (mc.level != world) { world = mc.level; Arrays.fill(team, 0); Arrays.fill(ender, 0); }
        if (mc.player == null) return;
        // An open chest is read every tick so the last item moved before closing is never missed; the rest a few times a second.
        // Whatever screen shows it (some mods replace the chest screen), an open chest container is what counts.
        boolean chestOpen = chests.on() && mc.player.containerMenu instanceof ChestMenu;
        if (++ticks % 4 != 0 && !chestOpen) return;
        Arrays.fill(inv, 0);
        total = 0;
        var pinv = mc.player.getInventory();
        for (int k = 0; k < pinv.getContainerSize(); k++) {
            int i = index(pinv.getItem(k));
            if (i >= 0) inv[i] += pinv.getItem(k).getCount();
        }
        if (!chestOpen) windowId = -1;
        if (chestOpen && mc.player.containerMenu instanceof ChestMenu menu) {
            if (chestName == null) {
                chestName = Component.translatable("container.chest").getString();
                enderName = Component.translatable("container.enderchest").getString();
                doubleName = Component.translatable("container.chestDouble").getString();
            }
            String title = mc.gui.screen() == null ? "" : Servers.strip(mc.gui.screen().getTitle().getString()).trim();
            int[] target = title.endsWith("Ender Chest") || title.equals(enderName) ? ender
                    : title.endsWith("Chest") || title.equals(chestName) || title.equals(doubleName) ? team : null;
            // The window title depends on the server and the game language, so also go by the block you are looking at:
            // a chest window opened while facing a chest block is that chest (shops are villagers, not blocks).
            if (target == null && mc.level != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
                var b = mc.level.getBlockState(hit.getBlockPos()).getBlock();
                if (b instanceof net.minecraft.world.level.block.EnderChestBlock) target = ender;
                else if (b instanceof net.minecraft.world.level.block.ChestBlock) target = team;
            }
            // Decided once per window and kept while it is open: the crosshair may wander and the title may be anything.
            if (menu.containerId != windowId) { windowId = menu.containerId; windowKind = target == ender ? 1 : target == team ? 0 : -1; }
            target = windowKind == 1 ? ender : windowKind == 0 ? team : null;
            if (target != null) {
                Arrays.fill(target, 0);
                Container box = menu.getContainer();
                for (int k = 0; k < box.getContainerSize(); k++) {
                    int i = index(box.getItem(k));
                    if (i >= 0) target[i] += box.getItem(k).getCount();
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            int st = team[i] + ender[i];
            total += inv[i] + (chests.on() ? st : 0);
            text[i] = String.valueOf(inv[i]);
            stored[i] = "+" + st;
        }
    }

    public void draw(boolean editing) {
        if (mc.player == null || text[0] == null) return;
        if (total == 0 && hideEmpty.on() && !editing) return;
        if (background.on()) Draw.roundRect(0, 0, width(), height(), 3, 0x70000000);
        if (icons == null) icons = new ItemStack[]{new ItemStack(ITEMS[0]), new ItemStack(ITEMS[1]), new ItemStack(ITEMS[2]), new ItemStack(ITEMS[3])};
        for (int i = 0; i < 4; i++) {
            int x = vertical() ? 2 : 2 + i * cell(), y = vertical() ? 1 + i * 17 : 2;
            Draw.item(icons[i], x, y, false);
            Draw.text(text[i], x + 19, y + 5, inv[i] == 0 ? 0xFF8891A5 : color.get(), true);
            if (chests.on()) Draw.text(stored[i], x + 20 + mc.font.width(text[i]), y + 5, storedColor.get(), true);
        }
    }
}
