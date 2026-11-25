package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField {
    @Inject(method = "textboxKeyTyped", at = @At("HEAD"), cancellable = true)
    private void lumen$hangul(char c, int key, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.textKey((GuiTextField) (Object) this, c, key)) cir.setReturnValue(true);
    }
}
