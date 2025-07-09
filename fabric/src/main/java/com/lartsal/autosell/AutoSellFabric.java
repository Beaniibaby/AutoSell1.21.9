package com.lartsal.autosell;

import net.fabricmc.api.ClientModInitializer;

public class AutoSellFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoSellCore.init();
    }
}
