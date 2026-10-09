package dev.beckon.selftest.forge;

import dev.beckon.selftest.BeckonSelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("beckon_selftest")
public final class BeckonSelfTestForge {
    public BeckonSelfTestForge() {
        BeckonSelfTest test = new BeckonSelfTest();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                test.tick();
            }
        });
    }
}
