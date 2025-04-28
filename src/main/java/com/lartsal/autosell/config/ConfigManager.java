package com.lartsal.autosell.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static ModConfig config;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autosell.json");

    public static ModConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                config = GSON.fromJson(Files.readString(CONFIG_PATH), ModConfig.class);
                config.trades.removeIf(entry -> !getConfig().isValidTradeEntry(entry));
            } else {
                config = new ModConfig();
                saveConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}