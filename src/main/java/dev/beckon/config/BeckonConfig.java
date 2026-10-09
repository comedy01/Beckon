package dev.beckon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class BeckonConfig {
    public static final String FILE_NAME = "beckon.json";

    private static final Logger LOGGER = LogManager.getLogger("beckon");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum Mode {
        VALUABLE,
        ALL,
        LISTED;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private boolean enabled = true;
    private Mode mode = Mode.VALUABLE;
    private float beamHeight = 2.5f;
    private float beamWidth = 1.0f;
    private float opacity = 0.8f;
    private boolean pulse = true;
    private boolean groundGlow = true;
    private boolean labels = true;
    private float labelDistance = 8f;
    private boolean despawnBlink = true;
    private int despawnWarnSeconds = 30;
    private boolean deathDrops = true;
    private boolean merge = true;
    private int maxBeams = 64;
    private List<String> alwaysShow = new ArrayList<>(List.of(
            "diamond", "emerald", "netherite_ingot", "netherite_scrap", "ancient_debris",
            "diamond_block", "emerald_block", "netherite_block", "gold_block",
            "shulker_box", "#c:shulker_boxes", "#c:gems", "#minecraft:music_discs"));
    private List<String> neverShow = new ArrayList<>();

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public Mode mode() {
        return mode == null ? Mode.VALUABLE : mode;
    }

    public void setMode(Mode value) {
        mode = value;
    }

    public float beamHeight() {
        return clamp(beamHeight, 0.5f, 16f);
    }

    public void setBeamHeight(float value) {
        beamHeight = value;
    }

    public float beamWidth() {
        return clamp(beamWidth, 0.25f, 4f);
    }

    public void setBeamWidth(float value) {
        beamWidth = value;
    }

    public float opacity() {
        return clamp(opacity, 0.05f, 1f);
    }

    public void setOpacity(float value) {
        opacity = value;
    }

    public boolean pulse() {
        return pulse;
    }

    public void setPulse(boolean value) {
        pulse = value;
    }

    public boolean groundGlow() {
        return groundGlow;
    }

    public void setGroundGlow(boolean value) {
        groundGlow = value;
    }

    public boolean labels() {
        return labels;
    }

    public void setLabels(boolean value) {
        labels = value;
    }

    public float labelDistance() {
        return clamp(labelDistance, 1f, 64f);
    }

    public void setLabelDistance(float value) {
        labelDistance = value;
    }

    public boolean despawnBlink() {
        return despawnBlink;
    }

    public void setDespawnBlink(boolean value) {
        despawnBlink = value;
    }

    public int despawnWarnTicks() {
        return Math.max(1, Math.min(300, despawnWarnSeconds)) * 20;
    }

    public boolean deathDrops() {
        return deathDrops;
    }

    public void setDeathDrops(boolean value) {
        deathDrops = value;
    }

    public boolean merge() {
        return merge;
    }

    public void setMerge(boolean value) {
        merge = value;
    }

    public int maxBeams() {
        return Math.max(1, Math.min(1024, maxBeams));
    }

    public void setMaxBeams(int value) {
        maxBeams = value;
    }

    public List<String> alwaysShow() {
        return alwaysShow == null ? List.of() : alwaysShow;
    }

    public List<String> neverShow() {
        return neverShow == null ? List.of() : neverShow;
    }

    public void resetToDefaults() {
        BeckonConfig fresh = new BeckonConfig();
        enabled = fresh.enabled;
        mode = fresh.mode;
        beamHeight = fresh.beamHeight;
        beamWidth = fresh.beamWidth;
        opacity = fresh.opacity;
        pulse = fresh.pulse;
        groundGlow = fresh.groundGlow;
        labels = fresh.labels;
        labelDistance = fresh.labelDistance;
        despawnBlink = fresh.despawnBlink;
        despawnWarnSeconds = fresh.despawnWarnSeconds;
        deathDrops = fresh.deathDrops;
        merge = fresh.merge;
        maxBeams = fresh.maxBeams;
        alwaysShow = fresh.alwaysShow;
        neverShow = fresh.neverShow;
    }

    private static float clamp(float value, float min, float max) {
        return Float.isNaN(value) ? min : Math.max(min, Math.min(max, value));
    }

    public static BeckonConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            BeckonConfig fresh = new BeckonConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            BeckonConfig loaded = GSON.fromJson(reader, BeckonConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            BeckonConfig fresh = new BeckonConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.write(temp, (GSON.toJson(this) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
