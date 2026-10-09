package dev.beckon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.beckon.client.BeckonClient;
import dev.beckon.client.LegacyBeams;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void beckon$blink(ItemEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (BeckonClient.active() && LegacyBeams.hidden(entity, partialTick)) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    private void beckon$render(ItemEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!BeckonClient.active()) {
            return;
        }
        try {
            LegacyBeams.render(this, entity, partialTick, poseStack, buffers, light);
        } catch (RuntimeException e) {
            BeckonClient.fail(e);
        }
    }
}
