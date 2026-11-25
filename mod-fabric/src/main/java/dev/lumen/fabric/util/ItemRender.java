package dev.lumen.fabric.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.lumen.fabric.Lumen;
import dev.lumen.fabric.mods.ItemPhysics;
import dev.lumen.fabric.mods.Items2D;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import java.util.Map;
import java.util.WeakHashMap;

/** Dropped-item transforms for Item Physics and 2D Items. */
public final class ItemRender {
    private record Info(boolean onGround, int id) {}
    private static final Map<ItemEntityRenderState, Info> INFO = new WeakHashMap<>();

    private static ItemPhysics physics() { ItemPhysics m = Lumen.modules == null ? null : Lumen.modules.get(ItemPhysics.class); return m != null && m.isEnabled() ? m : null; }
    private static boolean flatMode() { Items2D m = Lumen.modules == null ? null : Lumen.modules.get(Items2D.class); return m != null && m.isEnabled(); }
    private static boolean flat(ItemEntityRenderState s) { return s.item.getModelBoundingBox().getZsize() <= 0.0626; }

    public static void extract(ItemEntity entity, ItemEntityRenderState state) {
        if (Lumen.modules == null) return;
        INFO.put(state, new Info(entity.onGround(), entity.getId()));
        if (flatMode()) state.count = 1;
    }

    /** Replaces the vanilla "bob up and down" translation. */
    public static void translate(PoseStack ps, float x, float y, float z, ItemEntityRenderState s) {
        Info info = INFO.get(s);
        if (physics() == null || info == null) { ps.translate(x, y, z); return; }
        if (flat(s)) ps.translate(0, info.onGround() ? 0.02f : 0.18f, 0);
        else ps.translate(0, -((float) s.item.getModelBoundingBox().minY) + 0.0625f, 0);
    }

    /** Replaces the vanilla spin. */
    public static void rotate(PoseStack ps, Quaternionfc vanilla, ItemEntityRenderState s, CameraRenderState camera) {
        Info info = INFO.get(s);
        ItemPhysics p = physics();
        if (p != null && info != null) {
            float yaw = (info.id() * 47 % 360) + s.bobOffset * 20f;
            float spin = info.onGround() ? 0 : (s.ageInTicks * 14f * p.speed.f() + s.bobOffset * 57f) % 360f;
            Draw.rotate(ps, Axis.YP.rotationDegrees(yaw));
            if (flat(s)) Draw.rotate(ps, Axis.XP.rotationDegrees(-90f + spin));
            else if (!info.onGround()) Draw.rotate(ps, new Quaternionf().rotateAxis((float) Math.toRadians(spin), 0.9285f, 0f, 0.3714f));
            return;
        }
        if (flatMode() && flat(s) && camera != null) { Draw.rotate(ps, Axis.YP.rotationDegrees(180f - camera.yRot)); return; }
        Draw.rotate(ps, vanilla);
    }
}
