package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bed Wars currencies: what you carry, plus what is stored in the team chest and your ender chest.
 * Stored amounts are read when you open a chest and adjusted from the "deposited" chat line when you punch one.
 */
public class ResourceCount extends HudModule {
    private static final Item[] ITEMS = {Items.iron_ingot, Items.gold_ingot, Items.diamond, Items.emerald};
    private static final ItemStack[] ICONS = {new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot), new ItemStack(Items.diamond), new ItemStack(Items.emerald)};
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
    private int total, ticks;
    /** The chest block that was last right-clicked (0 team, 1 ender) and when, and which window that turned out to be. */
    private int clicked = -1, windowId = -1, windowKind = -1;
    private long clickedAt;

    public ResourceCount() { super("Resource Count", "Iron, gold, diamonds, emeralds: carried + in chests.", Category.HYPIXEL, END, 4, CENTER, -34); }

    private boolean vertical() { return layout.is("Vertical"); }
    private int cell() { return chests.on() ? 62 : 38; }
    public float width() { return vertical() ? cell() + 6 : 4 * cell() + 2; }
    public float height() { return vertical() ? 4 * 17 + 2 : 20; }

    private static int index(Item item) { for (int i = 0; i < 4; i++) if (ITEMS[i] == item) return i; return -1; }

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

    @SubscribeEvent
    public void onUse(net.minecraftforge.event.entity.player.PlayerInteractEvent e) {
        if (e.action != net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || e.pos == null || e.world == null) return;
        net.minecraft.block.Block b = e.world.getBlockState(e.pos).getBlock();
        int kind = b == net.minecraft.init.Blocks.ender_chest ? 1 : b == net.minecraft.init.Blocks.chest || b == net.minecraft.init.Blocks.trapped_chest ? 0 : -1;
        if (kind >= 0) { clicked = kind; clickedAt = System.currentTimeMillis(); }
    }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { java.util.Arrays.fill(team, 0); java.util.Arrays.fill(ender, 0); }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !chests.on()) return;
        String msg = Util.strip(e.message.getUnformattedText());
        int at = msg.toLowerCase().indexOf("deposited"), colon = msg.indexOf(": ");
        if (at < 0 || (colon > 0 && colon < at)) return; // not a deposit line, or a player typing one
        int[] d = parseDeposit(msg);
        if (d != null) { int[] box = d[2] == 1 ? ender : team; box[d[0]] = d[3] >= 0 ? d[3] : box[d[0]] + d[1]; }
    }

    /** Counted a few times a second instead of every frame. */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        // An open chest is read every tick so the last item moved before closing is never missed; the rest a few times a second.
        // Whatever screen shows it (some mods replace the chest screen), an open chest container is what counts.
        boolean chestOpen = chests.on() && mc.thePlayer.openContainer instanceof ContainerChest;
        if (++ticks % 4 != 0 && !chestOpen) return;
        java.util.Arrays.fill(inv, 0);
        total = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            int i = s == null ? -1 : index(s.getItem());
            if (i >= 0) inv[i] += s.stackSize;
        }
        if (!chestOpen) windowId = -1;
        if (chestOpen) {
            IInventory box = ((ContainerChest) mc.thePlayer.openContainer).getLowerChestInventory();
            String title = Util.strip(box.getDisplayName().getUnformattedText()).trim();
            int[] target = title.endsWith("Ender Chest") || title.equals(I18n.format("container.enderchest")) ? ender
                    : title.endsWith("Chest") || title.equals(I18n.format("container.chest")) || title.equals(I18n.format("container.chestDouble")) ? team : null;
            // The window title depends on the server and the game language, so also go by the block you are looking at:
            // a chest window opened while facing a chest block is that chest (shops are villagers, not blocks).
            net.minecraft.util.MovingObjectPosition hit = mc.objectMouseOver;
            if (target == null && hit != null && hit.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK && hit.getBlockPos() != null && mc.theWorld != null) {
                net.minecraft.block.Block b = mc.theWorld.getBlockState(hit.getBlockPos()).getBlock();
                if (b == net.minecraft.init.Blocks.ender_chest) target = ender;
                else if (b == net.minecraft.init.Blocks.chest || b == net.minecraft.init.Blocks.trapped_chest) target = team;
            }
            // Decided once per window: a chest you just right-clicked wins over the title, and the answer is kept
            // for as long as that window stays open (the crosshair may wander, the title may be anything).
            int id = mc.thePlayer.openContainer.windowId;
            if (id != windowId) {
                windowId = id;
                windowKind = System.currentTimeMillis() - clickedAt < 3000 && clicked >= 0 ? clicked : target == ender ? 1 : target == team ? 0 : -1;
                clicked = -1;
            }
            target = windowKind == 1 ? ender : windowKind == 0 ? team : null;
            if (target != null) {
                java.util.Arrays.fill(target, 0);
                for (int k = 0; k < box.getSizeInventory(); k++) {
                    ItemStack s = box.getStackInSlot(k);
                    int i = s == null ? -1 : index(s.getItem());
                    if (i >= 0) target[i] += s.stackSize;
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
        if (mc.thePlayer == null || text[0] == null) return;
        if (total == 0 && hideEmpty.on() && !editing) return;
        if (background.on()) box(0, 0, width(), height());
        for (int i = 0; i < 4; i++) {
            int x = vertical() ? 2 : 2 + i * cell(), y = vertical() ? 1 + i * 17 : 2;
            ArmorStatus.drawItem(ICONS[i], x, y);
            R.mcText(text[i], x + 19, y + 5, inv[i] == 0 ? 0xFF8891A5 : color.get(), true);
            if (chests.on()) R.mcText(stored[i], x + 20 + mc.fontRendererObj.getStringWidth(text[i]), y + 5, storedColor.get(), true);
        }
    }
}
