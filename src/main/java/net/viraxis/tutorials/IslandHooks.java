package net.viraxis.tutorials;

import net.viraxis.islands.entity.data.Island;
import net.viraxis.islands.entity.data.Islander;
import net.viraxis.islands.manager.GridManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

final class IslandHooks implements Listener {
    IslandHooks(TutorialsPlugin plugin) {}

    static boolean hasIsland(Player player) {
        Islander islander = Islander.get(player);
        return islander != null && islander.getIsland() != null;
    }

    static boolean onOwnIsland(Player player, Location location) {
        if (location == null) return false;
        Islander islander = Islander.get(player);
        if (islander == null || islander.getIslandId() == null) return false;
        Island island = GridManager.get().getIslandAt(location);
        return island != null && island.getId().equals(islander.getIslandId());
    }
}
