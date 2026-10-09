package dev.beckon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.beckon.client.BeamDraw;
import dev.beckon.client.StateBeams;
import dev.beckon.client.BeckonClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
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

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void beckon$blink(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (BeckonClient.active() && StateBeams.hidden(state)) {
            ci.cancel();
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("TAIL"))
    private void beckon$submit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!BeckonClient.active()) {
            return;
        }
        try {
            BeamDraw.submit(state, poseStack, collector, camera.pos);
        } catch (RuntimeException e) {
            BeckonClient.fail(e);
        }
    }
}
