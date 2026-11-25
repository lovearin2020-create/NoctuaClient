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
import dev.lumen.fabric.cosmetics.WingsLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void lumen$layers(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererInvoker) this).lumen$addLayer(new WingsLayer((RenderLayerParent) this));
        ((LivingEntityRendererInvoker) this).lumen$addLayer(new dev.lumen.fabric.cosmetics.HatLayer((RenderLayerParent) this));
        ((LivingEntityRendererInvoker) this).lumen$addLayer(new dev.lumen.fabric.cosmetics.ShieldLayer((RenderLayerParent) this));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void lumen$outfit(Avatar entity, net.minecraft.client.renderer.entity.state.AvatarRenderState state, float partial, CallbackInfo ci) {
        Hooks.outfit(entity, state);
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void lumen$ownName(Avatar entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.ownNametag(entity)) cir.setReturnValue(true);
    }
}
