package dev.beckon.selftest.neoforge;

import dev.beckon.selftest.BeckonSelfTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "beckon_selftest", dist = Dist.CLIENT)
public final class BeckonSelfTestNeoForge {
    public BeckonSelfTestNeoForge() {
        BeckonSelfTest test = new BeckonSelfTest();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> test.tick());
    }
}
