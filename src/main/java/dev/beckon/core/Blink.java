package dev.beckon.core;

public final class Blink {
    public static final int LIFETIME = 6000;
    private static final float SLOWEST = 20f;
    private static final float FASTEST = 4f;

    private Blink() {
    }

    /**
     * The blink period shrinks from {@link #SLOWEST} to {@link #FASTEST} ticks over the warning. The phase counts the
     * cycles blinked since the warning began (the integral of 1 / period), so every cycle is a little shorter than the
     * one before it instead of jumping around.
     */
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
