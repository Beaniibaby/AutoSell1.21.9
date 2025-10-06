package com.lartsal.autosell.platform;

import com.lartsal.autosell.Constants;
import com.lartsal.autosell.callbacks.INoArgumentsCallback;
import com.lartsal.autosell.callbacks.IEntityInteractionCallback;
import com.lartsal.autosell.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public void registerConfigScreen() {
        // Fabric config screen doesn't need to be registered. It is configured
        // in ModMenuIntegration and accessed via "modmenu" entrypoint
    }

    @Override
    public void registerClientStartCallback(INoArgumentsCallback callback) {
        ClientLifecycleEvents.CLIENT_STARTED.register(event -> callback.invoke());
    }

    @Override
    public void registerKeyBinding(KeyMapping keyMapping) {
        Constants.LOGGER.debug("Registering key: {}", keyMapping.getName());
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }

    @Override
    public void registerEntityInteractionCallback(IEntityInteractionCallback callback) {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide) return InteractionResult.PASS;
            return callback.invoke(player, entity, hand);
        });
    }

    @Override
    public void registerClientTickEndCallback(INoArgumentsCallback callback) {
        ClientTickEvents.END_CLIENT_TICK.register(event -> callback.invoke());
    }

    @Override
    public Path getConfigPath(String modId, String fileName) {
        return FabricLoader.getInstance().getConfigDir().resolve(modId).resolve(fileName);
    }

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
