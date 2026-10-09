package dev.beckon.selftest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class Summon {
    private Summon() {
    }

    static String item(String id) {
        return "{Item:{id:\"" + id + "\",count:1},Motion:[0.0,0.0,0.0],PickupDelay:32767}";
    }

    static void name(ItemStack stack, Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name);
    }
}
