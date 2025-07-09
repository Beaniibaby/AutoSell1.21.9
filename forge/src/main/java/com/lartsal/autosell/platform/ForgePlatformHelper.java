package com.lartsal.autosell.platform;

import com.lartsal.autosell.Constants;
import com.lartsal.autosell.callbacks.IEntityInteractionCallback;
import com.lartsal.autosell.callbacks.INoArgumentsCallback;
import com.lartsal.autosell.config.ConfigScreen;
import com.lartsal.autosell.platform.services.IPlatformHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {
    private static IEventBus eventBus;

    public static void setEventBus(IEventBus eventBus) {
        ForgePlatformHelper.eventBus = eventBus;
    }

    @Override
    public void registerConfigScreen() {
        ModContainer modContainer = ModList.get().getModContainerById(Constants.MOD_ID).orElseThrow(
                () -> new RuntimeException("Could not get ModContainer for " + Constants.MOD_ID)
        );
        modContainer.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> ConfigScreen.createConfigScreen(parent)
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
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
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
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> callback.invoke());
    }

    @Override
    public Path getConfigPath(String modId, String fileName) {
        return FMLPaths.CONFIGDIR.get().resolve(modId).resolve(fileName);
    }

    @Override
    public String getPlatformName() {
        return "Forge";
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
