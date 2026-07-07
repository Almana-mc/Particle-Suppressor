package me.almana.particlesuppresor.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import me.almana.particlesuppresor.Rules;
import me.almana.particlesuppresor.gui.InvButton;
import me.almana.particlesuppresor.gui.SuppressorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class ParticlesuppresorFabric implements ClientModInitializer {
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;

    public static final KeyMapping OPEN_KEY = new KeyMapping(
        "key.particlesuppresor.open", InputConstants.UNKNOWN.getValue(), "key.categories.misc");

    @Override
    public void onInitializeClient() {
        Rules.loadGlobal();
        KeyBindingHelper.registerKeyBinding(OPEN_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ScreenEvents.AFTER_INIT.register(this::onScreenInit);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Rules.loadAnchors());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Rules.save();
            Rules.clearAnchors();
        });
    }

    private void onTick(Minecraft client) {
        while (OPEN_KEY.consumeClick()) {
            client.setScreen(new SuppressorScreen());
        }
    }

    private void onScreenInit(Minecraft client, Screen screen, int width, int height) {
        if (Rules.showInvButton && screen instanceof InventoryScreen) {
            Screens.getButtons(screen).add(new InvButton(
                (width - INVENTORY_WIDTH) / 2, (height - INVENTORY_HEIGHT) / 2));
        }
    }
}
