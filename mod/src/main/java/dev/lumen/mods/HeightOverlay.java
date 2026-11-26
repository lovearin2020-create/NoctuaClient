package dev.lumen.mods;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.lumen.core.Category;
import dev.lumen.core.Config;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Bw;
import dev.lumen.util.Util;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Darkens wool and hardened clay on the highest layer you can build on, so you can see the build
 * limit before you run into it. Hypixel does not tell the client the limit, so it is learned the
 * first time the server refuses a block on a map and remembered for that map from then on.
 */
public class HeightOverlay extends Module {
    private final Setting.Num darken = num("Darken", 45, 15, 80, 5);
    private final Setting.Col color = col("Color", 0xFF000000);
    private final Setting.Num radius = num("Radius", 24, 8, 40, 4);
    private final Setting.Bool announce = bool("Tell Me When Learned", true);

    private static final String UNKNOWN = "?";
    private Map<String, Integer> learned;
    private String map = UNKNOWN;
    private int limit = -1, attemptY = -1, ticks;
    private long attemptAt;
    private final List<long[]> marked = new ArrayList<long[]>();

    public HeightOverlay() { super("Height Overlay", "Bed Wars: darkens blocks on the highest layer you can build on.", Category.HYPIXEL); }

    private static File file() { return new File(Config.dir(), "heights.json"); }

    private Map<String, Integer> learned() {
        if (learned == null) {
            learned = new HashMap<String, Integer>();
            try {
                FileReader r = new FileReader(file());
                Map<String, Integer> m = new Gson().fromJson(r, new TypeToken<Map<String, Integer>>() {}.getType());
                r.close();
                if (m != null) learned.putAll(m);
            } catch (Exception ignored) {}
        }
        return learned;
    }

    private void save() {
        try { FileWriter w = new FileWriter(file()); new Gson().toJson(learned(), w); w.close(); } catch (Exception ignored) {}
    }

    /** The first Y the server refuses, or -1 while it is not known for this map. */
    public int limit() { return limit; }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { map = UNKNOWN; limit = -1; attemptY = -1; marked.clear(); }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Util.onHypixel()) return;
        String msg = Util.strip(e.message.getUnformattedText());
        if (Bw.playerChat(msg) || !msg.toLowerCase().contains("height limit")) return;
        if (attemptY < 0 || System.currentTimeMillis() - attemptAt > 1500) return;
        if (limit >= 0 && attemptY >= limit) return;
        limit = attemptY;
        if (!UNKNOWN.equals(map)) { learned().put(map, limit); save(); }
        if (announce.on() && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§9[Noctua] §7Build limit" + (UNKNOWN.equals(map) ? "" : " on §f" + map + "§7") + " is Y §f" + limit
                    + "§7. The top layer is darkened from now on."));
        }
        ticks = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null || !Util.onHypixel()) return;
        // where the block in hand would go, in case the server answers that it is too high
        MovingObjectPosition m = mc.objectMouseOver;
        if (mc.gameSettings.keyBindUseItem.isKeyDown() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock
                && m != null && m.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos at = m.getBlockPos();
            if (!mc.theWorld.getBlockState(at).getBlock().isReplaceable(mc.theWorld, at)) at = at.offset(m.sideHit);
            attemptY = at.getY(); attemptAt = System.currentTimeMillis();
        }
        if (ticks++ % 10 != 0) return;
        if (ticks % 40 == 1) {
            String seen = Bw.mapOnSidebar();
            if (seen != null && !seen.equals(map)) {
                map = seen;
                Integer known = learned().get(map);
                if (known != null) limit = known;
                else if (limit >= 0) { learned().put(map, limit); save(); }
            }
        }
        scan();
    }

    private static boolean tinted(Block b) { return b == Blocks.wool || b == Blocks.stained_hardened_clay; }

    /** Collects the blocks on the top layer near the player, with the faces that are open to air. */
    private void scan() {
        marked.clear();
        if (limit < 1) return;
        int y = limit - 1, r = (int) radius.f();
        int px = (int) Math.floor(mc.thePlayer.posX), pz = (int) Math.floor(mc.thePlayer.posZ);
        if (Math.abs(mc.thePlayer.posY - y) > 48) return;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = px - r; x <= px + r; x++) for (int z = pz - r; z <= pz + r; z++) {
            p.set(x, y, z);
            if (!tinted(mc.theWorld.getBlockState(p).getBlock())) continue;
            int faces = 0;
            for (EnumFacing f : EnumFacing.VALUES) {
                p.set(x + f.getFrontOffsetX(), y + f.getFrontOffsetY(), z + f.getFrontOffsetZ());
                if (!mc.theWorld.getBlockState(p).getBlock().isOpaqueCube()) faces |= 1 << f.ordinal();
            }
            if (faces != 0) marked.add(new long[]{x, z, faces});
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (marked.isEmpty() || limit < 1) return;
        Entity v = mc.getRenderViewEntity();
        if (v == null) return;
        double cx = v.lastTickPosX + (v.posX - v.lastTickPosX) * e.partialTicks, cy = v.lastTickPosY + (v.posY - v.lastTickPosY) * e.partialTicks,
                cz = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * e.partialTicks;
        int c = color.get();
        GlStateManager.pushMatrix();
        GlStateManager.translate(-cx, -cy, -cz);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, darken.f() / 100f);
        double y0 = limit - 1 - 0.003, y1 = limit + 0.003, o = 0.003;
        GL11.glBegin(GL11.GL_QUADS);
        for (long[] m : marked) {
            double x0 = m[0] - o, x1 = m[0] + 1 + o, z0 = m[1] - o, z1 = m[1] + 1 + o;
            int f = (int) m[2];
            if ((f & 1) != 0) { GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x0, y0, z1); }   // down
            if ((f & 2) != 0) { GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y1, z0); }   // up
            if ((f & 4) != 0) { GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x1, y0, z0); }   // north
            if ((f & 8) != 0) { GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1); }   // south
            if ((f & 16) != 0) { GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x0, y1, z0); }  // west
            if ((f & 32) != 0) { GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y0, z1); }  // east
        }
        GL11.glEnd();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
