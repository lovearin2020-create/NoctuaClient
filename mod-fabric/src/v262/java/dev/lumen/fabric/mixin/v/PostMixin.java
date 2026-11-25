package dev.lumen.fabric.mixin.v;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import dev.lumen.fabric.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Motion Blur: runs the Lumen post effect right after the world has been drawn. */
@Mixin(GameRenderer.class)
public class PostMixin {
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void lumen$motionBlur(CallbackInfo ci) {
        lumen$run(Hooks.motionBlur());
        lumen$run(Hooks.vibrance());
    }

    private void lumen$run(Identifier id) {
        if (id == null) return;
        try {
            PostChain chain = this.minecraft.getShaderManager().getPostChain(id, LevelTargetBundle.MAIN_TARGETS);
            if (chain != null) chain.process(this.mainRenderTarget, this.resourcePool);
        } catch (RuntimeException ignored) {}
    }
}
