package me.almana.particlesuppresor.fabric;

import java.nio.file.Path;
import me.almana.particlesuppresor.Platform;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
