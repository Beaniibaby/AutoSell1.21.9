package com.lartsal.autosell.config;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ModConfig {
    public static final Pattern TRADES_ENTRY_PATTERN = Pattern.compile("^\\s*(?:([a-z0-9_.-]+):)?([a-z0-9/._-]+)(?:\\s*\\+\\s*(?:([a-z0-9_.-]+):)?([a-z0-9/._-]+))?\\s*=>\\s*(?:([a-z0-9_.-]+):)?([a-z0-9/._-]+)\\s*$");

    public enum ParticleDistributionType {
        CUBOID,
        ELLIPSOID
    }

    public boolean isModEnabled = true;
    public boolean isVillagerHighlightingEnabled = true;
    public int tradesDelay = 1;     // In ticks
    public double acceptablePriceMultiplier = 1.5;
    public List<String> trades = List.of(
            "pumpkin => emerald",
            "melon => emerald"
    );

    public String highlightingParticlesId = "minecraft:flame";
    public double particlesPerTick = 1;
    public double yLevel = 1.25;
    public ParticleDistributionType particlesShape = ParticleDistributionType.ELLIPSOID;
    public double radiusX = 0, radiusY = 0, radiusZ = 0;
    public boolean randomSpeed = true;
    public double speedX = 0.07, speedY = 0.07, speedZ = 0.07;

    public static boolean isValidParticle(String particleName) {
        try {
            return Registries.PARTICLE_TYPE.containsId(Identifier.of(particleName));
        } catch (InvalidIdentifierException e) {
            return false;
        }
    }

    public static boolean isValidTradeEntry(String entry) {
        return TRADES_ENTRY_PATTERN.matcher(entry).matches();
    }
}