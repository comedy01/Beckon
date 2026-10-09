package dev.beckon.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class BeamTypes {
    private static final RenderType BEAM = RenderType.beaconBeam(ResourceLocation.fromNamespaceAndPath(BeckonClient.MOD_ID, "textures/misc/beam.png"), true);
    private static final RenderType GLOW = RenderType.beaconBeam(ResourceLocation.fromNamespaceAndPath(BeckonClient.MOD_ID, "textures/misc/glow.png"), true);

    private BeamTypes() {
    }

    static VertexConsumer buffer(MultiBufferSource buffers, boolean glow) {
        return buffers.getBuffer(glow ? GLOW : BEAM);
    }
}
