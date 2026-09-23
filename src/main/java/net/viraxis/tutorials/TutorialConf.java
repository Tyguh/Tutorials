package net.viraxis.tutorials;

import com.massivecraft.massivecore.store.Entity;

public class TutorialConf extends Entity<TutorialConf> {
    static transient TutorialConf instance;
    public String activeGamemode = "skyblock";

    public static TutorialConf get() { return instance; }
}
