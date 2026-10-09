package dev.beckon.core;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Rules {
    private final Set<String> ids = new HashSet<>();
    private final Set<String> tags = new HashSet<>();

    public Rules(List<String> entries) {
        if (entries == null) {
            return;
        }
        for (String raw : entries) {
            if (raw == null) {
                continue;
            }
            String entry = raw.trim().toLowerCase(Locale.ROOT);
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.startsWith("#")) {
                tags.add(qualify(entry.substring(1)));
            } else {
                ids.add(qualify(entry));
            }
        }
    }

    public boolean isEmpty() {
        return ids.isEmpty() && tags.isEmpty();
    }

    public boolean matches(String id, Iterable<String> itemTags) {
        if (ids.contains(id)) {
            return true;
        }
        if (tags.isEmpty()) {
            return false;
        }
        for (String tag : itemTags) {
            if (tags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    static String qualify(String id) {
        return id.indexOf(':') < 0 ? "minecraft:" + id : id;
    }
}
