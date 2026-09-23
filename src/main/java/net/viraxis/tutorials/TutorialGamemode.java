package net.viraxis.tutorials;

import com.massivecraft.massivecore.store.Entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One gamemode's complete tutorial and NPC interaction configuration. */
public final class TutorialGamemode extends Entity<TutorialGamemode> {
    public String tutorialId = "getting-started";
    public String name = "Getting Started";
    public List<TutorialDefinition.Quest> quests = new ArrayList<>();
    public Map<String, TutorialDefinition.NpcInteraction> npcInteractions = new LinkedHashMap<>();
}
