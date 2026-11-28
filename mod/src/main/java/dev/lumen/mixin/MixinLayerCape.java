package dev.lumen.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player wearing a Noctua cape shows that one only: their Minecraft or OptiFine cape is not drawn underneath it. */
@Mixin(LayerCape.class)
public abstract class MixinLayerCape {
    @Inject(method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void lumen$hideUnderOurs(AbstractClientPlayer p, float a, float b, float c, float d, float e, float f, float g, CallbackInfo ci) {
        try { if (dev.lumen.online.Online.wear(p, "cape") != null) ci.cancel(); } catch (Throwable ignored) {}
    }
}
