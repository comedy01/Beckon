package dev.beckon.client.gui;

import dev.beckon.client.BeckonClient;
import dev.beckon.client.Texts;
import dev.beckon.config.BeckonConfig;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

public final class BeckonSettingsScreen extends BeckonOptionsScreen {
    private static final int WIDTH = 150;
    private static final float[] HEIGHTS = {1.5f, 2.5f, 4f, 6f, 10f};
    private static final float[] WIDTHS = {0.5f, 0.75f, 1f, 1.5f, 2f};
    private static final float[] OPACITIES = {0.25f, 0.5f, 0.8f, 1f};
    private static final float[] LABEL_DISTANCES = {4f, 8f, 16f, 32f};

    public BeckonSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Texts.translatable("beckon.options.title"));
    }

    @Override
    protected void addOptions() {
        BeckonConfig config = BeckonClient.config();

        addRow(
                toggle("beckon.options.enabled", config::enabled, config::setEnabled),
                modeButton(config));
        addRow(
                cycle("beckon.options.height", HEIGHTS, config::beamHeight, config::setBeamHeight, BeckonSettingsScreen::blocks),
                cycle("beckon.options.width", WIDTHS, config::beamWidth, config::setBeamWidth, BeckonSettingsScreen::percent));
        addRow(
                cycle("beckon.options.opacity", OPACITIES, config::opacity, config::setOpacity, BeckonSettingsScreen::percent),
                toggle("beckon.options.pulse", config::pulse, config::setPulse));
        addRow(
                toggle("beckon.options.ground", config::groundGlow, config::setGroundGlow),
                toggle("beckon.options.labels", config::labels, config::setLabels));
        addRow(
                cycle("beckon.options.label_distance", LABEL_DISTANCES, config::labelDistance, config::setLabelDistance, BeckonSettingsScreen::blocks),
                toggle("beckon.options.blink", config::despawnBlink, config::setDespawnBlink));
        addRow(
                toggle("beckon.options.death", config::deathDrops, config::setDeathDrops),
                toggle("beckon.options.merge", config::merge, config::setMerge));
        addRow(resetButton(config), null);
    }

    @Override
    public void removed() {
        super.removed();
        BeckonClient.saveConfig();
    }

    private AbstractWidget resetButton(BeckonConfig config) {
        return tooltip(button(Texts.translatable("beckon.options.reset"), WIDTH, button -> {
            config.resetToDefaults();
            ScreenOpener.open(minecraft, new BeckonSettingsScreen(lastScreen, options));
        }), Texts.translatable("beckon.options.reset.tooltip"));
    }

    private AbstractWidget modeButton(BeckonConfig config) {
        Supplier<Component> label = () -> valueLabel("beckon.options.mode",
                Texts.translatable("beckon.options.mode." + config.mode().name().toLowerCase(Locale.ROOT)));
        return tooltip(button(label.get(), WIDTH, button -> {
            config.setMode(config.mode().next());
            button.setMessage(label.get());
        }), Texts.translatable("beckon.options.mode.tooltip"));
    }

    private AbstractWidget cycle(String key, float[] values, Supplier<Float> getter, Consumer<Float> setter, DoubleFunction<Component> format) {
        return tooltip(button(valueLabel(key, format.apply(getter.get())), WIDTH, button -> {
            float current = getter.get();
            float next = values[0];
            for (float value : values) {
                if (value > current + 1.0E-3f) {
                    next = value;
                    break;
                }
            }
            setter.accept(next);
            button.setMessage(valueLabel(key, format.apply(getter.get())));
        }), Texts.translatable(key + ".tooltip"));
    }

    private AbstractWidget toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return tooltip(button(toggleLabel(key, getter.getAsBoolean()), WIDTH, button -> {
            setter.accept(!getter.getAsBoolean());
            button.setMessage(toggleLabel(key, getter.getAsBoolean()));
        }), Texts.translatable(key + ".tooltip"));
    }

    private static Component blocks(double value) {
        String text = value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
        return Texts.translatable("beckon.options.blocks", text);
    }

    private static Component percent(double value) {
        return Texts.literal(Math.round(value * 100) + "%");
    }

    private static Component valueLabel(String key, Component value) {
        return Texts.translatable("options.generic_value", Texts.translatable(key), value);
    }

    private static Component toggleLabel(String key, boolean on) {
        return valueLabel(key, Texts.translatable(on ? "options.on" : "options.off"));
    }
}
