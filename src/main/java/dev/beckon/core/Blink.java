package dev.beckon.core;

public final class Blink {
    public static final int LIFETIME = 6000;
    private static final float SLOWEST = 20f;
    private static final float FASTEST = 4f;

    private Blink() {
    }

    public static boolean visible(int ticksLeft, float partialTick, int warnTicks) {
        if (ticksLeft > warnTicks || ticksLeft < 0) {
            return true;
        }
        float into = Math.min(warnTicks, warnTicks - ticksLeft + partialTick);
        float slope = (SLOWEST - FASTEST) / warnTicks;
        float period = SLOWEST - slope * into;
        double cycles = Math.log(SLOWEST / period) / slope;
        return cycles - Math.floor(cycles) < 0.62;
    }
}
