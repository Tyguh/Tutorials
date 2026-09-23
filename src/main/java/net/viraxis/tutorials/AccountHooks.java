package net.viraxis.tutorials;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/** Reads the same account records used by /freerank. An unavailable service never completes a step. */
final class AccountHooks {
    private AccountHooks() {}

    static boolean discordLinked(Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("DiscordRelay");
        if (plugin == null || !plugin.isEnabled()) return false;
        try {
            Class<?> apiType = Class.forName("net.viraxis.relay.plugin.api.DiscordAPI", true,
                    plugin.getClass().getClassLoader());
            Object api = apiType.getMethod("getInstance").invoke(null);
            return api != null && apiType.getMethod("getUserRecord", UUID.class).invoke(api, player.getUniqueId()) != null;
        } catch (ReflectiveOperationException | LinkageError error) { return false; }
    }

    static long totalVotes(Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Voting");
        if (plugin == null || !plugin.isEnabled()) return -1;
        try {
            Class<?> type = Class.forName("net.viraxis.voting.entity.VotePlayerColl", true,
                    plugin.getClass().getClassLoader());
            Object coll = type.getMethod("get").invoke(null);
            Object data = type.getMethod("getByUuid", String.class).invoke(coll, player.getUniqueId().toString());
            return data == null ? -1 : ((Number) data.getClass().getMethod("getTotalVotes").invoke(data)).longValue();
        } catch (ReflectiveOperationException | LinkageError error) { return -1; }
    }

    static boolean freeRankClaimed(Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (plugin == null || !plugin.isEnabled()) return false;
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            return user != null && user.getNodes().stream().anyMatch(node ->
                    node.getKey().equals("essentials.freerank.claimed") && node.getValue());
        } catch (IllegalStateException | LinkageError error) { return false; }
    }
}
