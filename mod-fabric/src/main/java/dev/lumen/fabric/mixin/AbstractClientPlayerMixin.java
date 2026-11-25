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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void lumen$fov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        float in = cir.getReturnValueF(), out = Hooks.fovModifier(in);
        if (out != in) cir.setReturnValue(out);
    }

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void lumen$skin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin in = cir.getReturnValue(), out = Hooks.skin(this, in);
        if (out != in) cir.setReturnValue(out);
    }
}
