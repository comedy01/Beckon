package dev.beckon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.beckon.client.BeamDraw;
import dev.beckon.client.BeckonClient;
import dev.beckon.client.StateBeams;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void beckon$extract(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!BeckonClient.active()) {
            StateBeams.clear(state);
            return;
        }
        try {
            StateBeams.extract(entity, state);
        } catch (RuntimeException e) {
            BeckonClient.fail(e);
            StateBeams.clear(state);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void beckon$blink(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (BeckonClient.active() && StateBeams.hidden(state)) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    private void beckon$render(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!BeckonClient.active()) {
            return;
        }
        try {
            BeamDraw.render(state, poseStack, buffers);
        } catch (RuntimeException e) {
            BeckonClient.fail(e);
        }
    }
}
