package dev.beckon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class Vertex {
    private static final int FULL_BRIGHT = 0xF000F0;

    private Vertex() {
    }

    static void put(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, int argb) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();
    }
}
