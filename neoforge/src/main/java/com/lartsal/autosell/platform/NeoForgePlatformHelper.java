package com.lartsal.autosell.platform;

import com.lartsal.autosell.callbacks.INoArgumentsCallback;
import com.lartsal.autosell.callbacks.IEntityInteractionCallback;
import com.lartsal.autosell.config.ConfigScreen;
import com.lartsal.autosell.platform.services.IPlatformHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private static IEventBus eventBus;

    public static void setEventBus(IEventBus eventBus) {
        NeoForgePlatformHelper.eventBus = eventBus;
    }

    @Override
    public void registerConfigScreen() {
        eventBus.addListener((FMLCommonSetupEvent event) ->
            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (minecraft, parent) -> ConfigScreen.createConfigScreen(parent)
            )
        );
    }

    @Override
    public void registerClientStartCallback(INoArgumentsCallback callback) {
        eventBus.addListener((FMLClientSetupEvent event) -> callback.invoke());
    }

    @Override
    public void registerKeyBinding(KeyMapping keyMapping) {
        if (FMLEnvironment.dist.isClient()) {
            eventBus.addListener((RegisterKeyMappingsEvent event) -> event.register(keyMapping));
        }
    }

    @Override
    public void registerEntityInteractionCallback(IEntityInteractionCallback callback) {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
            if (event.getLevel().isClientSide) {
                event.setCancellationResult(InteractionResult.PASS);
                return;
            }
            InteractionResult interactionResult = callback.invoke(event.getEntity(), event.getTarget(), event.getHand());
            event.setCancellationResult(interactionResult);
        });
    }

    @Override
    public void registerClientTickEndCallback(INoArgumentsCallback callback) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> callback.invoke());
    }

    @Override
    public Path getConfigPath(String modId, String fileName) {
        return FMLPaths.CONFIGDIR.get().resolve(modId).resolve(fileName);
    }

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
