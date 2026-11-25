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
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void lumen$button(long window, MouseButtonInfo info, int action, CallbackInfo ci) { Hooks.mouse(info.button(), action); }

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void lumen$scroll(long window, double x, double y, CallbackInfo ci) { Hooks.scroll(y); }

    /** Runs once per frame while the game accepts world-space gizmos (hitboxes, TNT timers, block fill). */
    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void lumen$frame(CallbackInfo ci) { Hooks.frame(); }
}
