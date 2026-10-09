package dev.beckon.selftest;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class Summon {
    private Summon() {
    }

    static String item(String id) {
        return "{Item:{id:\"" + id + "\",Count:1b},Motion:[0.0,0.0,0.0],PickupDelay:32767}";
    }

    static void name(ItemStack stack, Component name) {
        stack.setHoverName(name);
    }
}
