package net.viraxis.tutorials;

import com.massivecraft.massivecore.store.MStore;
import com.massivecraft.massivecore.store.SenderColl;

public class TutorialPlayerColl extends SenderColl<TutorialPlayer> {
    private static final TutorialPlayerColl INSTANCE = new TutorialPlayerColl();
    public static TutorialPlayerColl get() { return INSTANCE; }

    private TutorialPlayerColl() {
        super("tutorials_players", TutorialPlayer.class, MStore.getDb(), TutorialsPlugin.get());
    }
}
