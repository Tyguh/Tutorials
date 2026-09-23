package net.viraxis.tutorials;

import com.massivecraft.massivecore.store.Coll;
import com.massivecraft.massivecore.store.MConf;

public class TutorialConfColl extends Coll<TutorialConf> {
    private static final TutorialConfColl INSTANCE = new TutorialConfColl();
    public static TutorialConfColl get() { return INSTANCE; }

    private TutorialConfColl() {
        super("tutorials_conf", null, MConf.getDb("tutorials_conf"), null);
    }

    @Override public void setActive(boolean active) {
        super.setActive(active);
        if (!active) return;
        TutorialConf.instance = get("conf", true);
        TutorialConf.instance.changed();
        TutorialConf.instance.sync();
    }
}
