package dev.beckon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class Vertex {
    private static final int FULL_BRIGHT = 0xF000F0;

    private Vertex() {
    }

    static void put(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, int argb) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }
}
