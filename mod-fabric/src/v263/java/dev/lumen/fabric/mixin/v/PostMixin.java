package dev.lumen.fabric.mixin.v;

import dev.lumen.fabric.Hooks;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Motion Blur: adds the Lumen post effect to the list the game applies after drawing the world. */
@Mixin(GameRenderer.class)
public class PostMixin {
    @Inject(method = "preparePostEffects", at = @At("HEAD"))
    private void lumen$motionBlur(List<Identifier> requested, CallbackInfo ci) {
        Identifier id = Hooks.motionBlur();
        try { if (id != null && !requested.contains(id)) requested.add(id); } catch (RuntimeException ignored) {}
        Identifier vibrance = Hooks.vibrance();
        try { if (vibrance != null && !requested.contains(vibrance)) requested.add(vibrance); } catch (RuntimeException ignored) {}
    }
}
