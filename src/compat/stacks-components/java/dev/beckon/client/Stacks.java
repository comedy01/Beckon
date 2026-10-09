package dev.beckon.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.stream.Stream;

public final class Stacks {
    private Stacks() {
    }

    public static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static Stream<String> tags(ItemStack stack) {
        return stack.getTags().map(tag -> tag.location().toString());
    }

    public static boolean isValuable(ItemStack stack) {
        return stack.getRarity() != Rarity.COMMON
                || stack.isEnchanted()
                || stack.has(DataComponents.CUSTOM_NAME)
                || stack.isDamageableItem();
    }

    public static int nameColor(ItemStack stack) {
        TextColor color = stack.getHoverName().getStyle().getColor();
        if (color == null) {
            color = TextColor.fromLegacyFormat(stack.getRarity().color());
        }
        return color == null ? -1 : color.getValue();
    }

    public static Component label(ItemStack stack, int total) {
        MutableComponent name = stack.getHoverName().copy().withStyle(stack.getRarity().color());
        if (total > 1) {
            name = name.append(Texts.literal(" \u00d7" + total));
        }
        return name;
    }
}
