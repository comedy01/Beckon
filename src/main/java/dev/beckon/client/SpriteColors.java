package dev.beckon.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.beckon.core.Palette;
import dev.beckon.mixin.SpriteContentsAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;

public final class SpriteColors {
    private SpriteColors() {
    }

    public static int of(SpriteContents contents) {
        NativeImage image = ((SpriteContentsAccessor) contents).beckon$originalImage();
        if (image == null) {
            return Palette.WHITE;
        }
        int w = Math.min(contents.width(), image.getWidth());
        int h = Math.min(contents.height(), image.getHeight());
        int step = Math.max(1, Math.max(w, h) / 32);
        int[] pixels = new int[((w + step - 1) / step) * ((h + step - 1) / step)];
        int i = 0;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                pixels[i++] = Pixels.argb(image, x, y);
            }
        }
        return Palette.dominant(pixels);
    }
}
