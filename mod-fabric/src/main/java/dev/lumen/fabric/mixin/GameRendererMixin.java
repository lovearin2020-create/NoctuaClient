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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void lumen$hurt(CameraRenderState camera, PoseStack pose, CallbackInfo ci) { if (Hooks.noHurtCam()) ci.cancel(); }

    /** Menu Blur must not depend on the game's own "menu background blur" option, which many players set to 0. */
    @org.spongepowered.asm.mixin.injection.Redirect(method = "extractOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getMenuBackgroundBlurriness()I"))
    private int lumen$blur(net.minecraft.client.Options options) { return Hooks.blurRadius(options.getMenuBackgroundBlurriness()); }
}
