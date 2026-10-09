package dev.beckon.client;

import dev.beckon.config.BeckonConfig;
import dev.beckon.core.Palette;

public final class BeamShape {
    public interface Vertices {
        void vertex(float x, float y, float z, float u, float v, int argb);
    }

    private BeamShape() {
    }

    public static void beams(BeamInfo info, float ageInTicks, double dx, double dz, Vertices out) {
        BeckonConfig config = BeckonClient.config();
        float alpha = config.opacity() * pulse(info, ageInTicks);
        float height = config.beamHeight() * (info.death ? 1.6f : 1f);
        float half = 0.36f * config.beamWidth();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) {
            dx = 1;
            dz = 0;
            len = 1;
        }
        float rx = (float) (-dz / len);
        float rz = (float) (dx / len);
        quad(out, rx, rz, half * 2.2f, height * 0.8f, Palette.withAlpha(info.color, alpha * 0.22f));
        quad(out, rx, rz, half, height, Palette.withAlpha(info.color, alpha * 0.7f));
        quad(out, rx, rz, half * 0.32f, height * 0.94f, Palette.withAlpha(mix(info.color, 0xFFFFFF, 0.6f), alpha));
    }

    public static boolean hasGround() {
        return BeckonClient.config().groundGlow();
    }

    public static void ground(BeamInfo info, float ageInTicks, Vertices out) {
        BeckonConfig config = BeckonClient.config();
        float pulse = pulse(info, ageInTicks);
        float s = 0.55f * Math.max(0.6f, config.beamWidth()) * (0.94f + 0.06f * pulse);
        int color = Palette.withAlpha(info.color, config.opacity() * pulse * 0.7f);
        float y = 0.02f;
        out.vertex(-s, y, -s, 0, 0, color);
        out.vertex(-s, y, s, 0, 1, color);
        out.vertex(s, y, s, 1, 1, color);
        out.vertex(s, y, -s, 1, 0, color);
    }

    private static float pulse(BeamInfo info, float ageInTicks) {
        if (!BeckonClient.config().pulse()) {
            return 1f;
        }
        return 0.84f + 0.16f * (float) Math.sin((ageInTicks / 36f + info.phase) * Math.PI * 2);
    }

    private static void quad(Vertices out, float rx, float rz, float half, float height, int color) {
        float x = rx * half;
        float z = rz * half;
        out.vertex(x, 0, z, 1, 1, color);
        out.vertex(-x, 0, -z, 0, 1, color);
        out.vertex(-x, height, -z, 0, 0, color);
        out.vertex(x, height, z, 1, 0, color);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t);
        int g = Math.round((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }
}
