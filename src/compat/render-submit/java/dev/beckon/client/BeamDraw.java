package dev.beckon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.phys.Vec3;

public final class BeamDraw {
    private BeamDraw() {
    }

    public static void submit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        BeamInfo info = StateBeams.info(state);
        if (info == null || camera == null) {
            return;
        }
        float age = state.ageInTicks;
        double dx = camera.x - state.x;
        double dz = camera.z - state.z;
        BeamTypes.submit(collector, poseStack, false, (pose, consumer) ->
                BeamShape.beams(info, age, dx, dz, (x, y, z, u, v, c) -> Vertex.put(pose, consumer, x, y, z, u, v, c)));
        if (BeamShape.hasGround()) {
            BeamTypes.submit(collector, poseStack, true, (pose, consumer) ->
                    BeamShape.ground(info, age, (x, y, z, u, v, c) -> Vertex.put(pose, consumer, x, y, z, u, v, c)));
        }
    }
}
