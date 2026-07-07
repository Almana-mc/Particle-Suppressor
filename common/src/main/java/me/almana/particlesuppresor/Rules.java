package me.almana.particlesuppresor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public final class Rules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean master = true;
    public static int maxAnchors = 10;
    public static boolean showInvButton = true;
    public static int invButtonX = 154;
    public static int invButtonY = 6;

    public static final Object2IntOpenHashMap<ResourceLocation> global = new Object2IntOpenHashMap<>();
    public static final List<Anchor> anchors = new ArrayList<>();
    public static final Map<ResourceLocation, Boolean> recent = Collections.synchronizedMap(
        new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ResourceLocation, Boolean> eldest) {
                return size() > 100;
            }
        });

    private static boolean active;
    private static String anchorsKey;

    static {
        global.defaultReturnValue(100);
    }

    private Rules() {}

    public static boolean suppress(ParticleOptions options, double x, double y, double z) {
        return suppress(options.getType(), x, y, z);
    }

    public static boolean suppress(ParticleType<?> type, double x, double y, double z) {
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        recent.put(id, Boolean.TRUE);
        if (!active) return false;
        int density = global.getInt(id);
        if (!anchors.isEmpty()) {
            ResourceLocation dim = Minecraft.getInstance().level.dimension().location();
            for (Anchor anchor : anchors) {
                density = Math.min(density, anchor.density(id, dim, x, y, z));
            }
        }
        if (density >= 100) return false;
        if (density <= 0) return true;
        return ThreadLocalRandom.current().nextInt(100) >= density;
    }

    public static void setGlobal(ResourceLocation id, int value) {
        if (value >= 100) global.removeInt(id);
        else global.put(id, value);
        recompute();
    }

    public static void recompute() {
        active = master && (!global.isEmpty() || !anchors.isEmpty());
    }

    private static Path dir() {
        return Platform.INSTANCE.configDir().resolve("particlesuppresor");
    }

    public static void loadGlobal() {
        Path file = dir().resolve("global.json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                GlobalData data = GSON.fromJson(reader, GlobalData.class);
                master = data.master;
                maxAnchors = data.maxAnchors;
                showInvButton = data.showInvButton;
                invButtonX = data.invButtonX;
                invButtonY = data.invButtonY;
                global.clear();
                data.densities.forEach((id, d) -> global.put(ResourceLocation.parse(id), (int) d));
            } catch (Exception e) {
                LOGGER.warn("Failed to load global.json", e);
            }
        }
        recompute();
    }

    public static void loadAnchors() {
        anchors.clear();
        anchorsKey = worldKey();
        Path file = dir().resolve("anchors").resolve(anchorsKey + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                List<Anchor> loaded = GSON.fromJson(reader, new TypeToken<List<Anchor>>() {}.getType());
                anchors.addAll(loaded);
                anchors.forEach(Anchor::rebuild);
            } catch (Exception e) {
                LOGGER.warn("Failed to load anchors for {}", anchorsKey, e);
            }
        }
        recompute();
    }

    public static void clearAnchors() {
        anchors.clear();
        anchorsKey = null;
        recompute();
    }

    public static void save() {
        try {
            Files.createDirectories(dir().resolve("anchors"));
            GlobalData data = new GlobalData();
            data.master = master;
            data.maxAnchors = maxAnchors;
            data.showInvButton = showInvButton;
            data.invButtonX = invButtonX;
            data.invButtonY = invButtonY;
            global.forEach((id, d) -> data.densities.put(id.toString(), d));
            Files.writeString(dir().resolve("global.json"), GSON.toJson(data));
            if (anchorsKey != null) {
                Files.writeString(dir().resolve("anchors").resolve(anchorsKey + ".json"), GSON.toJson(anchors));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to save", e);
        }
    }

    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        String raw;
        if (mc.getSingleplayerServer() != null) {
            raw = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
        } else {
            raw = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "unknown";
        }
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static class GlobalData {
        boolean master = true;
        int maxAnchors = 10;
        boolean showInvButton = true;
        int invButtonX = 154;
        int invButtonY = 6;
        Map<String, Integer> densities = new TreeMap<>();
    }
}
