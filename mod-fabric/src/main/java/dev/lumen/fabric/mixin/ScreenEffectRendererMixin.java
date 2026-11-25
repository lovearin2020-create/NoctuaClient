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
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "submitFire", at = @At("HEAD"))
    private static void lumen$firePre(PoseStack pose, SubmitNodeCollector out, TextureAtlasSprite sprite, CallbackInfo ci) {
        pose.pushPose();
        pose.translate(0f, -Hooks.fireLower(), 0f);
    }

    @Inject(method = "submitFire", at = @At("RETURN"))
    private static void lumen$firePost(PoseStack pose, SubmitNodeCollector out, TextureAtlasSprite sprite, CallbackInfo ci) { pose.popPose(); }
}
