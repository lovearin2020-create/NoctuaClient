package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Bw;
import dev.lumen.util.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Bed Wars: paints each bed's blanket in its team's colour. 1.8 beds are all red and the server
 * never says whose bed is whose, so the team is worked out: your own bed is the one you spawn
 * beside, other beds take the colour of the players standing at them when the game starts, and
 * failing that the colour most of the blocks around the bed share. A bed stays red until one of
 * those gives an answer.
 */
public class ColoredBeds extends Module {
    private final Setting.Num opacity = num("Opacity", 85, 40, 100, 5);
    private final Setting.Bool guess = bool("Guess From Nearby Blocks", true);

    private static final int REACH = 6; // chunks
    /** A bed, keyed by its foot block. */
    private static final class Bed { final BlockPos foot; final EnumFacing facing; char team; int rank; Bed(BlockPos f, EnumFacing d) { foot = f; facing = d; } }
    private final Map<BlockPos, Bed> beds = new HashMap<BlockPos, Bed>();
    private long startedAt;
    private boolean ownPending, swept;
    private int cursor, ticks, centreX, centreZ;
    private long sweptAt;

    public ColoredBeds() { super("Colored Beds", "Bed Wars: beds in their team's colour.", Category.HYPIXEL); }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { beds.clear(); startedAt = 0; ownPending = false; cursor = 0; swept = false; }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Util.onHypixel()) return;
        String msg = Util.strip(e.message.getUnformattedText());
        if (Bw.playerChat(msg) || !msg.contains(Bw.START)) return;
        startedAt = System.currentTimeMillis(); ownPending = true; cursor = 0; swept = false;
        for (Bed b : beds.values()) { b.team = 0; b.rank = 0; }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null || !Util.onHypixel()) return;
        // a few chunks a tick, so a whole pass over the area costs nothing noticeable
        // a sweep keeps the centre it started with; being moved far away (the teleport to your base) starts it over
        int side = REACH * 2 + 1, pcx = mc.thePlayer.chunkCoordX, pcz = mc.thePlayer.chunkCoordZ;
        if (cursor == 0 || Math.abs(pcx - centreX) > 2 || Math.abs(pcz - centreZ) > 2) { cursor = 0; centreX = pcx; centreZ = pcz; }
        for (int i = 0; i < 8; i++) {
            scan(centreX - REACH + cursor % side, centreZ - REACH + cursor / side);
            if (++cursor == side * side) { cursor = 0; if (!swept) { swept = true; sweptAt = System.currentTimeMillis(); } break; }
        }
        if (++ticks % 10 == 0) assign();
    }

    private void scan(int cx, int cz) {
        for (Iterator<BlockPos> it = beds.keySet().iterator(); it.hasNext(); ) { BlockPos p = it.next(); if (p.getX() >> 4 == cx && p.getZ() >> 4 == cz && !isFoot(p)) it.remove(); }
        Chunk chunk = mc.theWorld.getChunkProvider().chunkExists(cx, cz) ? mc.theWorld.getChunkFromChunkCoords(cx, cz) : null;
        if (chunk == null || chunk.isEmpty()) return;
        for (ExtendedBlockStorage s : chunk.getBlockStorageArray()) {
            if (s == null || s.isEmpty()) continue;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (s.getBlockByExtId(x, y, z) != Blocks.bed) continue;
                IBlockState st = s.get(x, y, z);
                if (st.getValue(BlockBed.PART) != BlockBed.EnumPartType.FOOT) continue;
                BlockPos p = new BlockPos((cx << 4) + x, s.getYLocation() + y, (cz << 4) + z);
                if (!beds.containsKey(p)) beds.put(p, new Bed(p, st.getValue(BlockBed.FACING)));
            }
        }
    }

    private boolean isFoot(BlockPos p) {
        IBlockState st = mc.theWorld.getBlockState(p);
        return st.getBlock() == Blocks.bed && st.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;
    }

    private boolean taken(char team, Bed except) {
        for (Bed b : beds.values()) if (b != except && b.team == team) return true;
        return false;
    }

    /** Gives a bed a team if this answer is better than the one it has (3 own spawn, 2 players, 1 blocks). */
    private void set(Bed b, char team, int rank) {
        if (team == 0 || rank <= b.rank) return;
        for (Bed o : beds.values()) if (o != b && o.team == team) { if (o.rank >= rank) return; o.team = 0; o.rank = 0; }
        b.team = team; b.rank = rank;
    }

    private void assign() {
        // nothing is decided until every bed around has been found, or a far bed could pass for the nearest
        if (!swept) return;
        long since = startedAt == 0 ? Long.MAX_VALUE : sweptAt - startedAt;
        // your own bed: the nearest one in the seconds after the game starts
        if (ownPending && since < 12000) {
            char mine = Bw.colour(mc.thePlayer.getTeam());
            Bed near = null; double best = 40 * 40;
            for (Bed b : beds.values()) { double d = mc.thePlayer.getDistanceSqToCenter(b.foot); if (d < best) { best = d; near = b; } }
            if (mine != 0 && near != null) { set(near, mine, 3); ownPending = false; }
        } else ownPending = false;
        // everyone is at their own bed right after the start
        if (since < 12000 && System.currentTimeMillis() - sweptAt < 6000) {
            for (Bed b : beds.values()) {
                if (b.rank >= 2) continue;
                int[] votes = new int[Bw.CODES.length()];
                for (EntityPlayer p : mc.theWorld.playerEntities) {
                    int t = Bw.CODES.indexOf(Bw.colour(p.getTeam()));
                    if (t >= 0 && p.getDistanceSqToCenter(b.foot) < 18 * 18) votes[t]++;
                }
                int top = top(votes, 0);
                if (top >= 0 && votes[top] > 0 && top(votes, votes[top]) < 0) set(b, Bw.CODES.charAt(top), 2);
            }
        }
        if (!guess.on()) return;
        for (Bed b : beds.values()) {
            if (b.rank >= 1) continue;
            int[] votes = census(b.foot);
            int top = top(votes, 0);
            if (top < 0) continue;
            int second = 0;
            for (int i = 0; i < votes.length; i++) if (i != top) second = Math.max(second, votes[i]);
            boolean pale = top == 5 || top == 7; // white and gray are also ordinary building blocks
            if (votes[top] >= (pale ? 16 : 6) && votes[top] >= second * 2 && !taken(Bw.CODES.charAt(top), b)) set(b, Bw.CODES.charAt(top), 1);
        }
    }

    /** The index with the most votes, or -1 when nothing has any; with {@code tie} set, another index holding that many. */
    private static int top(int[] votes, int tie) {
        int best = -1, seen = 0;
        for (int i = 0; i < votes.length; i++) {
            if (tie > 0) { if (votes[i] == tie && ++seen > 1) return i; }
            else if (votes[i] > 0 && (best < 0 || votes[i] > votes[best])) best = i;
        }
        return best;
    }

    private static int team(EnumDyeColor c) {
        switch (c) {
            case RED: return 0; case BLUE: return 1; case LIME: case GREEN: return 2; case YELLOW: return 3;
            case CYAN: case LIGHT_BLUE: return 4; case WHITE: return 5; case PINK: return 6; case GRAY: return 7; default: return -1;
        }
    }

    private int[] census(BlockPos c) {
        int[] votes = new int[Bw.CODES.length()];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -8; x <= 8; x++) for (int y = -4; y <= 6; y++) for (int z = -8; z <= 8; z++) {
            p.set(c.getX() + x, c.getY() + y, c.getZ() + z);
            IBlockState st = mc.theWorld.getBlockState(p);
            Block b = st.getBlock();
            EnumDyeColor dye = b == Blocks.wool || b == Blocks.stained_hardened_clay ? st.getValue(BlockColored.COLOR) : b == Blocks.stained_glass ? st.getValue(BlockStainedGlass.COLOR)
                    : b == Blocks.carpet ? st.getValue(BlockCarpet.COLOR) : null;
            int t = dye == null ? -1 : team(dye);
            if (t >= 0) votes[t]++;
        }
        return votes;
    }

    /** For tests and the like: the team of the bed whose foot is here, or 0. */
    public char teamAt(BlockPos foot) { Bed b = beds.get(foot); return b == null ? 0 : b.team; }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (beds.isEmpty()) return;
        Entity v = mc.getRenderViewEntity();
        if (v == null) return;
        double cx = v.lastTickPosX + (v.posX - v.lastTickPosX) * e.partialTicks, cy = v.lastTickPosY + (v.posY - v.lastTickPosY) * e.partialTicks,
                cz = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * e.partialTicks;
        boolean any = false;
        for (Bed b : beds.values()) {
            if (b.team == 0) continue;
            if (!any) {
                any = true;
                GlStateManager.pushMatrix();
                GlStateManager.translate(-cx, -cy, -cz);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.disableCull();
                GlStateManager.depthMask(false);
                GL11.glBegin(GL11.GL_QUADS);
            }
            // the blanket: the whole foot block and the half of the head block next to it, leaving the pillow
            BlockPos f = b.foot;
            double o = 0.004, x0 = f.getX() - o, x1 = f.getX() + 1 + o, z0 = f.getZ() - o, z1 = f.getZ() + 1 + o, half = 0.5;
            switch (b.facing) {
                case NORTH: z0 -= half; break; case SOUTH: z1 += half; break; case WEST: x0 -= half; break; default: x1 += half;
            }
            double y0 = f.getY() + 3 / 16.0, y1 = f.getY() + 9 / 16.0 + o;
            int c = Bw.rgb(b.team);
            float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, bl = (c & 255) / 255f, a = opacity.f() / 100f;
            GL11.glColor4f(r, g, bl, a);
            GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y1, z0);
            GL11.glColor4f(r * 0.8f, g * 0.8f, bl * 0.8f, a);
            GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x1, y0, z0);
            GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1);
            GL11.glColor4f(r * 0.65f, g * 0.65f, bl * 0.65f, a);
            GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x0, y1, z0);
            GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y0, z1);
        }
        if (!any) return;
        GL11.glEnd();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
