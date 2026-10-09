package dev.beckon.mixin;

import dev.beckon.client.BeamInfo;
import dev.beckon.client.BeamState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements BeamState {
    @Unique
    private BeamInfo beckon$info;
    @Unique
    private int beckon$ticksLeft = -1;

    @Override
    public BeamInfo beckon$info() {
        return beckon$info;
    }

    @Override
    public void beckon$setInfo(BeamInfo info) {
        beckon$info = info;
    }

    @Override
    public int beckon$ticksLeft() {
        return beckon$ticksLeft;
    }

    @Override
    public void beckon$setTicksLeft(int ticksLeft) {
        beckon$ticksLeft = ticksLeft;
    }
}
