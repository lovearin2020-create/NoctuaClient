package dev.lumen.mixin;

import dev.lumen.mods.GameFont;
import dev.lumen.util.ReloadAccess;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinResourceManager implements ReloadAccess {
    @Shadow @Final private List<IResourceManagerReloadListener> reloadListeners;

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void lumen$font(ResourceLocation location, CallbackInfoReturnable<IResource> cir) {
        IResource r = GameFont.resource(location);
        if (r != null) cir.setReturnValue(r);
    }

    public List<?> lumen$listeners() { return reloadListeners; }
}
