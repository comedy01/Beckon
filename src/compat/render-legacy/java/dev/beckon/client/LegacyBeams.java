package dev.beckon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.beckon.config.BeckonConfig;
import dev.beckon.core.Blink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class LegacyBeams {
    private LegacyBeams() {
    }

    public static boolean hidden(ItemEntity entity, float partialTick) {
        BeckonConfig config = BeckonClient.config();
        int age = entity.getAge();
        if (!config.despawnBlink() || age < 0) {
            return false;
        }
        return !Blink.visible(Blink.LIFETIME - age, partialTick, config.despawnWarnTicks());
    }

    public static void render(Object renderer, ItemEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ItemStack stack = entity.getItem();
        Minecraft mc = Minecraft.getInstance();
        if (Beams.needsSpriteColor(stack.getItem())) {
            BakedModel model = mc.getItemRenderer().getModel(stack, entity.level(), null, entity.getId());
            TextureAtlasSprite sprite = model == null ? null : model.getParticleIcon();
            if (sprite != null) {
                Beams.rememberSpriteColor(stack.getItem(), SpriteColors.of(sprite.contents()));
            }
        }
        BeamInfo info = Beams.get(entity.getId());
        if (info == null) {
            return;
        }
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        float age = entity.getAge() + partialTick;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer beam = BeamTypes.buffer(buffers, false);
        BeamShape.beams(info, age, camera.x - x, camera.z - z, (vx, vy, vz, u, v, c) -> Vertex.put(pose, beam, vx, vy, vz, u, v, c));
        if (BeamShape.hasGround()) {
            VertexConsumer glow = BeamTypes.buffer(buffers, true);
            BeamShape.ground(info, age, (vx, vy, vz, u, v, c) -> Vertex.put(pose, glow, vx, vy, vz, u, v, c));
        }
        if (info.labelled && BeckonClient.config().labels()) {
            poseStack.pushPose();
            poseStack.translate(0, 0.3, 0);
            NameTags.draw(renderer, entity, Stacks.label(stack, info.total), poseStack, buffers, light, partialTick);
            poseStack.popPose();
        }
    }
}
