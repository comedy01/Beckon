package dev.beckon.core;

public final class Palette {
    public static final int WHITE = 0xFFFFFF;

    private Palette() {
    }

    public static int dominant(int[] argb) {
        double r = 0, g = 0, b = 0, total = 0;
        double fr = 0, fg = 0, fb = 0, ftotal = 0;
        for (int pixel : argb) {
            int a = pixel >>> 24;
            if (a < 128) {
                continue;
            }
            int pr = pixel >> 16 & 0xFF, pg = pixel >> 8 & 0xFF, pb = pixel & 0xFF;
            float[] hsv = hsv(pr, pg, pb);
            fr += pr;
            fg += pg;
            fb += pb;
            ftotal++;
            if (hsv[2] < 0.18f) {
                continue;
            }
            double w = 0.15 + hsv[1] * hsv[1] * 4 + hsv[2] * 0.5;
            r += pr * w;
            g += pg * w;
            b += pb * w;
            total += w;
        }
        if (total == 0) {
            if (ftotal == 0) {
                return WHITE;
            }
            return vivid(rgb(fr / ftotal, fg / ftotal, fb / ftotal));
        }
        return vivid(rgb(r / total, g / total, b / total));
    }

    public static int vivid(int rgb) {
        float[] hsv = hsv(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
        float s = hsv[1] < 0.12f ? hsv[1] * 0.5f : Math.min(1f, hsv[1] * 1.25f + 0.1f);
        float v = Math.max(hsv[2], 0.92f);
        return fromHsv(hsv[0], s, v);
    }

    public static boolean isWhite(int rgb) {
        float[] hsv = hsv(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
        return hsv[1] < 0.08f && hsv[2] > 0.9f;
    }

    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return a << 24 | rgb & 0xFFFFFF;
    }

    private static int rgb(double r, double g, double b) {
        return (int) Math.round(r) << 16 | (int) Math.round(g) << 8 | (int) Math.round(b);
    }

    static float[] hsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h;
        if (d == 0) {
            h = 0;
        } else if (max == rf) {
            h = ((gf - bf) / d) % 6;
        } else if (max == gf) {
            h = (bf - rf) / d + 2;
        } else {
            h = (rf - gf) / d + 4;
        }
        h /= 6;
        if (h < 0) {
            h += 1;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    static int fromHsv(float h, float s, float v) {
        float c = v * s;
        float hp = h * 6;
        float x = c * (1 - Math.abs(hp % 2 - 1));
        float r, g, b;
        switch ((int) hp % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        float m = v - c;
        return rgb((r + m) * 255, (g + m) * 255, (b + m) * 255);
    }
}
