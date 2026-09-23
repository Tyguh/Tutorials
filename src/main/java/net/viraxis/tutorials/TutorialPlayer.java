package net.viraxis.tutorials;

import com.massivecraft.massivecore.store.SenderEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TutorialPlayer extends SenderEntity<TutorialPlayer> {
    private Map<String, Progress> progress = new HashMap<>();

    public Progress progress(String key) {
        if (progress == null) progress = new HashMap<>();
        return progress.computeIfAbsent(key, ignored -> new Progress());
    }

    public void reset(String key) {
        if (progress != null) progress.remove(key);
        changed();
        sync();
    }

    public static final class Progress {
        public Set<String> completed = new HashSet<>();
        public Set<String> started = new HashSet<>();
        public Map<String, Integer> counts = new HashMap<>();

        public void normalize() {
            if (completed == null) completed = new HashSet<>();
            if (started == null) started = new HashSet<>();
            if (counts == null) counts = new HashMap<>();
        }
    }
}
