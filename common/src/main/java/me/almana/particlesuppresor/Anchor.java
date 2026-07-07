package me.almana.particlesuppresor;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class Anchor {
    public String name = "Anchor";
    public String dimension;
    public int x, y, z;
    public int radius = 16;
    public Map<String, Integer> densities = new HashMap<>();

    transient ResourceLocation dimId;
    transient Object2IntOpenHashMap<ResourceLocation> lookup = emptyLookup();

    private static Object2IntOpenHashMap<ResourceLocation> emptyLookup() {
        Object2IntOpenHashMap<ResourceLocation> map = new Object2IntOpenHashMap<>();
        map.defaultReturnValue(100);
        return map;
    }

    public void rebuild() {
        dimId = dimension == null ? null : ResourceLocation.parse(dimension);
        lookup = emptyLookup();
        densities.forEach((id, d) -> lookup.put(ResourceLocation.parse(id), (int) d));
    }

    public void setDensity(ResourceLocation id, int value) {
        if (value >= 100) {
            densities.remove(id.toString());
            lookup.removeInt(id);
        } else {
            densities.put(id.toString(), value);
            lookup.put(id, value);
        }
    }

    public int getDensity(ResourceLocation id) {
        return lookup.getInt(id);
    }

    public int density(ResourceLocation id, ResourceLocation dim, double px, double py, double pz) {
        if (!dim.equals(dimId) || lookup.isEmpty()) return 100;
        double dx = px - (x + 0.5);
        double dy = py - (y + 0.5);
        double dz = pz - (z + 0.5);
        if (dx * dx + dy * dy + dz * dz > (double) radius * radius) return 100;
        return lookup.getInt(id);
    }

    public void setPos() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = mc.player.blockPosition();
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        dimId = mc.level.dimension().location();
        dimension = dimId.toString();
    }
}
