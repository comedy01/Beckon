package dev.beckon.client;

public interface BeamState {
    BeamInfo beckon$info();

    void beckon$setInfo(BeamInfo info);

    int beckon$ticksLeft();

    void beckon$setTicksLeft(int ticksLeft);
}
