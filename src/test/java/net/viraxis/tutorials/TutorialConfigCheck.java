package net.viraxis.tutorials;

import com.massivecraft.massivecore.xlib.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Small config contract check for the shipped Skyblock progression. */
public final class TutorialConfigCheck {
    public static void main(String[] args) throws Exception {
        TutorialGamemode config = new GsonBuilder().create()
                .fromJson(Files.readString(Path.of(args[0])), TutorialGamemode.class);
        TutorialDefinition line = TutorialDefinition.from("skyblock", config);
        List<String> ids = line.quests().stream().map(TutorialDefinition.Quest::id).toList();
        if (!ids.equals(List.of("meet-guide", "discord", "vote", "island", "generator", "expand",
                "shop", "first-sale", "settings", "free-rank", "enchanter", "tinkerer", "coin-shop",
                "outpost", "island-top"))) throw new AssertionError("Unexpected quest order: " + ids);
        if (!line.quests().getFirst().triggers().getFirst().type().equals("NpcInteract"))
            throw new AssertionError("The tutorial must begin with an NPC interaction");
        if (!"discord".equals(line.quests().getFirst().target().npc()))
            throw new AssertionError("The first marker must track the Discord NPC");
        if (!line.quests().get(1).triggers().getFirst().type().equals("DiscordLinked"))
            throw new AssertionError("Discord must use a linked-account check");
        if (!"discord".equals(line.quests().get(1).target().npc()))
            throw new AssertionError("The Discord linking objective must continue tracking the Discord NPC");
        if (line.quests().get(2).triggers().getFirst().minimumVotes() != 2)
            throw new AssertionError("Vote count must be two");
        if (!line.quests().get(9).triggers().getFirst().type().equals("FreeRankClaimed"))
            throw new AssertionError("Rank must use a claim check");
        if (!line.quests().get(10).id().equals("enchanter"))
            throw new AssertionError("The guide must continue after the free-rank objective");
        if (!line.npcInteractions().containsKey("discord") || !line.npcInteractions().containsKey("enchanter"))
            throw new AssertionError("Tutorial-managed NPC interactions are missing");
        if (line.quests().get(10).completeActions().stream().noneMatch(action -> action.type().equals("PlayerCommand")))
            throw new AssertionError("The Enchanter objective must open its menu through Tutorials");
        verifyArrow(0, 1, 0, 1, "↑");
        verifyArrow(0, 1, -1, 1, "↗");
        verifyArrow(0, 1, -1, 0, "→");
        verifyArrow(0, 1, 0, -1, "↓");
        verifyArrow3d(0, 0, 1, 0, -1, 1, "↓");
        verifyArrow3d(0, 0, 1, 0, 1, 1, "↑");
        verifyArrow3d(0, 0.94, 0.342, 0, 0, 1, "↓");
        verifyArrow3d(0, 0.7, 0.7, 1, 0, 1, "↙");
        verifyArrow3d(0, 0.7, 0.7, -1, 0, 1, "↘");
        verifyArrowAngles(0, -90, -1, 0, 0, "↘");
        verifyArrowAngles(0, -90, 1, 0, 0, "↙");
        verifyArrowAngles(90, 90, 0, 0, 1, "↖");
        verifyArrow(0, 1, 1, 0, "←");
        System.out.println("Verified " + ids.size() + " Skyblock tutorial quests");
    }

    private static void verifyArrow(double viewX, double viewZ, double targetX, double targetZ, String expected) {
        String actual = DirectionArrow.between(viewX, viewZ, targetX, targetZ);
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static void verifyArrow3d(double viewX, double viewY, double viewZ,
                                      double targetX, double targetY, double targetZ, String expected) {
        String actual = DirectionArrow.between(viewX, viewY, viewZ, targetX, targetY, targetZ);
        if (!expected.equals(actual))
            throw new IllegalStateException("Expected arrow " + expected + " but got " + actual);
    }

    private static void verifyArrowAngles(float viewYaw, float viewPitch,
                                          double targetX, double targetY, double targetZ, String expected) {
        String actual = DirectionArrow.between(viewYaw, viewPitch, targetX, targetY, targetZ);
        if (!expected.equals(actual))
            throw new IllegalStateException("Expected angle arrow " + expected + " but got " + actual);
    }
}
