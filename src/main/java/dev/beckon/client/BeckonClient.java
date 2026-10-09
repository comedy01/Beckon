package dev.beckon.client;

import dev.beckon.config.BeckonConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class BeckonClient {
    public static final String MOD_ID = "beckon";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static BeckonConfig config = new BeckonConfig();
    private static Path configPath;
    private static boolean failed;

    private BeckonClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(BeckonConfig.FILE_NAME);
        config = BeckonConfig.load(configPath);
        Beams.reloadRules(config);
    }

    public static BeckonConfig config() {
        return config;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
        Beams.reloadRules(config);
    }

    public static boolean active() {
        return !failed && config.enabled();
    }

    public static void fail(Throwable e) {
        if (!failed) {
            failed = true;
            LOGGER.error("Beckon hit an error and turned itself off for this session", e);
        }
    }
}
