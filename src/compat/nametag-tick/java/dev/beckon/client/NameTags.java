package dev.beckon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.beckon.mixin.EntityRendererInvoker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

final class NameTags {
    private NameTags() {
    }

    static void draw(Object renderer, Entity entity, Component name, PoseStack poseStack, MultiBufferSource buffers, int light, float partialTick) {
        ((EntityRendererInvoker) renderer).beckon$renderNameTag(entity, name, poseStack, buffers, light, partialTick);
    }
}
