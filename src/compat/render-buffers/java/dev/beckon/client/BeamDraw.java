package dev.beckon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.phys.Vec3;

public final class BeamDraw {
    private BeamDraw() {
    }

    public static void render(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers) {
        BeamInfo info = StateBeams.info(state);
        if (info == null) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer beam = BeamTypes.buffer(buffers, false);
        BeamShape.beams(info, state.ageInTicks, camera.x - state.x, camera.z - state.z,
                (x, y, z, u, v, c) -> Vertex.put(pose, beam, x, y, z, u, v, c));
        if (BeamShape.hasGround()) {
            VertexConsumer glow = BeamTypes.buffer(buffers, true);
            BeamShape.ground(info, state.ageInTicks, (x, y, z, u, v, c) -> Vertex.put(pose, glow, x, y, z, u, v, c));
        }
    }
}
