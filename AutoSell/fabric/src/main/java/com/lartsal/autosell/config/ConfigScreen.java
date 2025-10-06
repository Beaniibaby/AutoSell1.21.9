package com.lartsal.autosell.config;

import com.lartsal.autosell.utils.Utils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

import static com.lartsal.autosell.AutoSellCore.applyConfig;

public class ConfigScreen {
    // TODO: Rename translation keys
    public static Screen createConfigScreen(Screen parent) {
        Config config = ConfigSerializer.getConfig();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("modmenu.autosell.settings.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ==================== GENERAL ====================
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("modmenu.autosell.settings.general.title"));

        // isModEnabled
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("modmenu.autosell.settings.general.enabled.name"), config.isModEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("modmenu.autosell.settings.general.enabled.hint"))
                .setSaveConsumer(newValue -> {
                    config.isModEnabled = newValue;
                    applyConfig(config);
                })
                .build());

        // isVillagerHighlightEnabled
        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("modmenu.autosell.settings.general.highlight_last_villager.name"), config.isVillagerHighlightingEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("modmenu.autosell.settings.general.highlight_last_villager.hint.1"),
                        Component.literal(" "),
                        Component.translatable("modmenu.autosell.settings.general.highlight_last_villager.hint.2"))
                .setSaveConsumer(newValue -> {
                    config.isVillagerHighlightingEnabled = newValue;
                    applyConfig(config);
                })
                .build());

        // tradeProcessDelay
        general.addEntry(entryBuilder.startIntField(Component.translatable("modmenu.autosell.settings.general.trades_process_delay.name"), config.tradeProcessDelay)
                .setDefaultValue(1)
                .setMin(0)
                .setMax(210)
                .setTooltip(Component.translatable("modmenu.autosell.settings.general.trades_process_delay.hint.1"),
                        Component.literal(" "),
                        Component.translatable("modmenu.autosell.settings.general.trades_process_delay.hint.2"))
                .setSaveConsumer(newValue -> {
                    config.tradeProcessDelay = newValue;
                    applyConfig(config);
                })
                .build());

        // acceptablePriceMultiplier
        general.addEntry(entryBuilder.startDoubleField(Component.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.name"), config.acceptablePriceMultiplier)
                .setDefaultValue(1.5)
                .setMin(1.0)
                .setMax(210.0)
                .setTooltip(Component.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.hint.1"),
                        Component.literal(" "),
                        Component.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.hint.2"),
                        Component.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.hint.3"))
                .setSaveConsumer(newValue -> {
                    config.acceptablePriceMultiplier = newValue;
                    applyConfig(config);
                })
                .build());

        // trades
        general.addEntry(entryBuilder.startStrList(Component.translatable("modmenu.autosell.settings.general.processed_trades.name"), config.trades)
                .setDefaultValue(List.of(
                        "pumpkin => emerald",
                        "melon => emerald"
                ))
                .setTooltip(Component.translatable("modmenu.autosell.settings.general.processed_trades.hint.1"),
                        Component.literal(" "),
                        Component.translatable("modmenu.autosell.settings.general.processed_trades.hint.2"),
                        Component.translatable("modmenu.autosell.settings.general.processed_trades.hint.3"),
                        Component.translatable("modmenu.autosell.settings.general.processed_trades.hint.4"),
                        Component.literal(" "),
                        Component.translatable("modmenu.autosell.settings.general.processed_trades.hint.5"),
                        Component.translatable("modmenu.autosell.settings.general.processed_trades.hint.6"),
                        Component.translatable("modmenu.autosell.settings.general.processed_trades.hint.7"))
                .setAddButtonTooltip(Component.translatable("modmenu.autosell.settings.general.processed_trades.add_button.hint"))
                .setRemoveButtonTooltip(Component.translatable("modmenu.autosell.settings.general.processed_trades.remove_button.hint"))
                .setCreateNewInstance(entry -> new StringListListEntry.StringListCell("_ + _ => _", entry))
                .setCellErrorSupplier(entry -> {
                    if (!Utils.isValidTradeEntry(entry)) {
                        return Optional.of(Component.translatable("modmenu.autosell.settings.general.processed_trades.error.invalid_entry_pattern"));
                    }
                    return Optional.empty();
                })
                .setSaveConsumer(newList -> {
                    config.trades = newList.stream()
                            .filter(Utils::isValidTradeEntry)
                            .toList();
                    applyConfig(config);
                })
                .build());

        // ==================== EFFECTS ====================
        ConfigCategory effects = builder.getOrCreateCategory(Component.translatable("modmenu.autosell.settings.effects.title"));

        // highlightingParticles
        effects.addEntry(entryBuilder.startStringDropdownMenu(Component.translatable("modmenu.autosell.settings.effects.highlighting_particles.name"), config.highlightingParticlesName)
                .setDefaultValue("minecraft:flame")
                .setTooltip(Component.translatable("modmenu.autosell.settings.effects.highlighting_particles.hint"))
                .setErrorSupplier(particleName -> {
                    if (!Utils.isValidParticle(particleName)) {
                        return Optional.of(Component.translatable("modmenu.autosell.settings.effects.highlighting_particles.error.invalid_particle_name"));
                    }
                    return Optional.empty();
                })
                .setSaveConsumer(newValue -> {
                    config.highlightingParticlesName = newValue;
                    applyConfig(config);
                })
                .setSelections(Utils.getSimpleParticleNames())
                .build());

        // particlesPerTick
        effects.addEntry(entryBuilder.startDoubleField(Component.translatable("modmenu.autosell.settings.effects.particles_per_tick.name"), config.particlesPerTick)
                .setDefaultValue(0.5)
                .setMin(0.01)
                .setMax(210)
                .setTooltip(Component.translatable("modmenu.autosell.settings.effects.particles_per_tick.hint.1"),
                        Component.literal(" "),
                        Component.translatable("modmenu.autosell.settings.effects.particles_per_tick.hint.2"),
                        Component.translatable("modmenu.autosell.settings.effects.particles_per_tick.hint.3"))
                .setSaveConsumer(newValue -> {
                    config.particlesPerTick = newValue;
                    applyConfig(config);
                })
                .build());

        // Particle Parameters
        double yLevelMin = -21.0;
        double yLevelMax = 21.0;
        double yLevelStep = 0.25;
        int yLevelDefault = (int) ((1.25 + Math.abs(yLevelMin)) / yLevelStep);
        int yLevelSliderSteps = (int) ((yLevelMax - yLevelMin) / yLevelStep);

        double radiusMin = 0.0;
        double radiusMax = 21.0;
        double radiusStep = 0.25;
        int radiusDefault = (int) ((0 + Math.abs(radiusMin)) / radiusStep);
        int radiusSliderSteps = (int) ((radiusMax - radiusMin) / radiusStep);

        double speedMin = 0.0;
        double speedMax = 2.1;
        double speedStep = 0.035;
        int speedDefault = (int) ((0.07 + Math.abs(speedMin)) / speedStep);
        int speedSliderSteps = (int) ((speedMax - speedMin) / speedStep);

        effects.addEntry(entryBuilder.startSubCategory(Component.translatable("modmenu.autosell.settings.effects.particles_parameters.title"), List.of(
                        // yLevel
                        entryBuilder.startIntSlider(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.y_level.name"), (int) ((config.yLevel + Math.abs(yLevelMin)) / yLevelStep), 0, yLevelSliderSteps)
                                .setDefaultValue(yLevelDefault)
                                .setMin(0)
                                .setMax(yLevelSliderSteps)
                                .setTooltip(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.y_level.hint"))
                                .setTextGetter(value -> Component.literal(String.format("%.2f", yLevelMin + value * yLevelStep)))
                                .setSaveConsumer(newValue -> {
                                    config.yLevel = yLevelMin + newValue * yLevelStep;
                                    applyConfig(config);
                                })
                                .build(),

                        // particlesShape
                        entryBuilder.startEnumSelector(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.particles_shape.name"), Config.ParticleDistributionType.class, config.particlesShape)
                                .setDefaultValue(Config.ParticleDistributionType.ELLIPSOID)
                                .setTooltip(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.particles_shape.hint"))
                                .setEnumNameProvider(value -> Component.translatable("modmenu.autosell.settings.effects.particle_parameters.particles_shape.type." + value.name().toLowerCase() + ".name"))
                                .setSaveConsumer(newValue -> {
                                    config.particlesShape = newValue;
                                    applyConfig(config);
                                })
                                .build(),

                        // radiusX
                        entryBuilder.startIntSlider(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.radius.x.name"), (int) ((config.radiusX + Math.abs(radiusMin)) / radiusStep), 0, radiusSliderSteps)
                                .setDefaultValue(radiusDefault)
                                .setMin(0)
                                .setMax(radiusSliderSteps)
                                .setTextGetter(value -> Component.literal(String.format("%.2f", radiusMin + value * radiusStep)))
                                .setSaveConsumer(newValue -> {
                                    config.radiusX = radiusMin + newValue * radiusStep;
                                    applyConfig(config);
                                })
                                .build(),

                        // radiusY
                        entryBuilder.startIntSlider(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.radius.y.name"), (int) ((config.radiusY + Math.abs(radiusMin)) / radiusStep), 0, radiusSliderSteps)
                                .setDefaultValue(radiusDefault)
                                .setMin(0)
                                .setMax(radiusSliderSteps)
                                .setTextGetter(value -> Component.literal(String.format("%.2f", radiusMin + value * radiusStep)))
                                .setSaveConsumer(newValue -> {
                                    config.radiusY = radiusMin + newValue * radiusStep;
                                    applyConfig(config);
                                })
                                .build(),

                        // radiusZ
                        entryBuilder.startIntSlider(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.radius.z.name"), (int) ((config.radiusZ + Math.abs(radiusMin)) / radiusStep), 0, radiusSliderSteps)
                                .setDefaultValue(radiusDefault)
                                .setMin(0)
                                .setMax(radiusSliderSteps)
                                .setTextGetter(value -> Component.literal(String.format("%.2f", radiusMin + value * radiusStep)))
                                .setSaveConsumer(newValue -> {
                                    config.radiusZ = radiusMin + newValue * radiusStep;
                                    applyConfig(config);
                                })
                                .build(),

                        // randomSpeed
                        entryBuilder.startBooleanToggle(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.random_speed.name"), config.randomSpeed)
                                .setDefaultValue(true)
                                .setTooltip(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.random_speed.hint.1"),
                                        Component.translatable("modmenu.autosell.settings.effects.particle_parameters.random_speed.hint.2"))
                                .setSaveConsumer(newValue -> {
                                    config.randomSpeed = newValue;
                                    applyConfig(config);
                                })
                                .build(),

                        // speedX
                        entryBuilder.startIntSlider(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.speed.x.name"), (int) ((config.speedX + Math.abs(speedMin)) / speedStep), 0, speedSliderSteps)
                                .setDefaultValue(speedDefault)
                                .setMin(0)
                                .setMax(speedSliderSteps)
                                .setTextGetter(value -> Component.literal(String.format("%.3f", speedMin + value * speedStep)))
                                .setSaveConsumer(newValue -> {
                                    config.speedX = speedMin + newValue * speedStep;
                                    applyConfig(config);
                                })
                                .build(),

                        // speedY
                        entryBuilder.startIntSlider(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.speed.y.name"), (int) ((config.speedY + Math.abs(speedMin)) / speedStep), 0, speedSliderSteps)
                                .setDefaultValue(speedDefault)
                                .setMin(0)
                                .setMax(speedSliderSteps)
                                .setTextGetter(value -> Component.literal(String.format("%.3f", speedMin + value * speedStep)))
                                .setSaveConsumer(newValue -> {
                                    config.speedY = speedMin + newValue * speedStep;
                                    applyConfig(config);
                                })
                                .build(),

                        // speedZ
                        entryBuilder.startIntSlider(Component.translatable("modmenu.autosell.settings.effects.particle_parameters.speed.z.name"), (int) ((config.speedZ + Math.abs(speedMin)) / speedStep), 0, speedSliderSteps)
                                .setDefaultValue(speedDefault)
                                .setMin(0)
                                .setMax(speedSliderSteps)
                                .setTextGetter(value -> Component.literal(String.format("%.3f", speedMin + value * speedStep)))
                                .setSaveConsumer(newValue -> {
                                    config.speedZ = speedMin + newValue * speedStep;
                                    applyConfig(config);
                                })
                                .build()
                ))
                .setExpanded(false)
                .build());

        builder.setSavingRunnable(ConfigSerializer::saveConfig);
        return builder.build();
    }
}
