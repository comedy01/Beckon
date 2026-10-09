package dev.beckon.client;

import dev.beckon.config.BeckonConfig;
import dev.beckon.core.Blink;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class StateBeams {
    private StateBeams() {
    }

    public static void clear(ItemEntityRenderState state) {
        BeamState beam = (BeamState) state;
        beam.beckon$setInfo(null);
        beam.beckon$setTicksLeft(-1);
    }

    public static void extract(ItemEntity entity, ItemEntityRenderState state) {
        BeckonConfig config = BeckonClient.config();
        BeamState beam = (BeamState) state;
        ItemStack stack = entity.getItem();
        if (Beams.needsSpriteColor(stack.getItem())) {
            var sprite = ParticleSprites.of(state);
            if (sprite != null) {
                Beams.rememberSpriteColor(stack.getItem(), SpriteColors.of(sprite));
            }
        }
        BeamInfo info = Beams.get(entity.getId());
        beam.beckon$setInfo(info);
        int age = entity.getAge();
        beam.beckon$setTicksLeft(config.despawnBlink() && age >= 0 ? Blink.LIFETIME - age : -1);
        if (info != null && info.labelled && config.labels()) {
            state.nameTag = Stacks.label(stack, info.total);
            state.nameTagAttachment = new Vec3(0, 0.55, 0);
        }
    }

    public static boolean hidden(ItemEntityRenderState state) {
        int left = ((BeamState) state).beckon$ticksLeft();
        if (left < 0) {
            return false;
        }
        float partial = state.ageInTicks - (float) Math.floor(state.ageInTicks);
        return !Blink.visible(left, partial, BeckonClient.config().despawnWarnTicks());
    }

    public static BeamInfo info(ItemEntityRenderState state) {
        return ((BeamState) state).beckon$info();
    }
}
