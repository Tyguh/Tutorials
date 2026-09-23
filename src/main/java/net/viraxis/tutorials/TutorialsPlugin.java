package net.viraxis.tutorials;

import com.massivecraft.massivecore.MassivePlugin;
import com.massivecraft.massivecore.player.SyntheticPlayers;
import com.massivecraft.massivecore.util.ActionFeedback;
import com.massivecraft.massivecore.util.Feedback;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class TutorialsPlugin extends MassivePlugin implements Listener {
    private static TutorialsPlugin instance;
    private String gamemode;
    private TutorialDefinition definition;
    private boolean islandHooks;
    private boolean currencyHooks;
    private boolean npcHooks;
    private TutorialPresentation presentation;

    public TutorialsPlugin() { instance = this; }
    public static TutorialsPlugin get() { return instance; }

    @Override public void onEnableInner() {
        activate(TutorialConfColl.class);
        try {
            migrateAndCreateConfig();
        } catch (Exception ex) {
            getLogger().severe("Could not prepare tutorial configuration: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!loadDefinition()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        activate(TutorialPlayerColl.class, TutorialCommand.class);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (islandHooks) Bukkit.getPluginManager().registerEvents(new IslandHooks(this), this);
        if (currencyHooks) Bukkit.getPluginManager().registerEvents(new CurrencyHooks(this), this);
        if (npcHooks) Bukkit.getPluginManager().registerEvents(new FancyNpcHooks(this), this);
        presentation = new TutorialPresentation(this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (definition == null) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (SyntheticPlayers.isSynthetic(player)) continue;
                if (islandHooks) advance(player, "HasIsland", trigger -> IslandHooks.hasIsland(player));
                advance(player, "DiscordLinked", trigger -> AccountHooks.discordLinked(player));
                advance(player, "VoteCount", trigger -> AccountHooks.totalVotes(player) >= trigger.minimumVotes());
                advance(player, "FreeRankClaimed", trigger -> AccountHooks.freeRankClaimed(player));
            }
        }, 40L, 40L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!SyntheticPlayers.isSynthetic(player)) Bukkit.getScheduler().runTaskLater(this, () -> announce(player), 20L);
        }
    }

    private boolean loadDefinition() {
        TutorialConf.get().loadFromRemote();
        String configured = TutorialConf.get().activeGamemode;
        String selected = configured == null ? "none" : configured.toLowerCase(Locale.ROOT);
        if (gamemode != null && !gamemode.equals(selected)) {
            getLogger().severe("Changing active-gamemode requires a server restart.");
            return false;
        }
        if (selected.equals("none")) {
            gamemode = selected;
            definition = null;
            islandHooks = false;
            currencyHooks = false;
            return true;
        }
        if (!selected.matches("[a-z0-9_-]+")) {
            getLogger().severe("Invalid active-gamemode: " + selected);
            return false;
        }
        try {
            File file = new File("mconf/tutorials", selected + ".yml");
            if (!file.isFile()) throw new IllegalArgumentException("Missing " + file.getPath());
            TutorialDefinition loaded = TutorialDefinition.load(file);
            boolean needsIslands = loaded.quests().stream().flatMap(q -> q.triggers().stream())
                    .anyMatch(t -> t.type().equals("HasIsland") || t.ownIsland());
            boolean needsCurrencies = loaded.quests().stream().flatMap(q -> q.triggers().stream())
                    .anyMatch(t -> t.type().equals("ShopSale"));
            boolean needsNpcs = loaded.quests().stream().anyMatch(q -> q.target() != null && q.target().npc() != null)
                    || loaded.quests().stream().flatMap(q -> q.triggers().stream()).anyMatch(t -> t.type().equals("NpcInteract"));
            if (needsIslands && Bukkit.getPluginManager().getPlugin("Islands") == null)
                throw new IllegalStateException("Islands is required by " + selected);
            if (needsCurrencies && Bukkit.getPluginManager().getPlugin("Currencies") == null)
                throw new IllegalStateException("Currencies is required by " + selected);
            if (needsNpcs && Bukkit.getPluginManager().getPlugin("FancyNpcs") == null)
                throw new IllegalStateException("FancyNpcs is required by " + selected);
            if (Bukkit.getPluginManager().getPlugin("Holograms") == null)
                throw new IllegalStateException("Holograms is required by " + selected);
            // Dependencies are fixed when the plugin enables. A changed dependency set needs a restart.
            if (definition != null && (needsIslands != islandHooks || needsCurrencies != currencyHooks || needsNpcs != npcHooks))
                throw new IllegalStateException("Trigger dependency changes require a restart");
            gamemode = selected;
            definition = loaded;
            islandHooks = needsIslands;
            currencyHooks = needsCurrencies;
            npcHooks = needsNpcs;
            getLogger().info("Loaded " + loaded.quests().size() + " " + selected + " tutorial quests.");
            return true;
        } catch (Exception ex) {
            getLogger().severe("Tutorial configuration rejected: " + ex.getMessage());
            return false;
        }
    }

    private void migrateAndCreateConfig() throws Exception {
        Path configDirectory = Path.of("mconf", "tutorials");
        Files.createDirectories(configDirectory);

        File oldConfig = new File(getDataFolder(), "config.yml");
        if (oldConfig.isFile()) {
            String oldGamemode = YamlConfiguration.loadConfiguration(oldConfig).getString("active-gamemode");
            if (oldGamemode != null && !oldGamemode.isBlank()) {
                TutorialConf.get().activeGamemode = oldGamemode;
                TutorialConf.get().changed();
                TutorialConf.get().sync();
            }
            Files.delete(oldConfig.toPath());
        }

        File oldGamemodes = new File(getDataFolder(), "gamemodes");
        File[] oldFiles = oldGamemodes.listFiles((dir, name) -> name.endsWith(".yml"));
        if (oldFiles != null) {
            for (File oldFile : oldFiles) {
                Path destination = configDirectory.resolve(oldFile.getName());
                if (Files.notExists(destination)) Files.move(oldFile.toPath(), destination);
                else Files.delete(oldFile.toPath());
            }
            try { Files.deleteIfExists(oldGamemodes.toPath()); }
            catch (java.nio.file.DirectoryNotEmptyException ignored) { /* Keep unrelated files. */ }
        }

        Path skyblock = configDirectory.resolve("skyblock.yml");
        if (!Files.isRegularFile(skyblock)) {
            try (InputStream template = getResource("gamemodes/skyblock.yml")) {
                if (template == null) throw new IllegalStateException("Missing bundled Skyblock tutorial");
                Files.copy(template, skyblock);
            }
        }
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (SyntheticPlayers.isSynthetic(player)) return;
        Bukkit.getScheduler().runTaskLater(this, () -> { if (player.isOnline()) announce(player); }, 20L);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        if (presentation != null) presentation.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (SyntheticPlayers.isSynthetic(player)) return;
        advance(player, "BlockBreak", trigger -> trigger.blocks().isEmpty() || trigger.blocks().contains(event.getBlock().getType()), event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (SyntheticPlayers.isSynthetic(player)) return;
        advance(player, "BlockPlace", trigger -> trigger.type().equals("AnyBlockPlace")
                || trigger.blocks().contains(event.getBlockPlaced().getType()), event.getBlockPlaced().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (SyntheticPlayers.isSynthetic(player)) return;
        String command = event.getMessage().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        advance(player, "Command", trigger -> command.equals(trigger.content().toLowerCase(Locale.ROOT))
                || command.startsWith(trigger.content().toLowerCase(Locale.ROOT) + " "));
    }

    void advance(Player player, String type, Predicate<TutorialDefinition.Trigger> predicate) {
        advance(player, type, predicate, null);
    }

    void advance(Player player, String type, Predicate<TutorialDefinition.Trigger> predicate, org.bukkit.Location location) {
        if (definition == null || !player.isOnline()) return;
        TutorialPlayer data = TutorialPlayerColl.get().get(player);
        if (data == null) return;
        TutorialPlayer.Progress progress = data.progress(key());
        progress.normalize();
        TutorialDefinition.Quest current = current(progress);
        if (current == null) return;
        boolean matched = current.triggers().stream().anyMatch(trigger -> (trigger.type().equals(type)
                || (type.equals("BlockPlace") && trigger.type().equals("AnyBlockPlace")))
                && predicate.test(trigger)
                && (!trigger.ownIsland() || (islandHooks && IslandHooks.onOwnIsland(player, location))));
        if (!matched) return;
        start(player, data, progress, current);
        int count = progress.counts.merge(current.id(), 1, Integer::sum);
        data.changed();
        if (count < current.completionAt()) {
            syncPresentation(player, progress);
            return;
        }
        progress.completed.add(current.id());
        data.changed();
        data.sync(); // Commit the completion before any reward command can run.
        actions(player, current.completeActions(), true);
        TutorialDefinition.Quest next = current(progress);
        objectiveComplete(player, current, next, progress);
        if (next != null) start(player, data, progress, next);
        syncPresentation(player, progress);
    }

    private void announce(Player player) {
        if (definition == null || !player.isOnline()) return;
        TutorialPlayer data = TutorialPlayerColl.get().get(player);
        if (data == null) return;
        TutorialPlayer.Progress progress = data.progress(key());
        progress.normalize();
        TutorialDefinition.Quest quest = current(progress);
        if (quest == null) { if (presentation != null) presentation.clear(player); return; }
        start(player, data, progress, quest);
        syncPresentation(player, progress);
    }

    private void start(Player player, TutorialPlayer data, TutorialPlayer.Progress progress, TutorialDefinition.Quest quest) {
        if (!progress.started.add(quest.id())) return;
        data.changed();
        data.sync();
        actions(player, quest.startActions(), false);
    }

    private TutorialDefinition.Quest current(TutorialPlayer.Progress progress) {
        for (TutorialDefinition.Quest quest : definition.quests())
            if (!progress.completed.contains(quest.id())) return quest;
        return null;
    }

    private String key() { return gamemode + ":" + definition.id(); }

    private void actions(Player player, List<TutorialDefinition.Action> actions, boolean complete) {
        for (TutorialDefinition.Action action : actions) {
            if (action.type().equals("Message")) Feedback.send(player, complete
                    ? Feedback.ok(action.content()) : Feedback.info(action.content(), null));
            else if (action.type().equals("NpcMessage")) Feedback.sendRawMiniMessage(player,
                    "<#2B8C99>[NPC] <#F4D35E>" + action.speaker() + ": <#F5F5F5>" + action.content());
            else if (action.type().equals("Command")) {
                String command = action.command().replace("%player-name%", player.getName());
                if (command.startsWith("/")) command = command.substring(1);
                if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command))
                    getLogger().warning("Tutorial action command failed: " + command);
            }
        }
    }

    boolean reloadTutorial() {
        if (!loadDefinition()) return false;
        for (Player player : Bukkit.getOnlinePlayers()) if (!SyntheticPlayers.isSynthetic(player)) announce(player);
        return true;
    }

    boolean resetTutorial(Player player) {
        if (definition == null) return false;
        TutorialPlayerColl.get().get(player).reset(key());
        announce(player);
        return true;
    }

    void showTutorial(Player player) {
        if (definition == null) { Feedback.send(player, Feedback.warn("No active tutorial on this server.")); return; }
        TutorialPlayer.Progress progress = TutorialPlayerColl.get().get(player).progress(key());
        progress.normalize();
        TutorialDefinition.Quest quest = current(progress);
        if (quest == null) { Feedback.send(player, Feedback.ok("<h>" + definition.name() + "</h> complete!")); return; }
        int count = progress.counts.getOrDefault(quest.id(), 0);
        Feedback.send(player, Feedback.info("<h>" + definition.name() + "</h> — " + quest.name()
                + " <h2>(" + count + "/" + quest.completionAt() + ")</h2>. " + quest.description(), null));
    }

    private void objectiveComplete(Player player, TutorialDefinition.Quest completed,
                                   TutorialDefinition.Quest next, TutorialPlayer.Progress progress) {
        int completedCount = (int) definition.quests().stream().filter(quest -> progress.completed.contains(quest.id())).count();
        String nextText = next == null ? "Tutorial complete" : "Next: " + next.name();
        Feedback.send(player, Feedback.ok("<h>OBJECTIVE COMPLETE!</h> <h2>(" + completedCount + "/"
                + definition.quests().size() + ")</h2> " + nextText).withTitle(new ActionFeedback.TitleConfig(
                "<#6FD96F><b>OBJECTIVE COMPLETE!</b>",
                next == null ? "<#F5F5F5>You're ready for Skyblock." : "<#F5F5F5>" + nextText,
                5, 45, 10)));
    }

    private void syncPresentation(Player player, TutorialPlayer.Progress progress) {
        if (presentation == null) return;
        TutorialDefinition.Quest quest = current(progress);
        if (quest == null) { presentation.clear(player); return; }
        presentation.sync(player, quest, progress.counts.getOrDefault(quest.id(), 0));
    }

    @Override public void onDisable() {
        if (presentation != null) presentation.close();
        super.onDisable();
    }
}
