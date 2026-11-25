package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockOverlay extends Module {
    private final Setting.Col outline = col("Outline Color", 0xFF4C8DFF);
    private final Setting.Num width = num("Outline Width", 2, 1, 6, 0.5);
    private final Setting.Bool fill = bool("Fill", true);
    private final Setting.Num fillOpacity = num("Fill Opacity", 20, 5, 80, 5);

    public BlockOverlay() { super("Block Overlay", "Custom block outline and fill.", Category.VISUAL); }

    public int color() { return 0xFF000000 | outline.get(); }
    public float width() { return width.f(); }

    public void onFrame(float partialTick) {
        if (!fill.on() || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        VoxelShape shape = mc.level.getBlockState(hit.getBlockPos()).getShape(mc.level, hit.getBlockPos());
        if (shape.isEmpty()) return;
        AABB box = shape.bounds().move(hit.getBlockPos()).inflate(0.002);
        int a = Math.round(fillOpacity.f() / 100f * 255f);
        Gizmos.cuboid(box, GizmoStyle.fill(a << 24 | (outline.get() & 0xFFFFFF)));
    }
}
