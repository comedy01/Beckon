package dev.beckon.selftest.fabric;

import dev.beckon.selftest.BeckonSelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class BeckonSelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BeckonSelfTest test = new BeckonSelfTest();
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
