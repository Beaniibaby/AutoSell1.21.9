package com.lartsal.autosell;

import com.lartsal.autosell.platform.ForgePlatformHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class AutoSellForge {
    public AutoSellForge(FMLJavaModLoadingContext context) {
        ForgePlatformHelper.setEventBus(context.getModEventBus());
        AutoSellCore.init();
    }
}
