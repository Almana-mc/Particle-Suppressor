package me.almana.particlesuppresor.neoforge;

import java.nio.file.Path;
import me.almana.particlesuppresor.Platform;
import net.neoforged.fml.loading.FMLPaths;

public class NeoForgePlatform implements Platform {
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
