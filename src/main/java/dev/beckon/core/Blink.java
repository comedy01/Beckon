package dev.beckon.core;

public final class Blink {
    public static final int LIFETIME = 6000;

    private Blink() {
    }

    public static boolean visible(int ticksLeft, float partialTick, int warnTicks) {
        if (ticksLeft > warnTicks || ticksLeft < 0) {
            return true;
        }
        float t = ticksLeft - partialTick;
        float urgency = 1f - Math.max(0f, t) / warnTicks;
        float period = 20f - 16f * urgency;
        float phase = ((LIFETIME - t) % period) / period;
        return phase < 0.62f;
    }
}
