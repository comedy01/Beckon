package dev.beckon.showcase.mixin;

import dev.beckon.showcase.Showcase;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererHandMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void beckonShowcase$hideHand(CallbackInfo ci) {
        if (Showcase.hideHand()) {
            ci.cancel();
        }
    }
}
