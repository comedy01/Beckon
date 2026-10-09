package dev.beckon.selftest.mixin;

import dev.beckon.selftest.BeckonSelfTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftFrameMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void beckonSelftest$frame(boolean advanceGameTime, CallbackInfo ci) {
        BeckonSelfTest.frame();
    }
}
