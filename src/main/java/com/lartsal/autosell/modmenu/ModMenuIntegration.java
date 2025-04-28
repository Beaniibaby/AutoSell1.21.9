package com.lartsal.autosell.modmenu;

import com.lartsal.autosell.config.ConfigManager;
import com.lartsal.autosell.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Optional;

import static com.lartsal.autosell.AutoSellMod.applyConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ConfigManager.getConfig();
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("modmenu.autosell.settings.title"));

            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("modmenu.autosell.settings.general.title"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // isModEnabled
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("modmenu.autosell.settings.general.enabled.name"), config.isModEnabled)
                    .setDefaultValue(config.isModEnabled)
                    .setTooltip(Text.translatable("modmenu.autosell.settings.general.enabled.hint"))
                    .setSaveConsumer(newValue -> {
                        config.isModEnabled = newValue;
                        applyConfig(config);
                    })
                    .build());

            // isVillagerHighlightEnabled
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("modmenu.autosell.settings.general.highlight_last_villager.name"), config.isVillagerHighlightingEnabled)
                    .setDefaultValue(config.isVillagerHighlightingEnabled)
                    .setTooltip(Text.translatable("modmenu.autosell.settings.general.highlight_last_villager.hint.1"),
                                Text.literal(" "),
                                Text.translatable("modmenu.autosell.settings.general.highlight_last_villager.hint.2"))
                    .setSaveConsumer(newValue -> {
                        config.isVillagerHighlightingEnabled = newValue;
                        applyConfig(config);
                    })
                    .build());

            // operationsDelay
            general.addEntry(entryBuilder.startIntField(Text.translatable("modmenu.autosell.settings.general.trades_delay.name"), config.tradesDelay)
                    .setDefaultValue(config.tradesDelay)
                    .setMin(0)
                    .setMax(210)
                    .setTooltip(Text.translatable("modmenu.autosell.settings.general.trades_delay.hint.1"),
                                Text.literal(" "),
                                Text.translatable("modmenu.autosell.settings.general.trades_delay.hint.2"))
                    .setSaveConsumer(newValue -> {
                        config.tradesDelay = newValue;
                        applyConfig(config);
                    })
                    .build());

            // acceptablePriceMultiplier
            general.addEntry(entryBuilder.startDoubleField(Text.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.name"), config.acceptablePriceMultiplier)
                    .setDefaultValue(config.acceptablePriceMultiplier)
                    .setMin(1.0)
                    .setMax(210.0)
                    .setTooltip(Text.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.hint.1"),
                                Text.literal(" "),
                                Text.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.hint.2"),
                                Text.translatable("modmenu.autosell.settings.general.acceptable_price_multiplier.hint.3"))
                    .setSaveConsumer(newValue -> {
                        config.acceptablePriceMultiplier = newValue;
                        applyConfig(config);
                    })
                    .build());

            // trades
            general.addEntry(entryBuilder.startStrList(Text.translatable("modmenu.autosell.settings.general.processed_trades.name"), config.trades)
                    .setDefaultValue(new ArrayList<>(config.trades))
                    .setTooltip(Text.translatable("modmenu.autosell.settings.general.processed_trades.hint.1"),
                                Text.literal(" "),
                                Text.translatable("modmenu.autosell.settings.general.processed_trades.hint.2"),
                                Text.translatable("modmenu.autosell.settings.general.processed_trades.hint.3"),
                                Text.translatable("modmenu.autosell.settings.general.processed_trades.hint.4"),
                                Text.literal(" "),
                                Text.translatable("modmenu.autosell.settings.general.processed_trades.hint.5"),
                                Text.translatable("modmenu.autosell.settings.general.processed_trades.hint.6"),
                                Text.translatable("modmenu.autosell.settings.general.processed_trades.hint.7"))
                    .setCreateNewInstance(entry -> new StringListListEntry.StringListCell("_ + _ => _", entry))
                    .setCellErrorSupplier(entry -> {
                        if (!config.isValidTradeEntry(entry)) {
                            return Optional.of(Text.translatable("modmenu.autosell.settings.error.invalid_entry_pattern"));
                        }
                        return Optional.empty();
                    })
                    .setSaveConsumer(newList -> {
                        config.trades = newList.stream()
                                .filter(config::isValidTradeEntry)
                                .toList();
                        applyConfig(config);
                    })
                    .build());

            builder.setSavingRunnable(ConfigManager::saveConfig);
            return builder.build();
        };
    }
}