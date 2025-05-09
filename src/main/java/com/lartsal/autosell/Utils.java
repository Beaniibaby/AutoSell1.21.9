package com.lartsal.autosell;

import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;

import java.util.List;
import java.util.Random;

public class Utils {
    private static final Random random = new Random();

    public static int getRandomSign() {
        return random.nextBoolean() ? 1 : -1;
    }

    public static List<String> getSimpleParticleIds() {
        return Registries.PARTICLE_TYPE.stream()
                .filter(particleType -> particleType instanceof SimpleParticleType)
                .map(particleType -> Registries.PARTICLE_TYPE.getId(particleType).toString())
                .sorted()
                .toList();
    }
}
