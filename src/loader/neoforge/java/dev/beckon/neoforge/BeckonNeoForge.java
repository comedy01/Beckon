package dev.beckon.neoforge;

import dev.beckon.client.BeckonClient;
import dev.beckon.client.gui.BeckonSettingsScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = BeckonClient.MOD_ID, dist = Dist.CLIENT)
public final class BeckonNeoForge {
    public BeckonNeoForge(IEventBus modBus, ModContainer container) {
        BeckonClient.init(FMLPaths.CONFIGDIR.get());

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new BeckonSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
