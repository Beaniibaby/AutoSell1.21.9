package com.lartsal.autosell.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lartsal.autosell.Constants;
import com.lartsal.autosell.platform.Services;
import com.lartsal.autosell.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigSerializer {
    private static Config config;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigPath(Constants.MOD_ID, "autosell.json");

    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                config = GSON.fromJson(Files.readString(CONFIG_PATH), Config.class);
                config.trades.removeIf(entry -> !Utils.isValidTradeEntry(entry));
            } else {
                Files.createDirectories(CONFIG_PATH.getParent());
                config = new Config();
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
