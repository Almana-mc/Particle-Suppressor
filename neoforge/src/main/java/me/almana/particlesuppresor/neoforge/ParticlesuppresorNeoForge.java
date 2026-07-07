package me.almana.particlesuppresor.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import me.almana.particlesuppresor.Rules;
import me.almana.particlesuppresor.gui.InvButton;
import me.almana.particlesuppresor.gui.SuppressorScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "particlesuppresor", dist = Dist.CLIENT)
public class ParticlesuppresorNeoForge {
    public static final KeyMapping OPEN_KEY = new KeyMapping(
        "key.particlesuppresor.open", InputConstants.UNKNOWN.getValue(), "key.categories.misc");

    public ParticlesuppresorNeoForge(IEventBus modBus) {
        Rules.loadGlobal();
        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::onScreenInit);
        NeoForge.EVENT_BUS.addListener(this::onLogin);
        NeoForge.EVENT_BUS.addListener(this::onLogout);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }

    private void onTick(ClientTickEvent.Post event) {
        while (OPEN_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new SuppressorScreen());
        }
    }

    private void onScreenInit(ScreenEvent.Init.Post event) {
        if (Rules.showInvButton && event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(new InvButton(screen.getGuiLeft(), screen.getGuiTop()));
        }
    }

    private void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Rules.loadAnchors();
    }

    private void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Rules.save();
        Rules.clearAnchors();
    }
}
