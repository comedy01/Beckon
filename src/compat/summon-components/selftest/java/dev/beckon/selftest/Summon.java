package dev.beckon.selftest;

final class Summon {
    private Summon() {
    }

    static String item(String id) {
        return "{Item:{id:\"" + id + "\",count:1},Motion:[0.0,0.0,0.0],PickupDelay:32767}";
    }
}
