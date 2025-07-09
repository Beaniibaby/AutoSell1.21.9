package com.lartsal.autosell;

import com.lartsal.autosell.platform.NeoForgePlatformHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class AutoSellNeoForge {
    public AutoSellNeoForge(IEventBus eventBus) {
        NeoForgePlatformHelper.setEventBus(eventBus);
        AutoSellCore.init();
    }
}
