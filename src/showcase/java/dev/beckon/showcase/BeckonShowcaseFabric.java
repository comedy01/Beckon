package dev.beckon.showcase;

import net.fabricmc.api.ClientModInitializer;

public final class BeckonShowcaseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Showcase.start();
    }
}
