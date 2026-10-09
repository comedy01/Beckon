package dev.beckon.selftest;

final class Summon {
    private Summon() {
    }

    static String item(String id) {
        return "{Item:{id:\"" + id + "\",Count:1b},Motion:[0.0,0.0,0.0],PickupDelay:32767}";
    }
}
