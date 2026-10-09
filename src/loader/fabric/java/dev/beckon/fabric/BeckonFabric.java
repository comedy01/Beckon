package dev.beckon.fabric;

import dev.beckon.client.BeckonClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class BeckonFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BeckonClient.init(FabricLoader.getInstance().getConfigDir());
    }
}
