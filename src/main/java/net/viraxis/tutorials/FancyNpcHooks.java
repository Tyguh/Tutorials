package net.viraxis.tutorials;

import com.massivecraft.massivecore.player.SyntheticPlayers;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

final class FancyNpcHooks implements Listener {
    private final TutorialsPlugin plugin;

    FancyNpcHooks(TutorialsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNpcInteract(NpcInteractEvent event) {
        Player player = event.getPlayer();
        if (SyntheticPlayers.isSynthetic(player) || event.getNpc() == null || event.getNpc().getData() == null) return;
        String name = event.getNpc().getData().getName();
        plugin.advance(player, "NpcInteract", trigger -> trigger.npc().equalsIgnoreCase(name));
    }

    static Location location(String npcName) {
        if (npcName == null || npcName.isBlank()) return null;
        Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpc(npcName);
        if (npc == null || npc.getData() == null || npc.getData().getLocation() == null) return null;
        return npc.getData().getLocation().clone().add(0.0D, npc.getEyeHeight() * npc.getData().getScale(), 0.0D);
    }
}
