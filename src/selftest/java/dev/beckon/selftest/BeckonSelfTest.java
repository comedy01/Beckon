package dev.beckon.selftest;

import dev.beckon.client.BeamInfo;
import dev.beckon.client.Beams;
import dev.beckon.client.BeckonClient;
import dev.beckon.client.gui.BeckonSettingsScreen;
import dev.beckon.config.BeckonConfig;
import dev.beckon.core.Blink;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.tutorial.TutorialSteps;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class BeckonSelfTest {
    private static final String WORLD = "beckon-selftest";
    private static final int TIMEOUT = 2400;

    private static volatile int frames;
    public static volatile boolean hideHud;

    private final List<Step> steps = new ArrayList<>();
    private final List<String> shots = new ArrayList<>();
    private final Set<String> existingShots = new HashSet<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int seenFrames;
    private int patience;

    private record Step(int delay, BooleanSupplier ready, Runnable action) {
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 40) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                ServerCompat.hideChat(mc);
                rememberShots(mc);
                plan(mc);
                delay = steps.get(0).delay();
                log("creating world");
                deleteWorld(mc, WORLD);
                Worlds.create(mc, WORLD, true, 0L);
            }
            return;
        }
        int drawn = frames;
        if (drawn == seenFrames) {
            return;
        }
        seenFrames = drawn;
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready().getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action().run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay();
        }
    }

    public static void frame() {
        frames++;
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private void when(BooleanSupplier ready, Runnable action) {
        steps.add(new Step(0, ready, action));
    }

    private void within(int ticks, BooleanSupplier ready, Runnable action) {
        then(1, () -> patience = ticks);
        when(() -> ready.getAsBoolean() || --patience < 0, action);
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        nameShots(mc);
        if (failure == null) {
            log("ALL CHECKS PASSED");
        } else {
            log("FAILED: " + failure);
            failure.printStackTrace();
        }
        BeckonConfig config = BeckonClient.config();
        config.resetToDefaults();
        BeckonClient.saveConfig();
        mc.stop();
    }

    private void plan(Minecraft mc) {
        BeckonConfig config = BeckonClient.config();
        int floor = ServerCompat.FLOOR;
        int z = -5;

        when(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null, () -> log("world loaded"));
        then(40, () -> {
            config.resetToDefaults();
            run(mc, "gamerule doDaylightCycle false");
            run(mc, "gamerule advance_time false");
            run(mc, "time set 6000");
            run(mc, "weather clear");
            run(mc, "tp @p 0.5 " + floor + " 2.5 180 12");
        });
        if (Boolean.getBoolean("beckon.icon")) {
            iconScene(mc);
            return;
        }
        then(20, () -> {
            drop(mc, "minecraft:dirt", -3.5, floor, z);
            drop(mc, "minecraft:diamond_sword", -2, floor, z);
            drop(mc, "minecraft:enchanted_golden_apple", -0.5, floor, z);
            drop(mc, "minecraft:totem_of_undying", 1, floor, z);
            drop(mc, "minecraft:diamond", 3.1, floor, z);
            drop(mc, "minecraft:diamond", 4.3, floor, z);
            drop(mc, "minecraft:cobblestone", -3.5, floor, z - 3);
        });
        within(200, () -> items(mc, "minecraft:diamond").size() == 2 && beam(mc, "minecraft:totem_of_undying") != null
                && beam(mc, "minecraft:diamond") != null && beam(mc, "minecraft:diamond").total == 2, () -> {
            check(items(mc, "minecraft:dirt").size() == 1, "the dirt did not spawn");
            check(beam(mc, "minecraft:dirt") == null, "plain dirt got a beam in Valuable mode");
            check(beam(mc, "minecraft:cobblestone") == null, "cobblestone got a beam in Valuable mode");
            BeamInfo sword = beam(mc, "minecraft:diamond_sword");
            check(sword != null, "the diamond sword has no beam");
            BeamInfo apple = beam(mc, "minecraft:enchanted_golden_apple");
            check(apple != null, "the enchanted golden apple has no beam");
            check(apple.color == 0xFF55FF || apple.color == 0x55FFFF, "the enchanted golden apple beam is not its rarity color: " + Integer.toHexString(apple.color));
            BeamInfo totem = beam(mc, "minecraft:totem_of_undying");
            check(totem != null && totem.color == 0xFFFF55, "the uncommon totem beam is not yellow");
            int beams = 0;
            int total = 0;
            for (ItemEntity diamond : items(mc, "minecraft:diamond")) {
                BeamInfo info = Beams.get(diamond.getId());
                if (info != null) {
                    beams++;
                    total = info.total;
                }
            }
            check(beams == 1, "two diamonds lying close together got " + beams + " beams instead of one");
            check(total == 2, "the merged diamond beam counts " + total + " instead of 2");
        });
        within(60, () -> {
            BeamInfo sword = beam(mc, "minecraft:diamond_sword");
            return sword != null && sword.color != 0xFFFFFF;
        }, () -> {
            BeamInfo sword = beam(mc, "minecraft:diamond_sword");
            int r = sword.color >> 16 & 0xFF;
            int b = sword.color & 0xFF;
            log("diamond sword beam color " + Integer.toHexString(sword.color));
            check(b > r, "the diamond sword beam is not diamond-blue: " + Integer.toHexString(sword.color));
        });
        // A custom name's colour wins over the item's own colour (1.21.2+ wraps the name in the rarity's colour).
        then(1, () -> drop(mc, "minecraft:stick", 2, floor, z - 3));
        then(20, () -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                for (Entity entity : server.overworld().getAllEntities()) {
                    if (entity instanceof ItemEntity item && item.getItem().getItem() == Items.STICK) {
                        ItemStack named = item.getItem().copy();
                        Summon.name(named, Component.literal("Magic Wand").withStyle(ChatFormatting.LIGHT_PURPLE));
                        item.setItem(named);
                    }
                }
            });
        });
        within(100, () -> beam(mc, "minecraft:stick") != null, () -> {
            BeamInfo stick = beam(mc, "minecraft:stick");
            check(stick != null, "the stick with a custom name has no beam");
            check(stick.color == 0xFF55FF, "the custom-named stick's beam is not its name colour: " + Integer.toHexString(stick.color));
        });
        then(20, () -> screenshot(mc, "day"));
        then(1, () -> run(mc, "time set 18000"));
        then(40, () -> screenshot(mc, "night"));
        then(1, () -> run(mc, "tp @p 0.5 " + floor + " " + (z + 2.2) + " 180 35"));
        then(30, () -> screenshot(mc, "labels"));
        then(1, () -> run(mc, "tp @p 6.5 " + (floor + 3) + " " + (z + 6) + " 145 25"));
        then(30, () -> screenshot(mc, "side"));

        then(5, () -> {
            int hidden = 0;
            int shown = 0;
            for (int i = 0; i < 40; i++) {
                if (Blink.visible(100 - i / 2, (i % 2) * 0.5f, config.despawnWarnTicks())) {
                    shown++;
                } else {
                    hidden++;
                }
            }
            check(hidden > 0 && shown > 0, "an item about to despawn does not blink");
            check(Blink.visible(5000, 0, config.despawnWarnTicks()), "a fresh item blinks");
        });

        then(5, () -> config.setEnabled(false));
        then(5, () -> {
            check(beam(mc, "minecraft:totem_of_undying") == null, "beams stayed after Beckon was turned off");
            config.setEnabled(true);
            config.setMode(BeckonConfig.Mode.ALL);
        });
        then(5, () -> check(beam(mc, "minecraft:dirt") != null, "All Items mode did not give the dirt a beam"));
        then(1, () -> config.setMode(BeckonConfig.Mode.VALUABLE));

        then(5, () -> {
            run(mc, "kill @e[type=minecraft:item]");
            run(mc, "gamemode survival @p");
            run(mc, "tp @p 40.5 " + floor + " 40.5 180 30");
            run(mc, "item replace entity @p hotbar.0 with minecraft:dirt 32");
            run(mc, "item replace entity @p hotbar.1 with minecraft:iron_pickaxe");
            run(mc, "item replace entity @p hotbar.2 with minecraft:bread 12");
        });
        within(400, () -> mc.player.getX() > 39 && mc.player.getInventory().getItem(2).getCount() == 12, () -> {
        });
        then(60, () -> run(mc, "kill @p"));
        within(800, () -> deathBeams(mc) >= 3, () -> {
            check(deathBeams(mc) >= 3, "only " + deathBeams(mc) + " of the death drops got death beams: " + itemCount(mc) + " items, dead " + mc.player.isDeadOrDying() + ", server items " + serverItems(mc) + ", player at " + mc.player.position());
            BeamInfo dirt = beam(mc, "minecraft:dirt");
            check(dirt != null && dirt.death, "the dirt dropped on death has no golden death beam");
        });
        then(5, () -> mc.player.respawn());
        within(100, () -> mc.player != null && !mc.player.isDeadOrDying() && Screens.current(mc) == null,
                () -> {
                    run(mc, "gamemode creative @p");
                    run(mc, "tp @p 40.5 " + floor + " 49.5 180 20");
                });
        then(40, () -> {
            check(deathBeams(mc) >= 3, "the death beams disappeared after respawning: " + deathBeams(mc) + " death beams, " + itemCount(mc) + " items, player at " + mc.player.blockPosition());
            screenshot(mc, "death");
        });

        then(20, () -> Screens.open(mc, new BeckonSettingsScreen(null, mc.options)));
        then(20, () -> {
            check(Screens.current(mc) instanceof BeckonSettingsScreen, "the settings screen did not open");
            screenshot(mc, "settings");
        });
        then(5, () -> Screens.open(mc, null));
    }

    private void iconScene(Minecraft mc) {
        BeckonConfig config = BeckonClient.config();
        int floor = ServerCompat.FLOOR;
        String[] flowers = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:oxeye_daisy",
                "minecraft:azure_bluet", "minecraft:allium", "minecraft:short_grass", "minecraft:short_grass",
                "minecraft:short_grass", "minecraft:short_grass", "minecraft:tall_grass"};
        then(5, () -> {
            config.setMode(BeckonConfig.Mode.ALL);
            config.setLabels(false);
            config.setBeamHeight(4f);
            hideHud = true;
            mc.options.fov().set(55);
            java.util.Random random = new java.util.Random(3);
            for (int x = -14; x <= 14; x++) {
                for (int z = -22; z <= 0; z++) {
                    if (random.nextFloat() < 0.32f && !(x >= -2 && x <= 2 && z >= -6)) {
                        run(mc, "setblock " + x + " " + floor + " " + z + " " + flowers[random.nextInt(flowers.length)]);
                    }
                }
            }
            run(mc, "place feature minecraft:fancy_oak -7 " + floor + " -15");
            run(mc, "place feature minecraft:oak 6 " + floor + " -13");
            run(mc, "place feature minecraft:birch 11 " + floor + " -19");
            run(mc, "place feature minecraft:oak -12 " + floor + " -20");
            run(mc, "place feature minecraft:birch 1 " + floor + " -22");
            run(mc, "fill 0 " + (floor - 1) + " 0 0 " + floor + " 0 minecraft:air");
        });
        then(20, () -> {
            drop(mc, "minecraft:diamond_sword", -1.3, floor, -2.9);
            drop(mc, "minecraft:gold_ingot", 0.1, floor, -3.6);
            drop(mc, "minecraft:amethyst_shard", 1.5, floor, -3.0);
            drop(mc, "minecraft:emerald", 0.9, floor, -5.2);
            run(mc, "tp @p 0.5 " + (floor - 1) + " 0.5 180 -4");
        });
        then(5, () -> run(mc, "time set 12500"));
        for (int i = 0; i < 10; i++) {
            String name = "icon-group-" + i;
            then(i == 0 ? 60 : 7, () -> screenshot(mc, name));
        }
        then(1, () -> {
            run(mc, "fill -2 " + (floor - 1) + " -1 -1 " + floor + " -1 minecraft:air");
            run(mc, "tp @p -1.3 " + (floor - 1) + " -0.7 180 -12");
        });
        for (int i = 0; i < 12; i++) {
            String name = "icon-sword-" + i;
            then(i == 0 ? 40 : 6, () -> screenshot(mc, name));
        }
        then(1, () -> run(mc, "tp @p 0.2 " + (floor - 1) + " -1.4 180 -12"));
        for (int i = 0; i < 12; i++) {
            String name = "icon-gold-" + i;
            then(i == 0 ? 40 : 6, () -> screenshot(mc, name));
        }
        then(1, () -> {
            hideHud = false;
            mc.options.fov().set(70);
        });
    }

    private static void drop(Minecraft mc, String item, double x, int y, double z) {
        run(mc, "summon minecraft:item " + x + " " + y + " " + z + " " + Summon.item(item));
    }

    private static List<ItemEntity> items(Minecraft mc, String id) {
        List<ItemEntity> found = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item && id.equals(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString())) {
                found.add(item);
            }
        }
        return found;
    }

    private static BeamInfo beam(Minecraft mc, String id) {
        for (ItemEntity item : items(mc, id)) {
            BeamInfo info = Beams.get(item.getId());
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    private static String serverItems(Minecraft mc) {
        StringBuilder out = new StringBuilder();
        for (Entity entity : mc.getSingleplayerServer().overworld().getAllEntities()) {
            if (entity instanceof ItemEntity item) {
                out.append(BuiltInRegistries.ITEM.getKey(item.getItem().getItem())).append('@').append(item.blockPosition().toShortString()).append(' ');
            }
        }
        return out.toString();
    }

    private static int itemCount(Minecraft mc) {
        int count = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity) {
                count++;
            }
        }
        return count;
    }

    private static int deathBeams(Minecraft mc) {
        int count = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item) {
                BeamInfo info = Beams.get(item.getId());
                if (info != null && info.death) {
                    count++;
                }
            }
        }
        return count;
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> ServerCompat.runCommand(server, command));
    }

    private void screenshot(Minecraft mc, String name) {
        log("screenshot: " + name);
        shots.add(name);
        ServerCompat.screenshot(mc, Screens.renderTarget(mc));
    }

    private void rememberShots(Minecraft mc) {
        File[] files = new File(mc.gameDirectory, "screenshots").listFiles();
        if (files != null) {
            for (File file : files) {
                existingShots.add(file.getName());
            }
        }
    }

    private void nameShots(Minecraft mc) {
        File dir = new File(mc.gameDirectory, "screenshots");
        File[] files = dir.listFiles((parent, name) -> name.endsWith(".png") && !existingShots.contains(name));
        if (files == null) {
            return;
        }
        List<File> fresh = new ArrayList<>(List.of(files));
        fresh.sort(Comparator.comparingLong(File::lastModified).thenComparing(File::getName));
        for (int i = 0; i < fresh.size() && i < shots.size(); i++) {
            File target = new File(dir, String.format(Locale.ROOT, "bk-%02d-%s.png", i, shots.get(i)));
            if ((!target.exists() || target.delete()) && fresh.get(i).renameTo(target)) {
                log("saved " + target.getName());
            }
        }
    }

    private static void deleteWorld(Minecraft mc, String name) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(name);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void log(String message) {
        System.out.println("[beckon-selftest] " + message);
    }
}
