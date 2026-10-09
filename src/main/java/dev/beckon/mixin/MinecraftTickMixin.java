package dev.beckon.mixin;

import dev.beckon.client.BeckonClient;
import dev.beckon.client.Beams;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void beckon$tick(CallbackInfo ci) {
        try {
            Beams.tick((Minecraft) (Object) this);
        } catch (RuntimeException e) {
            BeckonClient.fail(e);
        }
    }
}
