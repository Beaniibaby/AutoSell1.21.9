package com.lartsal.autosell.config;

import java.util.List;

public class Config {
    public enum ParticleDistributionType {
        CUBOID,
        ELLIPSOID
    }

    public boolean isModEnabled = true;
    public boolean isVillagerHighlightingEnabled = true;
    public int tradeProcessDelay = 1;     // In ticks
    public double acceptablePriceMultiplier = 1.5;
    public List<String> trades = List.of(
            "pumpkin => emerald",
            "melon => emerald"
    );

    public String highlightingParticlesName = "minecraft:flame";
    public double particlesPerTick = 1;
    public double yLevel = 1.25;
    public ParticleDistributionType particlesShape = ParticleDistributionType.ELLIPSOID;
    public double radiusX = 0, radiusY = 0, radiusZ = 0;
    public boolean randomSpeed = true;
    public double speedX = 0.07, speedY = 0.07, speedZ = 0.07;
}
