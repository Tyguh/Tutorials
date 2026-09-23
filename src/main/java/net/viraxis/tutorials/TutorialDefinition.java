package net.viraxis.tutorials;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** A gamemode's ordered, admin-editable tutorial. Quest ids are stable progress keys. */
public record TutorialDefinition(String id, String name, List<Quest> quests) {
    public record Quest(String id, String name, String description, int completionAt,
                        PointOfInterest target, List<Trigger> triggers,
                        List<Action> startActions, List<Action> completeActions) {}
    public record Trigger(String type, Set<Material> blocks, boolean ownIsland, String content, String currency,
                          int minimumVotes, String npc) {}
    public record Action(String type, String content, String command, String speaker) {}
    public record PointOfInterest(String npc, String label, String world, Double x, Double y, Double z,
                                  double distanceThreshold) {}

    public static TutorialDefinition load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = required(yaml.getString("id"), "tutorial id");
        String name = required(yaml.getString("name"), "tutorial name");
        List<?> rawQuests = yaml.getList("quests");
        if (rawQuests == null || rawQuests.isEmpty()) throw new IllegalArgumentException("quests must not be empty");
        List<OrderedQuest> ordered = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (Object object : rawQuests) {
            Map<?, ?> data = map(object, "quest");
            String questId = required(string(data, "id"), "quest id");
            int order = number(data, "order", 0);
            if (order < 1 || !orders.add(order)) throw new IllegalArgumentException("Invalid/duplicate quest order: " + order);
            if (!ids.add(questId)) throw new IllegalArgumentException("Duplicate quest id: " + questId);
            int completion = number(data, "completion-at", 1);
            if (completion < 1) throw new IllegalArgumentException("completion-at must be positive for " + questId);
            List<Trigger> triggers = triggers(data.get("triggers"));
            if (triggers.isEmpty()) throw new IllegalArgumentException("No triggers for " + questId);
            Quest quest = new Quest(questId, required(string(data, "name"), "quest name"),
                    required(string(data, "description"), "quest description"), completion,
                    target(data.get("target")), triggers,
                    actions(data.get("start-actions")), actions(data.get("complete-actions")));
            ordered.add(new OrderedQuest(order, quest));
        }
        ordered.sort(Comparator.comparingInt(OrderedQuest::order));
        return new TutorialDefinition(id, name, ordered.stream().map(OrderedQuest::quest).toList());
    }

    private record OrderedQuest(int order, Quest quest) {}

    private static List<Trigger> triggers(Object raw) {
        List<Trigger> result = new ArrayList<>();
        for (Object object : list(raw, "triggers")) {
            Map<?, ?> data = map(object, "trigger");
            String type = required(string(data, "type"), "trigger type");
            if (!Set.of("HasIsland", "BlockBreak", "BlockPlace", "AnyBlockPlace", "Command", "ShopSale",
                    "DiscordLinked", "VoteCount", "FreeRankClaimed", "NpcInteract").contains(type))
                throw new IllegalArgumentException("Unsupported trigger: " + type);
            Set<Material> blocks = new HashSet<>();
            Object rawBlocks = data.containsKey("blocks") ? data.get("blocks") : data.get("block");
            if (rawBlocks instanceof List<?> items) {
                for (Object item : items) blocks.add(material(item));
            } else if (rawBlocks != null) blocks.add(material(rawBlocks));
            if (type.equals("BlockPlace") && blocks.isEmpty()) throw new IllegalArgumentException("BlockPlace requires block(s)");
            String content = string(data, "content");
            if (type.equals("Command") && (content == null || !content.startsWith("/")))
                throw new IllegalArgumentException("Command trigger requires /content");
            boolean ownIsland = Boolean.parseBoolean(String.valueOf(data.containsKey("own-island") ? data.get("own-island") : "false"));
            int minimumVotes = number(data, "minimum-votes", 1);
            if (type.equals("VoteCount") && minimumVotes < 1) throw new IllegalArgumentException("minimum-votes must be positive");
            String npc = string(data, "npc");
            if (type.equals("NpcInteract")) required(npc, "npc trigger name");
            result.add(new Trigger(type, Set.copyOf(blocks), ownIsland, content, string(data, "currency"), minimumVotes, npc));
        }
        return List.copyOf(result);
    }

    private static List<Action> actions(Object raw) {
        List<Action> result = new ArrayList<>();
        for (Object object : list(raw, "actions")) {
            Map<?, ?> data = map(object, "action");
            String type = required(string(data, "type"), "action type");
            if (type.equals("Message")) result.add(new Action(type, required(string(data, "content"), "message content"), null, null));
            else if (type.equals("NpcMessage")) result.add(new Action(type,
                    required(string(data, "content"), "NPC message content"), null,
                    required(string(data, "speaker"), "NPC speaker")));
            else if (type.equals("Command")) {
                if (!"Console".equalsIgnoreCase(string(data, "source")))
                    throw new IllegalArgumentException("Only Console commands are supported");
                result.add(new Action(type, null, required(string(data, "command"), "action command"), null));
            } else throw new IllegalArgumentException("Unsupported action: " + type);
        }
        return List.copyOf(result);
    }

    private static PointOfInterest target(Object raw) {
        if (raw == null) return null;
        Map<?, ?> data = map(raw, "target");
        String npc = string(data, "npc");
        String world = string(data, "world");
        Double x = decimal(data, "x");
        Double y = decimal(data, "y");
        Double z = decimal(data, "z");
        boolean hasPosition = world != null || x != null || y != null || z != null;
        if ((npc == null || npc.isBlank()) && !hasPosition)
            throw new IllegalArgumentException("target requires npc or world/x/y/z");
        if (hasPosition && (world == null || x == null || y == null || z == null))
            throw new IllegalArgumentException("coordinate target requires world, x, y, and z");
        double threshold = decimal(data, "distance-threshold", 10.0D);
        if (threshold < 0.0D) throw new IllegalArgumentException("distance-threshold cannot be negative");
        String label = string(data, "label");
        if (label == null || label.isBlank()) label = npc == null ? "Objective" : npc;
        return new PointOfInterest(npc, label, world, x, y, z, threshold);
    }

    private static List<?> list(Object value, String field) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) return list;
        throw new IllegalArgumentException(field + " must be a list");
    }

    private static Map<?, ?> map(Object value, String field) {
        if (value instanceof Map<?, ?> map) return map;
        throw new IllegalArgumentException(field + " must be an object");
    }

    private static String string(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + field);
        return value;
    }

    private static int number(Map<?, ?> data, String key, int fallback) {
        Object value = data.get(key);
        if (value == null) return fallback;
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid " + key + ": " + value); }
    }

    private static Double decimal(Map<?, ?> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid " + key + ": " + value); }
    }

    private static double decimal(Map<?, ?> data, String key, double fallback) {
        Double value = decimal(data, key);
        return value == null ? fallback : value;
    }

    private static Material material(Object value) {
        Material material = Material.matchMaterial(String.valueOf(value).toUpperCase(Locale.ROOT));
        if (material == null) throw new IllegalArgumentException("Unknown material: " + value);
        return material;
    }
}
