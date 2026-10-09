package dev.beckon.selftest.mixin;

import dev.beckon.selftest.BeckonSelfTest;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererHandMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void beckonSelftest$hideHand(CallbackInfo ci) {
        if (BeckonSelfTest.hideHud) {
            ci.cancel();
        }
    }
}
