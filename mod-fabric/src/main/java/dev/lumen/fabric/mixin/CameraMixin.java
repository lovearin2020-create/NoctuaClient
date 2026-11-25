package dev.lumen.fabric.mixin;

import dev.lumen.fabric.Hooks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void lumen$fov(float partialTick, CallbackInfoReturnable<Float> cir) {
        float in = cir.getReturnValueF(), out = Hooks.fov(in);
        if (out != in) cir.setReturnValue(out);
    }
}
