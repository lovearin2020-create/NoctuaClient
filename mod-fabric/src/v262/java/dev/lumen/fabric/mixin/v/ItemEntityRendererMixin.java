package dev.lumen.fabric.mixin.v;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lumen.fabric.util.ItemRender;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item Physics and 2D Items: replaces the hover and spin of dropped items. */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void lumen$extract(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) { ItemRender.extract(entity, state); }

    @Redirect(method = SUBMIT, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void lumen$translate(PoseStack ps, float x, float y, float z, ItemEntityRenderState state, PoseStack p2, SubmitNodeCollector out, CameraRenderState camera) {
        ItemRender.translate(ps, x, y, z, state);
    }

    @Redirect(method = SUBMIT, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void lumen$rotate(PoseStack ps, Quaternionfc spin, ItemEntityRenderState state, PoseStack p2, SubmitNodeCollector out, CameraRenderState camera) {
        ItemRender.rotate(ps, spin, state, camera);
    }
}
