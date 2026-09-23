package net.viraxis.tutorials;

import com.massivecraft.massivecore.store.Coll;
import com.massivecraft.massivecore.store.MConf;

public final class TutorialGamemodeColl extends Coll<TutorialGamemode> {
    private static final TutorialGamemodeColl INSTANCE = new TutorialGamemodeColl();

    public static TutorialGamemodeColl get() { return INSTANCE; }

    private TutorialGamemodeColl() {
        super("tutorials_gamemodes", TutorialGamemode.class, MConf.getDb("tutorials_gamemodes"), TutorialsPlugin.get());
    }
}
