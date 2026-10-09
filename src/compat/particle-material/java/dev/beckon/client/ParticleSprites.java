package dev.beckon.client;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;

final class ParticleSprites {
    private static final RandomSource PICK = RandomSource.create(0L);

    private ParticleSprites() {
    }

    static SpriteContents of(ItemEntityRenderState state) {
        if (state.item.isEmpty()) {
            return null;
        }
        Material.Baked material = state.item.pickParticleMaterial(PICK);
        return material == null ? null : material.sprite().contents();
    }
}
