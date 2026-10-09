package dev.beckon.showcase;

import com.mojang.datafixers.util.Pair;
import dev.beckon.client.BeckonClient;
import dev.beckon.client.gui.BeckonSettingsScreen;
import dev.beckon.config.BeckonConfig;
import dev.beckon.showcase.mixin.ItemEntityAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class Showcase {
    private static final String WORLD = "beckon-showcase";
    private static final long SETTLE_NANOS = 3_000_000_000L;
    private static final int GLIDE = 24;
    private static final int EDGE = 30;
    private static final int DUSK = 12600;
    private static final int NIGHT = 18000;
    private static final String GLINT = "{\"minecraft:enchantment_glint_override\":true}";
    private static final String[] ARROW = {
            "B",
            "BB",
            "BWB",
            "BWWB",
            "BWWWB",
            "BWWWWB",
            "BWWWWWB",
            "BWWWWWWB",
            "BWWWWWWWB",
            "BWWWWWWWWB",
            "BWWWWWWWWWB",
            "BWWWWWWBBBBB",
            "BWWWBWWB",
            "BWWB BWWB",
            "BWB  BWWB",
            "BB    BWWB",
            "B     BWWB",
            "       BWWB",
            "        BB"};
    private static Showcase instance;

    private final Path out;
    private final Recorder recorder;
    private final long seed;
    private final List<Action> actions = new ArrayList<>();
    private final AtomicReference<BlockPos> site = new AtomicReference<>();
    private final Set<Integer> aged = new HashSet<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int frame;
    private long actionStart;
    private Path pendingStill;
    private boolean cursorShown;
    private double cursorX;
    private double cursorY;

    private interface Action {
        boolean run(int frame);
    }

    private record Stop(int frame, Supplier<double[]> where) {
    }

    private Showcase(Path out, String ffmpeg, long seed) {
        this.out = out;
        this.recorder = new Recorder(ffmpeg);
        this.seed = seed;
    }

    public static void start() {
        String ffmpeg = System.getProperty("beckon.ffmpeg", "ffmpeg");
        long seed = Long.parseLong(System.getProperty("beckon.seed", "12345"));
        instance = new Showcase(Path.of(System.getProperty("beckon.showcase")), ffmpeg, seed);
    }

    public static boolean hideHand() {
        return instance != null && instance.started;
    }

    public static boolean hideHud() {
        return instance != null && instance.started;
    }

    public static void frame() {
        if (instance != null) {
            instance.onFrame();
        }
    }

    public static void drawCursor(GuiGraphicsExtractor graphics) {
        if (instance == null || !instance.started || !instance.cursorShown) {
            return;
        }
        float cell = 2.0F / (float) Minecraft.getInstance().getWindow().getGuiScale();
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) instance.cursorX, (float) instance.cursorY);
        graphics.pose().scale(cell, cell);
        for (int row = 0; row < ARROW.length; row++) {
            String line = ARROW[row];
            int x = 0;
            while (x < line.length()) {
                char c = line.charAt(x);
                int end = x;
                while (end < line.length() && line.charAt(end) == c) {
                    end++;
                }
                if (c != ' ') {
                    graphics.fill(x, row, end, row + 1, c == 'B' ? 0xFF000000 : 0xFFFFFFFF);
                }
                x = end;
            }
        }
        graphics.pose().popMatrix();
    }

    private void onFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 120) {
                started = true;
                setUpOptions(mc);
                plan(mc);
                log("creating world with seed " + seed);
                deleteWorld(mc);
                Worlds.create(mc, WORLD, false, seed);
            }
            return;
        }
        Clock.step();
        mc.gui.toastManager().clear();
        try {
            while (index < actions.size()) {
                if (frame == 0) {
                    actionStart = System.nanoTime();
                }
                if (!actions.get(index).run(frame++)) {
                    if (cursorShown && Screens.current(mc) != null) {
                        double scale = mc.getWindow().getGuiScale();
                        Ui.moveMouse(mc, cursorX * scale, cursorY * scale);
                    }
                    return;
                }
                index++;
                frame = 0;
            }
        } catch (Throwable e) {
            log("FAILED: " + e);
            e.printStackTrace();
        }
        finish(mc);
    }

    private void finish(Minecraft mc) {
        finished = true;
        Clock.release();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(90_000L);
            } catch (InterruptedException e) {
                return;
            }
            log("the game did not close, forcing it");
            Runtime.getRuntime().halt(1);
        }, "beckon-showcase-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        BeckonClient.config().resetToDefaults();
        BeckonClient.saveConfig();
        log("showcase done");
        mc.stop();
    }

    private static void setUpOptions(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.renderDistance().set(12);
        mc.options.bobView().set(false);
        mc.options.particles().set(ParticleStatus.MINIMAL);
        mc.options.gamma().set(Double.parseDouble(System.getProperty("beckon.gamma", "0.5")));
        mc.options.fov().set(Integer.getInteger("beckon.fov", 65));
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.guiScale().set(3);
        mc.resizeGui();
    }

    private void once(Runnable action) {
        actions.add(f -> {
            action.run();
            return true;
        });
    }

    private void until(BooleanSupplier ready) {
        actions.add(f -> ready.getAsBoolean());
    }

    private void holdFor(int frames, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= frames;
        });
    }

    private void settle(Minecraft mc, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= 90 && System.nanoTime() - actionStart > SETTLE_NANOS
                    && (mc.levelRenderer.hasRenderedAllSections() || System.nanoTime() - actionStart > 20 * SETTLE_NANOS);
        });
    }

    private void record(String name, int frames, IntConsumer script) {
        actions.add(f -> {
            if (f == 0) {
                log("recording " + name);
                recorder.start(out.resolve("clips").resolve(name + ".mp4"));
            } else {
                Path still = pendingStill;
                pendingStill = null;
                recorder.capture(still);
            }
            if (f < frames) {
                script.accept(f);
                return false;
            }
            return true;
        });
        until(recorder::drained);
        once(recorder::stop);
    }

    private void shot(String name) {
        pendingStill = out.resolve("stills").resolve(name + ".png");
    }

    static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void cmd(Minecraft mc, String format, Object... args) {
        run(mc, String.format(Locale.ROOT, format, args));
    }

    private static void face(Minecraft mc, double yaw, double pitch) {
        LocalPlayer player = mc.player;
        player.setYRot((float) yaw);
        player.setXRot((float) pitch);
        player.yRotO = (float) yaw;
        player.xRotO = (float) pitch;
        player.setYHeadRot((float) yaw);
        player.yHeadRotO = (float) yaw;
        player.yBodyRot = (float) yaw;
        player.yBodyRotO = (float) yaw;
    }

    private static void lookAt(Minecraft mc, Vec3 target) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        face(mc, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static void place(Minecraft mc, Vec3 feet) {
        LocalPlayer player = mc.player;
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(feet.x, feet.y, feet.z);
        player.xo = feet.x;
        player.yo = feet.y;
        player.zo = feet.z;
        player.xOld = feet.x;
        player.yOld = feet.y;
        player.zOld = feet.z;
    }

    private void cursor(int f, Stop... stops) {
        double[] at = stops[0].where().get();
        for (int i = 1; i < stops.length; i++) {
            Stop stop = stops[i];
            if (f >= stop.frame()) {
                at = stop.where().get();
            } else if (f >= stop.frame() - GLIDE) {
                double[] to = stop.where().get();
                double s = smooth((f - (stop.frame() - GLIDE)) / (double) GLIDE);
                at = new double[] {lerp(at[0], to[0], s), lerp(at[1], to[1], s)};
                break;
            } else {
                break;
            }
        }
        cursorShown = true;
        cursorX = at[0];
        cursorY = at[1];
    }

    private static boolean wanted(String scene) {
        String only = System.getProperty("beckon.scenes");
        return only == null || List.of(only.split(",")).contains(scene);
    }

    private Vec3 at(double dx, double dy, double dz) {
        BlockPos c = site.get();
        return new Vec3(c.getX() + dx + 0.5, c.getY() + dy, c.getZ() + dz + 0.5);
    }

    private static int height(ServerLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
    }

    private void locate(Minecraft mc) {
        once(() -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                ChunkGenerator generator = level.getChunkSource().getGenerator();
                RandomState noise = level.getChunkSource().randomState();
                Pair<BlockPos, Holder<Biome>> pair = level.findClosestBiome3d(h -> h.is(Biomes.PLAINS),
                        BlockPos.ZERO, 6400, 32, 64);
                BlockPos found = pair == null ? BlockPos.ZERO : pair.getFirst();
                BlockPos best = null;
                int bestScore = Integer.MAX_VALUE;
                for (int ox = -1536; ox <= 1536; ox += 96) {
                    for (int oz = -1536; oz <= 1536; oz += 96) {
                        int cx = found.getX() + ox;
                        int cz = found.getZ() + oz;
                        int centre = generator.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, level, noise);
                        if (!level.getBiome(new BlockPos(cx, centre, cz)).is(Biomes.PLAINS)) {
                            continue;
                        }
                        List<Integer> heights = new ArrayList<>();
                        int wet = 0;
                        for (int dx = -40; dx <= 40; dx += 8) {
                            for (int dz = -40; dz <= 40; dz += 8) {
                                int top = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.WORLD_SURFACE_WG,
                                        level, noise);
                                int floor = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.OCEAN_FLOOR_WG,
                                        level, noise);
                                heights.add(top);
                                if (top != floor) {
                                    wet++;
                                }
                            }
                        }
                        int usual = mode(heights);
                        int score = wet * 3;
                        for (int h : heights) {
                            if (Math.abs(h - usual) > 1) {
                                score++;
                            }
                        }
                        if (score < bestScore) {
                            bestScore = score;
                            best = new BlockPos(cx, centre, cz);
                        }
                    }
                }
                site.set(best == null ? found : best);
                log("plains at " + site.get().toShortString() + ", " + bestScore + " uneven samples");
            });
        });
        until(() -> site.get() != null);
    }

    private static int mode(List<Integer> values) {
        Map<Integer, Integer> counts = new HashMap<>();
        int best = values.get(0);
        for (int value : values) {
            int count = counts.merge(value, 1, Integer::sum);
            if (count > counts.get(best)) {
                best = value;
            }
        }
        return best;
    }

    private void level(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        int ground = server.submit(() -> {
            ServerLevel level = server.overworld();
            BlockPos c = site.get();
            List<Integer> heights = new ArrayList<>();
            for (int dx = -EDGE; dx <= EDGE; dx += 2) {
                for (int dz = -EDGE; dz <= EDGE; dz += 2) {
                    heights.add(height(level, c.getX() + dx, c.getZ() + dz));
                }
            }
            return mode(heights);
        }).join();
        BlockPos c = site.get();
        site.set(new BlockPos(c.getX(), ground + 1, c.getZ()));
        log("meadow at y " + (ground + 1));
    }

    private static void fill(Minecraft mc, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        int layers = Math.max(1, 32768 / ((Math.abs(x2 - x1) + 1) * (Math.abs(z2 - z1) + 1)));
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y += layers) {
            cmd(mc, "fill %d %d %d %d %d %d %s", x1, y, z1, x2, Math.min(y + layers - 1, Math.max(y1, y2)), z2, block);
        }
    }

    /** A flat meadow ringed by trees. The middle, where the loot lies, keeps only a few flowers so nothing hides it. */
    private void buildMeadow(Minecraft mc) {
        BlockPos c = site.get();
        int x = c.getX();
        int z = c.getZ();
        int ground = c.getY() - 1;
        fill(mc, x - EDGE, ground + 1, z - EDGE, x + EDGE, ground + 40, z + EDGE, "minecraft:air");
        fill(mc, x - EDGE, ground - 6, z - EDGE, x + EDGE, ground - 1, z + EDGE, "minecraft:dirt");
        fill(mc, x - EDGE, ground, z - EDGE, x + EDGE, ground, z + EDGE, "minecraft:grass_block");
        String[] kinds = {"oak", "birch", "fancy_oak"};
        int[][] trees = {{-9, -19}, {7, -21}, {-19, -15}, {18, -17}, {-3, -26}, {22, -24}, {-24, -23}, {13, -14},
                {-14, -12}, {25, -9}, {-26, -6}, {-12, 16}, {10, 17}, {-22, 12}, {21, 13}, {-4, 21}, {16, 23}};
        for (int i = 0; i < trees.length; i++) {
            cmd(mc, "place feature minecraft:%s %d %d %d", kinds[i % kinds.length], x + trees[i][0], ground + 1,
                    z + trees[i][1]);
        }
        Random random = new Random(seed);
        String[] flowers = {"dandelion", "poppy", "oxeye_daisy", "cornflower", "azure_bluet", "allium"};
        for (int dx = -EDGE + 1; dx <= EDGE - 1; dx++) {
            for (int dz = -EDGE + 1; dz <= EDGE - 1; dz++) {
                boolean clear = Math.abs(dx) <= 9 && dz >= -9 && dz <= 10;
                double roll = random.nextDouble();
                String block;
                if (clear) {
                    if (roll >= 0.035) {
                        continue;
                    }
                    block = flowers[random.nextInt(flowers.length)];
                } else if (roll < 0.18) {
                    block = "short_grass";
                } else if (roll < 0.23) {
                    block = flowers[random.nextInt(flowers.length)];
                } else if (roll < 0.24) {
                    block = "tall_grass";
                } else {
                    continue;
                }
                cmd(mc, "setblock %d %d %d minecraft:%s keep", x + dx, ground + 1, z + dz, block);
            }
        }
        run(mc, "kill @e[type=!minecraft:player]");
    }

    /** Lays an item on the ground that never despawns and can't be picked up. */
    private void drop(Minecraft mc, String item, int count, Vec3 pos, String components) {
        cmd(mc, "summon minecraft:item %.3f %.3f %.3f {Item:{id:\"minecraft:%s\",count:%d%s},Motion:[0.0,0.0,0.0],"
                        + "PickupDelay:32767,Age:-32768}", pos.x, pos.y, pos.z, item, count,
                components == null ? "" : ",components:" + components);
    }

    private void drop(Minecraft mc, String item, int count, double dx, double dz) {
        drop(mc, item, count, at(dx, 0.05, dz), null);
    }

    private static void kit(Minecraft mc, String... items) {
        run(mc, "clear @p");
        for (int i = 0; i < items.length; i++) {
            cmd(mc, "item replace entity @p hotbar.%d with minecraft:%s", i, items[i]);
        }
    }

    private void scene(Minecraft mc, BeckonConfig config, int time, Vec3 feet, Runnable setup, Runnable hold) {
        once(() -> {
            Screens.open(mc, null);
            config.resetToDefaults();
            cursorShown = false;
            aged.clear();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            run(mc, "gamemode creative @p");
            run(mc, "clear @p");
            run(mc, "attribute @p minecraft:block_interaction_range base set 0");
            mc.options.gamma().set(Double.parseDouble(System.getProperty("beckon.gamma", "0.5")));
            cmd(mc, "time set %d", time);
            cmd(mc, "tp @p %.3f %.3f %.3f", feet.x, feet.y, feet.z);
            run(mc, "kill @e[type=!minecraft:player]");
        });
        holdFor(40, () -> {
            place(mc, feet);
            hold.run();
        });
        once(setup);
        settle(mc, () -> {
            place(mc, feet);
            hold.run();
        });
    }

    /** Client-side item age, which drives Beckon's despawn blink; ages are set once per item. */
    private void age(Minecraft mc, String id, int age) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item && !aged.contains(item.getId())
                    && id.equals(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath())) {
                ((ItemEntityAccessor) item).beckonShowcase$setAge(age);
                aged.add(item.getId());
            }
        }
    }

    /** Removes items whose client age ran out, as the server would. */
    private static void despawn(Minecraft mc) {
        List<ItemEntity> gone = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item && item.getAge() >= 6000) {
                gone.add(item);
            }
        }
        gone.forEach(Entity::discard);
    }

    private void plan(Minecraft mc) {
        BeckonConfig config = BeckonClient.config();

        until(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null);
        once(() -> {
            log("world loaded");
            Clock.fix();
            config.resetToDefaults();
            run(mc, "time set 6000");
            run(mc, "weather clear 1000000");
            run(mc, "gamerule advance_time false");
            run(mc, "gamerule spawn_mobs false");
            run(mc, "gamerule random_tick_speed 0");
            run(mc, "difficulty peaceful");
        });
        locate(mc);
        once(() -> {
            BlockPos c = site.get();
            cmd(mc, "forceload add %d %d %d %d", c.getX() - 40, c.getZ() - 40, c.getX() + 40, c.getZ() + 40);
            cmd(mc, "tp @p %d %d %d", c.getX(), c.getY() + 45, c.getZ());
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        });
        settle(mc, () -> {
        });
        once(() -> level(mc));
        once(() -> buildMeadow(mc));
        holdFor(60, () -> {
        });
        once(() -> {
            BlockPos c = site.get();
            cmd(mc, "setworldspawn %d %d %d", c.getX(), c.getY(), c.getZ() + 9);
            cmd(mc, "spawnpoint @p %d %d %d", c.getX(), c.getY(), c.getZ() + 9);
        });
        once(() -> film(mc, config));
    }

    private void film(Minecraft mc, BeckonConfig config) {
        if (wanted("intro")) {
            Vec3 start = at(0, 0, 12);
            Vec3 end = at(0, 0, 5.5);
            scene(mc, config, DUSK, start, () -> {
                drop(mc, "diamond_sword", 1, at(-3.2, 0.05, -0.5), GLINT);
                drop(mc, "emerald", 5, -1.4, -2.6);
                drop(mc, "experience_bottle", 1, 0.3, -1.2);
                drop(mc, "totem_of_undying", 1, 1.9, -3.1);
                drop(mc, "diamond", 7, 3.4, -0.9);
                drop(mc, "enchanted_golden_apple", 1, -0.6, -4.6);
                drop(mc, "heart_of_the_sea", 1, 4.6, -4.2);
            }, () -> face(mc, 180, 14));
            record("01-intro", 330, f -> {
                place(mc, lerp(start, end, smooth(f / 330.0) * 0.85 + f / 330.0 * 0.15));
                face(mc, 180 - 2.5 * Math.sin(f / 60.0), 14 + 2 * Math.sin(f / 50.0));
                if (f == 300) {
                    shot("intro");
                }
            });
        }

        if (wanted("colors")) {
            Vec3 feet = at(0, 0, 4);
            String[] items = {"diamond_sword", "emerald", "experience_bottle", "enchanted_golden_apple", "totem_of_undying",
                    "diamond", "nether_star", "music_disc_cat"};
            scene(mc, config, 13200, feet, () -> {
                for (int i = 0; i < items.length; i++) {
                    drop(mc, items[i], 1, at(-5.6 + i * 1.6, 0.05, -1.5),
                            i == 0 ? GLINT : null);
                }
                drop(mc, "stick", 1, at(0, 0.05, -4), "{\"minecraft:custom_name\":{text:\"Magic Wand\",color:\"light_purple\"}}");
            }, () -> face(mc, 165, 22));
            record("02-colors", 390, f -> {
                place(mc, feet);
                face(mc, lerp(162, 198, smooth(f / 390.0)), 22);
                if (f == 195) {
                    shot("colors");
                }
            });
        }

        if (wanted("night")) {
            Vec3 feet = at(0, 0, 5);
            scene(mc, config, NIGHT, feet, () -> {
                String[] items = {"diamond", "emerald", "experience_bottle", "diamond_pickaxe", "totem_of_undying",
                        "enchanted_book", "netherite_ingot", "diamond_chestplate", "emerald", "trident", "nether_star",
                        "diamond"};
                double[][] spots = {{-4, -2}, {-1.5, -5}, {2.5, -1.5}, {5, -6}, {-7, -8}, {8, -2}, {0, -10}, {-9, -1},
                        {6, -11}, {-4, -13}, {11, -8}, {-12, -6}};
                for (int i = 0; i < items.length; i++) {
                    drop(mc, items[i], 1, spots[i][0], spots[i][1]);
                }
                mc.options.gamma().set(1.0);
            }, () -> face(mc, 150, 10));
            record("03-night", 420, f -> {
                place(mc, feet);
                face(mc, lerp(148, 212, smooth(f / 420.0)), 10 + 1.5 * Math.sin(f / 70.0));
                if (f == 210) {
                    shot("night");
                }
            });
        }

        if (wanted("labels")) {
            Vec3 feet = at(0, 0, 3);
            Vec3[] targets = {at(-2.2, 0.3, -1.6), at(0.2, 0.3, -2.4), at(2.4, 0.3, -1.4)};
            scene(mc, config, DUSK, feet, () -> {
                drop(mc, "diamond", 12, -2.2, -1.6);
                drop(mc, "enchanted_golden_apple", 2, 0.2, -2.4);
                drop(mc, "diamond_sword", 1, at(2.4, 0.05, -1.4), GLINT);
            }, () -> lookAt(mc, targets[0]));
            record("04-labels", 420, f -> {
                place(mc, feet);
                lookAt(mc, sweep(f, targets, 40, 100));
                if (f == 200) {
                    shot("labels");
                }
            });
        }

        if (wanted("merge")) {
            Vec3 feet = at(0, 0, 2.8);
            Vec3 pile = at(0, 0.3, -1);
            scene(mc, config, DUSK, feet, () -> {
            }, () -> lookAt(mc, pile));
            double[][] offsets = {{0, 0}, {0.6, 0.3}, {-0.5, 0.4}, {0.3, -0.6}, {-0.4, -0.4}, {0.7, -0.2}};
            record("05-merge", 400, f -> {
                place(mc, feet);
                lookAt(mc, pile);
                int n = (f - 20) / 50;
                if (f >= 20 && (f - 20) % 50 == 0 && n < offsets.length) {
                    drop(mc, "diamond", 4, at(offsets[n][0], 1.4, -1 + offsets[n][1]), null);
                }
                if (f == 380) {
                    shot("merge");
                }
            });
        }

        if (wanted("death")) {
            Vec3 spot = at(0, 0, -3);
            Vec3 start = at(1.5, 0, 9);
            Vec3 end = at(0.5, 0, 3.5);
            Vec3 look = at(0, 0.6, -3);
            scene(mc, config, DUSK, spot, () -> {
                run(mc, "gamemode survival @p");
                kit(mc, "diamond_sword", "diamond 24", "iron_pickaxe", "emerald 9", "experience_bottle 4", "bread 16",
                        "torch 32", "oak_log 20");
            }, () -> face(mc, 180, 0));
            once(() -> run(mc, "kill @p"));
            until(() -> mc.player.isDeadOrDying());
            holdFor(120, () -> {
            });
            once(() -> mc.player.respawn());
            until(() -> mc.player != null && !mc.player.isDeadOrDying() && Screens.current(mc) == null);
            once(() -> {
                run(mc, "gamemode creative @p");
                run(mc, "attribute @p minecraft:block_interaction_range base set 0");
                cmd(mc, "tp @p %.3f %.3f %.3f", start.x, start.y, start.z);
            });
            settle(mc, () -> {
                place(mc, start);
                lookAt(mc, look);
            });
            record("06-death", 390, f -> {
                place(mc, lerp(start, end, smooth(f / 390.0)));
                lookAt(mc, look);
                if (f == 330) {
                    shot("death");
                }
            });
        }

        if (wanted("blink")) {
            Vec3 feet = at(0, 0, 2.6);
            Vec3 look = at(0, 0.3, -1.4);
            scene(mc, config, DUSK, feet, () -> {
                drop(mc, "emerald", 3, -1.6, -1.2);
                drop(mc, "diamond", 2, 0, -2.0);
                drop(mc, "totem_of_undying", 1, 1.6, -1.2);
            }, () -> lookAt(mc, look));
            record("07-blink", 450, f -> {
                place(mc, feet);
                lookAt(mc, look);
                if (f == 0) {
                    // Slow, middle and fast stages of the warning side by side; the totem runs out on camera.
                    age(mc, "emerald", 6000 - 400);
                    age(mc, "diamond", 6000 - 250);
                    age(mc, "totem_of_undying", 6000 - 130);
                }
                despawn(mc);
            });
        }

        if (wanted("settings")) {
            Vec3 feet = at(0, 0, 4);
            scene(mc, config, DUSK, feet, () -> {
                drop(mc, "diamond", 3, -2.5, -1.5);
                drop(mc, "emerald", 2, 2.2, -1.8);
                drop(mc, "cobblestone", 8, -0.9, -2.6);
                drop(mc, "dirt", 5, 0.8, -0.8);
                drop(mc, "oak_log", 4, 3.5, -3.6);
                drop(mc, "bone", 2, -3.8, -3.8);
            }, () -> face(mc, 180, 18));
            once(() -> Screens.open(mc, new BeckonSettingsScreen(null, mc.options)));
            holdFor(20, () -> {
                place(mc, feet);
                cursor(0, new Stop(0, () -> corner(mc)));
            });
            record("08-settings", 400, f -> {
                place(mc, feet);
                face(mc, 180, 18);
                if (f < 235) {
                    cursor(f, new Stop(0, () -> corner(mc)),
                            new Stop(45, () -> labelAt(mc, "Beams On", 0.5)),
                            new Stop(110, () -> labelAt(mc, "Beam Height", 0.5)),
                            new Stop(165, () -> labelAt(mc, "Beam Height", 0.5)),
                            new Stop(215, () -> corner(mc)));
                }
                if (f == 55) {
                    clickLabel(mc, "Beams On", 0.5);
                } else if (f == 120 || f == 175) {
                    clickLabel(mc, "Beam Height", 0.5);
                } else if (f == 230) {
                    shot("settings");
                } else if (f == 235) {
                    cursorShown = false;
                    Screens.open(mc, null);
                } else if (f == 330) {
                    shot("settings-after");
                }
            });
            once(() -> Screens.open(mc, null));
        }

        if (wanted("outro")) {
            Vec3 start = at(0, 0, 1.5);
            Vec3 end = at(0, 1.5, 9);
            Vec3 look = at(0, 0.8, -3);
            scene(mc, config, 13000, start, () -> {
                String[] items = {"diamond_sword", "emerald", "experience_bottle", "totem_of_undying", "diamond",
                        "enchanted_golden_apple", "nether_star", "heart_of_the_sea", "diamond_pickaxe", "trident"};
                for (int i = 0; i < items.length; i++) {
                    double a = Math.PI * (0.12 + 0.76 * i / (items.length - 1));
                    drop(mc, items[i], 1, at(-Math.cos(a) * 4.2, 0.05, -3 - Math.sin(a) * 2.6),
                            i == 0 ? GLINT : null);
                }
            }, () -> lookAt(mc, look));
            record("09-outro", 390, f -> {
                place(mc, lerp(start, end, smooth(f / 390.0)));
                lookAt(mc, look);
                if (f == 70) {
                    shot("outro");
                }
            });
        }
    }

    /** Looks at each target in turn: holds for {@code hold} frames, then glides to the next over {@code move}. */
    private static Vec3 sweep(int f, Vec3[] targets, int move, int hold) {
        int step = move + hold;
        int i = Math.min(f / step, targets.length - 1);
        int into = f - i * step;
        if (i >= targets.length - 1 || into < hold) {
            return targets[i];
        }
        return lerp(targets[i], targets[i + 1], smooth((into - hold) / (double) move));
    }

    private static double[] corner(Minecraft mc) {
        Screen screen = Screens.current(mc);
        return new double[] {screen.width - 36, screen.height - 30};
    }

    private static double[] labelAt(Minecraft mc, String label, double along) {
        AbstractWidget widget = find(Screens.current(mc), label);
        if (widget == null) {
            throw new IllegalStateException("no widget " + label);
        }
        return new double[] {widget.getX() + widget.getWidth() * along, widget.getY() + widget.getHeight() / 2.0};
    }

    private static void clickLabel(Minecraft mc, String label, double along) {
        double[] at = labelAt(mc, label, along);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static AbstractWidget find(GuiEventListener node, String label) {
        if (node instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(label + ":")) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void deleteWorld(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static void log(String message) {
        System.out.println("[beckon-showcase] " + message);
    }
}
