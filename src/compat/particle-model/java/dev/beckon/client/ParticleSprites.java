package dev.beckon.client;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

final class ParticleSprites {
    private ParticleSprites() {
    }

    static SpriteContents of(ItemEntityRenderState state) {
        if (state.itemModel == null) {
            return null;
        }
        TextureAtlasSprite sprite = state.itemModel.getParticleIcon();
        return sprite == null ? null : sprite.contents();
    }
}
