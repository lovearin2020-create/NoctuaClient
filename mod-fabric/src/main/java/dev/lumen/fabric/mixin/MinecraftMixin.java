package dev.lumen.fabric.mixin;

import dev.lumen.fabric.Hooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow private void runTick(boolean advanceGameTime) {}

    @Inject(method = "tick", at = @At("TAIL"))
    private void lumen$tick(CallbackInfo ci) { Hooks.tick(); }

    /** Crash Patcher: a crash inside one frame is reported and recovered from instead of closing the game. */
    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"))
    private void lumen$guardedTick(Minecraft self, boolean advanceGameTime) {
        try {
            this.runTick(advanceGameTime);
        } catch (RuntimeException e) {
            if (!Hooks.crash(e)) throw e;
        }
    }
}
