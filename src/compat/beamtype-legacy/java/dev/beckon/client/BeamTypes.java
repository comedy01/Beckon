package dev.beckon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.ResourceLocation;

final class BeamTypes {
    private static final RenderType BEAM = RenderType.beaconBeam(ResourceLocation.fromNamespaceAndPath(BeckonClient.MOD_ID, "textures/misc/beam.png"), true);
    private static final RenderType GLOW = RenderType.beaconBeam(ResourceLocation.fromNamespaceAndPath(BeckonClient.MOD_ID, "textures/misc/glow.png"), true);

    private BeamTypes() {
    }

    static void submit(SubmitNodeCollector collector, PoseStack poseStack, boolean glow, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        collector.submitCustomGeometry(poseStack, glow ? GLOW : BEAM, renderer);
    }
}
