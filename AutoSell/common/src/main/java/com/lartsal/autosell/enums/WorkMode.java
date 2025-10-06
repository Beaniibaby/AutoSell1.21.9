package com.lartsal.autosell.enums;

import net.minecraft.network.chat.Component;

public enum WorkMode {
    OFF("autosell.work_mode.off"),
    SELL_ONLY_REAL_PRICES("autosell.work_mode.only_real_prices"),
    SELL_ACCEPTABLE_PRICES("autosell.work_mode.acceptable_prices"),
    SELL_ANY_PRICES("autosell.work_mode.any_prices");

    private final Component displayText;

    WorkMode(String translationKey) {
        this.displayText = Component.translatable(translationKey);
    }

    public WorkMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component getDisplayText() {
        return displayText;
    }

    @Override
    public String toString() {
        return name();
    }
}
