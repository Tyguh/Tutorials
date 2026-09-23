package net.viraxis.tutorials;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Validated runtime view of a gamemode's MConf tutorial definition. */
public record TutorialDefinition(String id, String name, List<Quest> quests,
                                 Map<String, NpcInteraction> npcInteractions) {
    public record Quest(String id, String name, String description, int completionAt,
                        PointOfInterest target, List<Trigger> triggers,
                        List<Action> startActions, List<Action> completeActions) {}
    public record Trigger(String type, Set<Material> blocks, boolean ownIsland, String content, String currency,
                          int minimumVotes, String npc) {}
    public record Action(String type, String content, String command, String speaker) {}
    public record PointOfInterest(String npc, String label, String world, Double x, Double y, Double z,
                                  double distanceThreshold) {}
    public record NpcInteraction(String speaker, String message, String sound, String command) {}

    static TutorialDefinition from(String gamemodeId, TutorialGamemode config) {
        if (config == null) throw new IllegalArgumentException("Missing MConf gamemode: " + gamemodeId);
        String tutorialId = required(config.tutorialId, "tutorialId");
        String tutorialName = required(config.name, "name");
        if (config.quests == null || config.quests.isEmpty()) throw new IllegalArgumentException("quests must not be empty");

        List<Quest> quests = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Quest raw : config.quests) {
            if (raw == null) throw new IllegalArgumentException("quest must not be null");
            String questId = required(raw.id(), "quest id");
            if (!ids.add(questId)) throw new IllegalArgumentException("Duplicate quest id: " + questId);
            if (raw.completionAt() < 1) throw new IllegalArgumentException("completionAt must be positive for " + questId);
            List<Trigger> triggers = validateTriggers(questId, raw.triggers());
            PointOfInterest target = validateTarget(questId, raw.target());
            quests.add(new Quest(questId, required(raw.name(), "quest name"),
                    required(raw.description(), "quest description"), raw.completionAt(), target, triggers,
                    validateActions(questId, raw.startActions()), validateActions(questId, raw.completeActions())));
        }

        Map<String, NpcInteraction> interactions = new LinkedHashMap<>();
        if (config.npcInteractions != null) {
            for (Map.Entry<String, NpcInteraction> entry : config.npcInteractions.entrySet()) {
                String npc = required(entry.getKey(), "NPC interaction id").toLowerCase(Locale.ROOT);
                NpcInteraction interaction = entry.getValue();
                if (interaction == null) throw new IllegalArgumentException("Missing NPC interaction for " + npc);
                interactions.put(npc, new NpcInteraction(required(interaction.speaker(), npc + " speaker"),
                        required(interaction.message(), npc + " message"), interaction.sound(), interaction.command()));
            }
        }
        return new TutorialDefinition(tutorialId, tutorialName, List.copyOf(quests), Map.copyOf(interactions));
    }

    private static List<Trigger> validateTriggers(String questId, List<Trigger> raw) {
        if (raw == null || raw.isEmpty()) throw new IllegalArgumentException("No triggers for " + questId);
        List<Trigger> result = new ArrayList<>();
        for (Trigger trigger : raw) {
            if (trigger == null) throw new IllegalArgumentException("Null trigger for " + questId);
            String type = required(trigger.type(), "trigger type");
            if (!Set.of("HasIsland", "BlockBreak", "BlockPlace", "AnyBlockPlace", "Command", "ShopSale",
                    "DiscordLinked", "VoteCount", "FreeRankClaimed", "NpcInteract").contains(type))
                throw new IllegalArgumentException("Unsupported trigger: " + type);
            Set<Material> blocks = trigger.blocks() == null ? Set.of() : Set.copyOf(trigger.blocks());
            if (type.equals("BlockPlace") && blocks.isEmpty()) throw new IllegalArgumentException("BlockPlace requires blocks");
            if (type.equals("Command") && (trigger.content() == null || !trigger.content().startsWith("/")))
                throw new IllegalArgumentException("Command trigger requires /content");
            if (type.equals("VoteCount") && trigger.minimumVotes() < 1)
                throw new IllegalArgumentException("minimumVotes must be positive");
            if (type.equals("NpcInteract")) required(trigger.npc(), "npc trigger name");
            result.add(new Trigger(type, blocks, trigger.ownIsland(), trigger.content(), trigger.currency(),
                    trigger.minimumVotes(), trigger.npc()));
        }
        return List.copyOf(result);
    }

    private static List<Action> validateActions(String questId, List<Action> raw) {
        if (raw == null) return List.of();
        List<Action> result = new ArrayList<>();
        for (Action action : raw) {
            if (action == null) throw new IllegalArgumentException("Null action for " + questId);
            String type = required(action.type(), "action type");
            if (type.equals("Message")) required(action.content(), "message content");
            else if (type.equals("NpcMessage")) {
                required(action.content(), "NPC message content");
                required(action.speaker(), "NPC speaker");
            } else if (type.equals("Command") || type.equals("PlayerCommand")) required(action.command(), "action command");
            else if (type.equals("Sound")) required(action.content(), "sound id");
            else throw new IllegalArgumentException("Unsupported action: " + type);
            result.add(action);
        }
        return List.copyOf(result);
    }

    private static PointOfInterest validateTarget(String questId, PointOfInterest target) {
        if (target == null) return null;
        String npc = target.npc();
        boolean hasPosition = target.world() != null || target.x() != null || target.y() != null || target.z() != null;
        if ((npc == null || npc.isBlank()) && !hasPosition)
            throw new IllegalArgumentException("target requires npc or coordinates for " + questId);
        if (hasPosition && (target.world() == null || target.x() == null || target.y() == null || target.z() == null))
            throw new IllegalArgumentException("coordinate target requires world, x, y, and z for " + questId);
        if (target.distanceThreshold() < 0.0D)
            throw new IllegalArgumentException("distanceThreshold cannot be negative for " + questId);
        String label = target.label();
        if (label == null || label.isBlank()) label = npc == null ? "Objective" : npc;
        return new PointOfInterest(npc, label, target.world(), target.x(), target.y(), target.z(), target.distanceThreshold());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + field);
        return value;
    }
}
