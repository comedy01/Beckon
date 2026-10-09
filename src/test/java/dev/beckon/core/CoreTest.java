package dev.beckon.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreTest {
    @Test
    void dominantColorFollowsTheColorfulPixels() {
        int[] pixels = new int[64];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = i < 16 ? 0xFF202020 : i < 24 ? 0x00000000 : 0xFF33E0D0;
        }
        int rgb = Palette.dominant(pixels);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        assertTrue(g > r && b > r, "expected a cyan beam, got " + Integer.toHexString(rgb));
    }

    @Test
    void grayItemsStayNeutral() {
        int[] pixels = new int[16];
        java.util.Arrays.fill(pixels, 0xFFA0A0A0);
        int rgb = Palette.dominant(pixels);
        assertTrue(Palette.isWhite(rgb) || Math.abs((rgb >> 16 & 0xFF) - (rgb & 0xFF)) < 12, Integer.toHexString(rgb));
    }

    @Test
    void transparentSpriteFallsBackToWhite() {
        assertEquals(Palette.WHITE, Palette.dominant(new int[8]));
    }

    @Test
    void blinkOnlyNearTheEnd() {
        int warn = 600;
        assertTrue(Blink.visible(5000, 0, warn));
        assertTrue(Blink.visible(-1, 0, warn));
        int hidden = 0;
        for (int t = 0; t < 40; t++) {
            if (!Blink.visible(200 - t, 0, warn)) {
                hidden++;
            }
        }
        assertTrue(hidden > 0 && hidden < 40);
    }

    @Test
    void blinkGetsFasterAsTimeRunsOut() {
        int warn = 600;
        assertTrue(flips(590, warn) < flips(30, warn));
    }

    private static int flips(int start, int warn) {
        int flips = 0;
        boolean last = Blink.visible(start, 0, warn);
        for (int i = 1; i <= 40; i++) {
            boolean now = Blink.visible(start - i / 4, (i % 4) / 4f, warn);
            if (now != last) {
                flips++;
            }
            last = now;
        }
        return flips;
    }

    @Test
    void rulesMatchIdsAndTags() {
        Rules rules = new Rules(List.of("diamond", "  #c:gems ", "mymod:ruby", ""));
        assertTrue(rules.matches("minecraft:diamond", List.of()));
        assertTrue(rules.matches("mymod:ruby", List.of()));
        assertTrue(rules.matches("othermod:sapphire", List.of("c:gems")));
        assertFalse(rules.matches("minecraft:dirt", List.of("minecraft:dirt")));
        assertTrue(new Rules(null).isEmpty());
    }
}
