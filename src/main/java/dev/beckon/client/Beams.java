package dev.beckon.client;

import dev.beckon.config.BeckonConfig;
import dev.beckon.core.Palette;
import dev.beckon.core.Rules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Beams {
    public static final int DEATH_COLOR = 0xFFD860;
    private static final double RANGE_SQ = 160 * 160;
    private static final int DEATH_WINDOW = 200;
    private static final double DEATH_RADIUS_SQ = 8 * 8;
    private static final double NEAR_SQ = 1.5 * 1.5;
    private static final double AIM_TOLERANCE = 1 - Math.cos(Math.toRadians(9));

    private static Map<Integer, BeamInfo> current = Collections.emptyMap();
    private static final Set<UUID> deathDrops = new HashSet<>();
    private static final Map<Item, Integer> spriteColors = new HashMap<>();
    private static Vec3 deathPos;
    private static long deathTick = Long.MIN_VALUE;
    private static boolean wasDead;
    private static long ticks;
    private static Rules always = new Rules(List.of());
    private static Rules never = new Rules(List.of());
    private static ClientLevel lastLevel;

    private Beams() {
    }

    public static BeamInfo get(int entityId) {
        return current.get(entityId);
    }

    public static boolean isDeathDrop(UUID uuid) {
        return deathDrops.contains(uuid);
    }

    public static void reloadRules(BeckonConfig config) {
        always = new Rules(config.alwaysShow());
        never = new Rules(config.neverShow());
    }

    public static void rememberSpriteColor(Item item, int rgb) {
        spriteColors.put(item, rgb);
    }

    public static boolean needsSpriteColor(Item item) {
        return !spriteColors.containsKey(item);
    }

    public static void tick(Minecraft mc) {
        ticks++;
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level != lastLevel) {
            lastLevel = level;
            spriteColors.clear();
            if (level == null) {
                deathDrops.clear();
                deathPos = null;
            }
        }
        BeckonConfig config = BeckonClient.config();
        if (level == null || player == null || !config.enabled()) {
            current = Collections.emptyMap();
            return;
        }
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead) {
            deathPos = player.position();
            deathTick = ticks;
        }
        wasDead = dead;

        List<Candidate> candidates = new ArrayList<>();
        Vec3 eye = player.position();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
                continue;
            }
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            double dist = item.distanceToSqr(eye);
            if (dist > RANGE_SQ) {
                continue;
            }
            boolean death = config.deathDrops() && checkDeathDrop(item);
            if (!death && !shown(config, stack)) {
                continue;
            }
            candidates.add(new Candidate(item, stack, death ? DEATH_COLOR : color(stack), death, dist));
        }
        candidates.sort((a, b) -> Double.compare(a.dist, b.dist));

        List<Candidate> leaders = new ArrayList<>();
        Map<String, Candidate> byCell = new HashMap<>();
        for (Candidate c : candidates) {
            c.total = c.stack.getCount();
            if (config.merge()) {
                String key = Stacks.id(c.stack) + '|' + c.death + '|' + cell(c.item);
                Candidate leader = byCell.get(key);
                if (leader != null) {
                    leader.total += c.total;
                    continue;
                }
                byCell.put(key, c);
            }
            leaders.add(c);
        }
        int count = Math.min(config.maxBeams(), leaders.size());
        int labelled = config.labels() ? labelTarget(player, leaders, count, config.labelDistance()) : -1;
        Map<Integer, BeamInfo> next = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Candidate c = leaders.get(i);
            int id = c.item.getId();
            next.put(id, new BeamInfo(c.color, c.death, c.total, (id * 0.618034f) % 1f, id == labelled));
        }
        current = next;
    }

    private static int labelTarget(LocalPlayer player, List<Candidate> leaders, int count, float distance) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);
        double maxSq = distance * distance;
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            Candidate c = leaders.get(i);
            Vec3 to = new Vec3(c.item.getX() - eye.x, c.item.getY() + 0.25 - eye.y, c.item.getZ() - eye.z);
            double lenSq = to.lengthSqr();
            if (lenSq > maxSq) {
                continue;
            }
            double len = Math.sqrt(lenSq);
            double off = len < 1.0E-3 ? 0 : 1 - to.dot(look) / len;
            double score = lenSq < NEAR_SQ ? len * 0.001 : off;
            if (off < AIM_TOLERANCE || lenSq < NEAR_SQ) {
                if (score < bestScore) {
                    bestScore = score;
                    best = c.item.getId();
                }
            }
        }
        return best;
    }

    private static String cell(ItemEntity item) {
        return (int) Math.floor(item.getX() / 1.5) + "," + (int) Math.floor(item.getY() / 2.0) + "," + (int) Math.floor(item.getZ() / 1.5);
    }

    private static boolean checkDeathDrop(ItemEntity item) {
        UUID uuid = item.getUUID();
        if (deathDrops.contains(uuid)) {
            return true;
        }
        if (deathPos == null || ticks - deathTick > DEATH_WINDOW || item.getAge() > ticks - deathTick + 40) {
            return false;
        }
        if (item.distanceToSqr(deathPos) > DEATH_RADIUS_SQ) {
            return false;
        }
        if (deathDrops.size() > 4096) {
            deathDrops.clear();
        }
        deathDrops.add(uuid);
        return true;
    }

    private static boolean shown(BeckonConfig config, ItemStack stack) {
        String id = Stacks.id(stack);
        Iterable<String> tags = () -> Stacks.tags(stack).iterator();
        if (!never.isEmpty() && never.matches(id, tags)) {
            return false;
        }
        return switch (config.mode()) {
            case ALL -> true;
            case LISTED -> always.matches(id, tags);
            case VALUABLE -> Stacks.isValuable(stack) || always.matches(id, tags);
        };
    }

    private static int color(ItemStack stack) {
        int name = Stacks.nameColor(stack);
        if (name >= 0 && !Palette.isWhite(name)) {
            return name;
        }
        Integer sprite = spriteColors.get(stack.getItem());
        return sprite != null ? sprite : Palette.WHITE;
    }

    private static final class Candidate {
        final ItemEntity item;
        final ItemStack stack;
        final int color;
        final boolean death;
        final double dist;
        int total;

        Candidate(ItemEntity item, ItemStack stack, int color, boolean death, double dist) {
            this.item = item;
            this.stack = stack;
            this.color = color;
            this.death = death;
            this.dist = dist;
        }
    }
}
