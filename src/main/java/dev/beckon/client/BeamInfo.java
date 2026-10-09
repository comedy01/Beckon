package dev.beckon.client;

public final class BeamInfo {
    public final int color;
    public final boolean death;
    public final int total;
    public final float phase;
    public final boolean labelled;

    BeamInfo(int color, boolean death, int total, float phase, boolean labelled) {
        this.color = color;
        this.death = death;
        this.total = total;
        this.phase = phase;
        this.labelled = labelled;
    }
}
