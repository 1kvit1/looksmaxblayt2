package com.looksmax;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/** "Внешность" мобов для мога. */
public final class MobRatings {
    private MobRatings() {}

    private static final Map<String, Double> FIXED = Map.ofEntries(
            Map.entry("zombie", 2.0), Map.entry("husk", 1.8), Map.entry("drowned", 1.9),
            Map.entry("skeleton", 2.6), Map.entry("stray", 3.0), Map.entry("creeper", 3.2),
            Map.entry("spider", 2.8), Map.entry("cave_spider", 2.4), Map.entry("slime", 1.5),
            Map.entry("enderman", 6.5), Map.entry("villager", 4.0), Map.entry("witch", 3.5),
            Map.entry("iron_golem", 5.5), Map.entry("piglin", 4.5), Map.entry("blaze", 6.0),
            Map.entry("phantom", 2.2), Map.entry("warden", 1.5), Map.entry("wither", 7.0),
            Map.entry("ender_dragon", 8.5), Map.entry("allay", 7.5), Map.entry("axolotl", 7.0),
            Map.entry("fox", 6.8), Map.entry("cat", 6.5), Map.entry("wolf", 6.0),
            Map.entry("pig", 3.5), Map.entry("cow", 3.8), Map.entry("sheep", 4.2),
            Map.entry("chicken", 3.0), Map.entry("bee", 5.0), Map.entry("panda", 7.2));

    public static double of(Entity e) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        Double fixed = FIXED.get(path);
        if (fixed != null) return fixed;
        return 1.0 + Math.floorMod(path.hashCode(), 450) / 100.0; // 1.00..5.49
    }
}
