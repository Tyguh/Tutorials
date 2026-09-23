package net.viraxis.tutorials;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

final class SpawnRegion {
    private static final String REGION_ID = "spawn";

    private SpawnRegion() {}

    static boolean contains(Location location) {
        if (location == null || location.getWorld() == null) return false;
        try {
            ApplicableRegionSet regions = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
            for (ProtectedRegion region : regions.getRegions()) {
                if (region.getId().equalsIgnoreCase(REGION_ID)) return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }
}
