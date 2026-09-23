package net.viraxis.tutorials;

import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.Txt;
import net.kyori.adventure.bossbar.BossBar;
import net.viraxis.holograms.HologramsPlugin;
import net.viraxis.holograms.api.ViraxisHologram;
import net.viraxis.holograms.entity.HologramBillboard;
import net.viraxis.holograms.entity.HologramData;
import net.viraxis.holograms.entity.HologramType;
import net.viraxis.holograms.entity.HologramVisibility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owns the per-player objective bar and packet hologram. */
final class TutorialPresentation {
    private static final double MARKER_DISTANCE = 3.2D;
    private static final double MARKER_HEIGHT = 0.45D;
    private static final double BOB_AMPLITUDE = 0.09D;
    private static final double BOB_SPEED = 0.055D;
    private static final double POSITION_LEAD_TICKS = 0.75D;
    private static final double MAX_PREDICTED_MOTION_SQUARED = 4.0D;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final BukkitTask task;
    private long ticks;

    TutorialPresentation(TutorialsPlugin plugin) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void sync(Player player, TutorialDefinition.Quest quest, int count, int questIndex, int questTotal) {
        if (player == null || !player.isOnline() || quest == null) {
            clear(player);
            return;
        }
        Session session = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new Session(player));
        session.quest = quest;
        session.count = Math.min(count, quest.completionAt());
        session.questIndex = questIndex;
        session.questTotal = questTotal;
        session.updateBossBar(player);
    }

    void clear(Player player) {
        if (player != null) clear(player.getUniqueId());
    }

    private void clear(UUID playerId) {
        Session session = sessions.remove(playerId);
        if (session != null) session.destroy(Bukkit.getPlayer(playerId));
    }

    void close() {
        task.cancel();
        for (UUID playerId : List.copyOf(sessions.keySet())) clear(playerId);
    }

    private void tick() {
        ticks++;
        for (Map.Entry<UUID, Session> entry : List.copyOf(sessions.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                clear(entry.getKey());
                continue;
            }
            entry.getValue().updateMarker(player);
        }
    }

    private final class Session {
        private final String hologramId;
        private BossBar bossBar;
        private ViraxisHologram hologram;
        private TutorialDefinition.Quest quest;
        private int count;
        private int questIndex;
        private int questTotal;
        private boolean markerVisible;
        private UUID regionWorld;
        private int regionX = Integer.MIN_VALUE;
        private int regionY = Integer.MIN_VALUE;
        private int regionZ = Integer.MIN_VALUE;
        private boolean insideSpawn;
        private Location previousEye;
        private double groundedEyeY = Double.NaN;

        private Session(Player player) {
            this.hologramId = "tutorial-objective-" + player.getUniqueId();
        }

        private void updateBossBar(Player player) {
            float progress = Math.max(0.0F, Math.min(1.0F, count / (float) Math.max(1, quest.completionAt())));
            String counter = quest.completionAt() > 1 ? " <gray>(" + count + "/" + quest.completionAt() + ")</gray>" : "";
            var title = Txt.colorize(player, "<#66D9E8><b>Objective:</b> <white>" + quest.name() + counter
                    + " <dark_gray>[" + questIndex + "/" + questTotal + "]</dark_gray>");
            if (bossBar == null) {
                bossBar = BossBar.bossBar(title, progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
                player.showBossBar(bossBar);
            } else {
                bossBar.name(title);
                bossBar.progress(progress);
            }
        }

        private void updateMarker(Player player) {
            if (!isInsideSpawn(player.getLocation())) {
                hideMarker(player);
                return;
            }

            Location playerEye = player.getEyeLocation();
            Location target = resolveTarget(quest.target());
            double targetDistance = -1.0D;
            Vector targetDirection = null;
            if (target != null && target.getWorld() == playerEye.getWorld()) {
                targetDistance = target.distance(playerEye);
                targetDirection = target.toVector().subtract(playerEye.toVector());
            }

            Vector viewDirection = horizontalDirection(playerEye);
            Location markerEye = stableMarkerEye(player, playerEye);
            Location predictedEye = predictNextEye(markerEye);
            double bob = Math.sin((ticks + POSITION_LEAD_TICKS) * BOB_SPEED) * BOB_AMPLITUDE;
            Location marker = predictedEye.add(viewDirection.clone().multiply(MARKER_DISTANCE))
                    .add(0.0D, MARKER_HEIGHT + bob, 0.0D);

            String arrow = targetDirection == null ? "↑" : DirectionArrow.between(
                    viewDirection.getX(), viewDirection.getZ(), targetDirection.getX(), targetDirection.getZ());
            String text = "<#E69A30><b>" + arrow + "</b> <#F4D35E>" + quest.name();
            if (quest.target() != null && targetDistance > quest.target().distanceThreshold())
                text += " <gray>(" + String.format(java.util.Locale.ROOT, "%.1fm", targetDistance) + ")</gray>";

            if (hologram == null) {
                HologramData data = new HologramData();
                data.type = HologramType.TEXT;
                data.location = PS.valueOf(marker);
                data.text = List.of(text);
                data.visibility = HologramVisibility.MANUAL;
                data.visibilityDistance = 12;
                data.teleportDuration = 1;
                data.textShadow = true;
                data.seeThrough = true;
                data.background = "transparent";
                data.billboard = HologramBillboard.CENTER;
                data.persistent = false;
                hologram = HologramsPlugin.get().getHolograms().createRuntime(hologramId, data);
                hologram.show(player);
                markerVisible = true;
            } else {
                HologramData data = hologram.getData();
                data.location = PS.valueOf(marker);
                if (!data.text.equals(List.of(text))) data.text = List.of(text);
                if (markerVisible) hologram.refresh(player);
                else {
                    hologram.show(player);
                    markerVisible = true;
                }
            }
        }

        private void hideMarker(Player player) {
            previousEye = null;
            groundedEyeY = Double.NaN;
            if (hologram == null || !markerVisible) return;
            hologram.hideManual(player);
            markerVisible = false;
        }

        private Location predictNextEye(Location playerEye) {
            Location predicted = playerEye.clone();
            if (previousEye != null && previousEye.getWorld() == playerEye.getWorld()) {
                Vector motion = playerEye.toVector().subtract(previousEye.toVector());
                if (motion.lengthSquared() <= MAX_PREDICTED_MOTION_SQUARED)
                    predicted.add(motion.multiply(POSITION_LEAD_TICKS));
            }
            previousEye = playerEye.clone();
            return predicted;
        }

        private Location stableMarkerEye(Player player, Location playerEye) {
            if (Double.isNaN(groundedEyeY) || player.isOnGround() || player.isFlying()
                    || Math.abs(playerEye.getY() - groundedEyeY) > 2.5D)
                groundedEyeY = playerEye.getY();
            Location markerEye = playerEye.clone();
            if (!player.isOnGround() && !player.isFlying()) markerEye.setY(groundedEyeY);
            return markerEye;
        }

        private boolean isInsideSpawn(Location location) {
            UUID world = location.getWorld().getUID();
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            if (!world.equals(regionWorld) || x != regionX || y != regionY || z != regionZ) {
                regionWorld = world;
                regionX = x;
                regionY = y;
                regionZ = z;
                insideSpawn = SpawnRegion.contains(location);
            }
            return insideSpawn;
        }

        private Location resolveTarget(TutorialDefinition.PointOfInterest target) {
            if (target == null) return null;
            if (target.npc() != null && !target.npc().isBlank()) return FancyNpcHooks.location(target.npc());
            World world = Bukkit.getWorld(target.world());
            return world == null ? null : new Location(world, target.x(), target.y(), target.z());
        }

        private void destroy(Player player) {
            if (bossBar != null && player != null) player.hideBossBar(bossBar);
            if (hologram != null && HologramsPlugin.get() != null)
                HologramsPlugin.get().getHolograms().remove(hologramId);
            markerVisible = false;
        }
    }

    static Vector horizontalDirection(Location location) {
        Vector direction = location.getDirection().setY(0.0D);
        if (direction.lengthSquared() > 1.0E-6D) return direction.normalize();
        double yaw = Math.toRadians(location.getYaw());
        return new Vector(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

}
