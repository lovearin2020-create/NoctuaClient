package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FoamFix-style memory saver: identical vertex arrays of baked block/item models are shared
 * instead of being kept once per model, and unused memory is released when you leave a world.
 */
public class MemoryOptimizer extends Module {
    private final Setting.Bool dedupe = bool("Deduplicate Model Data", true);
    private final Setting.Bool gcOnLeave = bool("Free Memory On World Leave", true);
    private long lastGc;

    public MemoryOptimizer() { super("Memory Optimizer", "Shares duplicate model data, frees memory.", Category.PERFORMANCE); }

    private static final class Key {
        final int[] data; final int hash;
        Key(int[] d) { data = d; hash = Arrays.hashCode(d); }
        public int hashCode() { return hash; }
        public boolean equals(Object o) { return o instanceof Key && Arrays.equals(data, ((Key) o).data); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBake(ModelBakeEvent e) {
        if (!dedupe.on()) return;
        try {
            Field f = ReflectionHelper.findField(BakedQuad.class, "vertexData", "field_178215_a");
            Map<Key, int[]> pool = new HashMap<Key, int[]>();
            int shared = 0;
            for (IBakedModel model : e.modelRegistry) {
                if (model == null) continue;
                try {
                    shared += intern(model.getGeneralQuads(), f, pool);
                    for (EnumFacing face : EnumFacing.values()) shared += intern(model.getFaceQuads(face), f, pool);
                } catch (Throwable ignored) {}
            }
            System.out.println("[Noctua] Memory Optimizer shared " + shared + " duplicate model arrays (" + pool.size() + " unique).");
        } catch (Throwable t) {
            System.err.println("[Noctua] Memory Optimizer skipped: " + t);
        }
    }

    private static int intern(List<BakedQuad> quads, Field f, Map<Key, int[]> pool) throws IllegalAccessException {
        if (quads == null) return 0;
        int n = 0;
        for (BakedQuad q : quads) {
            if (q == null || q.getClass() != BakedQuad.class) continue;
            int[] data = (int[]) f.get(q);
            if (data == null) continue;
            Key k = new Key(data);
            int[] existing = pool.get(k);
            if (existing == null) pool.put(k, data);
            else if (existing != data) { f.set(q, existing); n++; }
        }
        return n;
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload e) {
        if (!gcOnLeave.on() || !e.world.isRemote) return;
        long now = System.currentTimeMillis();
        if (now - lastGc < 30000) return;
        lastGc = now;
        System.gc();
    }
}
