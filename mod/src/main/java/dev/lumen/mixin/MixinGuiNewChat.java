package dev.lumen.mixin;

import dev.lumen.Hooks;
import dev.lumen.util.ChatAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat implements ChatAccess {
    @Shadow @Final private Minecraft mc;
    @Shadow @Final private List<ChatLine> chatLines;
    @Shadow @Final private List<ChatLine> drawnChatLines;
    @Shadow private int scrollPos;
    @Shadow public abstract boolean getChatOpen();
    @Shadow public abstract float getChatScale();
    @Shadow public abstract int getChatWidth();
    @Shadow public abstract int getLineCount();

    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int lumen$history(int original) { return Hooks.chatHistory(original); }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V", ordinal = 0))
    private void lumen$background(int left, int top, int right, int bottom, int color) {
        if (!Hooks.chatNoBackground()) Gui.drawRect(left, top, right, bottom, color);
    }

    @Inject(method = "printChatMessage", at = @At("HEAD"), cancellable = true)
    private void lumen$compact(IChatComponent c, CallbackInfo ci) {
        if (Hooks.chatPrint((GuiNewChat) (Object) this, c)) ci.cancel();
    }

    public String lumen$messageAt(int mouseX, int mouseY) {
        if (!getChatOpen()) return null;
        int sf = new ScaledResolution(mc).getScaleFactor();
        float cs = getChatScale();
        int x = MathHelper.floor_float((mouseX / sf - 3) / cs), y = MathHelper.floor_float((mouseY / sf - 27) / cs);
        if (x < 0 || y < 0) return null;
        int shown = Math.min(getLineCount(), drawnChatLines.size());
        int fh = mc.fontRendererObj.FONT_HEIGHT;
        if (x > MathHelper.floor_float(getChatWidth() / cs) || y >= fh * shown + shown) return null;
        int idx = y / fh + scrollPos;
        if (idx < 0 || idx >= drawnChatLines.size()) return null;
        ChatLine line = drawnChatLines.get(idx);
        String part = line.getChatComponent().getUnformattedText();
        for (ChatLine full : chatLines) {
            if (full.getUpdatedCounter() != line.getUpdatedCounter() || full.getChatLineID() != line.getChatLineID()) continue;
            String text = full.getChatComponent().getUnformattedText();
            if (text.contains(part.trim())) return text;
        }
        return part;
    }
}
