package dev.beckon.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.beckon.client.gui.BeckonSettingsScreen;
import net.minecraft.client.Minecraft;

public final class BeckonModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new BeckonSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
